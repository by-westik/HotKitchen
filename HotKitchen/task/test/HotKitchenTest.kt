import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.hyperskill.hstest.dynamic.DynamicTest
import org.hyperskill.hstest.stage.StageTest
import org.hyperskill.hstest.testcase.CheckResult

class HotKitchenTest : StageTest<Any>() {

    @Serializable
    private data class Credentials(var email: String, var userType: String, var password: String)

    @Serializable
    private data class SignUpCredentials(var email: String, var password: String)

    @Serializable
    private data class Token(val token: String)

    private object Messages {
        const val invalidEmail = """{"status":"Invalid email"}"""
        const val invalidPassword = """{"status":"Invalid password"}"""
        const val userAlreadyExists = """{"status":"User already exists"}"""
        const val invalidEmailPassword = """{"status":"Invalid email or password"}"""
    }

    private val time = System.currentTimeMillis().toString()
    private val wrongEmails =
        arrayOf(
            "@example.com",
            time,
            "$time@gmail",
            "$time@mail@com",
            "$time.gmail",
            "$time.mail.ru",
            "$time@yandex.ru@why",
            "$time@yandex@ru.why",
            "@which$time@gmail.com",
            "$time@gmail",
            "$time#lala@mail.us",
            "Goose Smith <$time@example.com>",
            "$time@example.com (Duck Smith)"
        )
    private val wrongPasswords =
        arrayOf(
            "",
            "ad12",
            "ad124",
            "password",
            "0123456",
            "paaaaaaaaaaaasssssword",
            "11113123123123123"
        )
    private val jwtRegex = """^[a-zA-Z0-9]+?\.[a-zA-Z0-9]+?\..+""".toRegex()
    private val currentCredentials = Credentials("$time@mail.com", "client", "password$time")
    private lateinit var signInToken: String
    private lateinit var signUpToken: String


    @DynamicTest(order = 1)
    fun checkWrongEmail(): CheckResult = withApplication(
        createTestEnvironment { config = HoconApplicationConfig(ConfigFactory.load("application.conf")) })
    {
        for (email in wrongEmails) {
            with(handleRequest(HttpMethod.Post, "/signup") {
                setBody(Json.encodeToString(Credentials(email, "client", "password123")))
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }) {
                if (response.content != Messages.invalidEmail || response.status() != HttpStatusCode.Forbidden)
                    return@withApplication CheckResult.wrong("Invalid email is not handled correctly.\nWrong response message or status code.\n$email is invalid email")
            }
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 2)
    fun checkWrongPassword(): CheckResult = withApplication(
        createTestEnvironment { config = HoconApplicationConfig(ConfigFactory.load("application.conf")) })
    {
        for (password in wrongPasswords) {
            with(handleRequest(HttpMethod.Post, "/signup") {
                setBody(Json.encodeToString(Credentials(currentCredentials.email, "client", password)))
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }) {
                if (response.content != Messages.invalidPassword || response.status() != HttpStatusCode.Forbidden)
                    return@withApplication CheckResult.wrong("Invalid password is not handled correctly.\nWrong response message or status code.\n$password is invalid password")
            }
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 3)
    fun getSignInJWTToken(): CheckResult = withApplication(
        createTestEnvironment { config = HoconApplicationConfig(ConfigFactory.load("application.conf")) })
    {
        with(handleRequest(HttpMethod.Post, "/signup") {
            setBody(Json.encodeToString(currentCredentials))
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }) {
            try {
                val principal = Json.decodeFromString<Token>(response.content ?: "")
                signInToken = principal.token
                if (!signInToken.matches(jwtRegex) || signInToken.contains(currentCredentials.email))
                    return@withApplication CheckResult.wrong("Invalid JWT token")
            } catch (e: Exception) {
                return@withApplication CheckResult.wrong("Cannot get token form /signin request")
            }
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 4)
    fun registerExistingUser(): CheckResult = withApplication(
        createTestEnvironment { config = HoconApplicationConfig(ConfigFactory.load("application.conf")) })
    {
        with(handleRequest(HttpMethod.Post, "/signup") {
            setBody(Json.encodeToString(currentCredentials))
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }) {
            if (response.content != Messages.userAlreadyExists || response.status() != HttpStatusCode.Forbidden)
                return@withApplication CheckResult.wrong("An existing user is registered. Wrong response message or status code.")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 5)
    fun wrongAuthorization(): CheckResult = withApplication(
        createTestEnvironment { config = HoconApplicationConfig(ConfigFactory.load("application.conf")) })
    {
        with(handleRequest(HttpMethod.Post, "/signin") {
            setBody(Json.encodeToString(SignUpCredentials("why?does?this?email?exists", currentCredentials.password)))
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }) {
            if (response.content != Messages.invalidEmailPassword || response.status() != HttpStatusCode.Forbidden)
                return@withApplication CheckResult.wrong("Error when authorizing a user using a wrong email. Wrong response message or status code.")
        }
        with(handleRequest(HttpMethod.Post, "/signin") {
            setBody(Json.encodeToString(SignUpCredentials(currentCredentials.email, "completelyWrong123")))
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }) {
            if (response.content != Messages.invalidEmailPassword || response.status() != HttpStatusCode.Forbidden)
                return@withApplication CheckResult.wrong("Error when authorizing a user using a wrong password. Wrong response message or status code.")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 6)
    fun getSignUpJWTToken(): CheckResult = withApplication(
        createTestEnvironment { config = HoconApplicationConfig(ConfigFactory.load("application.conf")) })
    {
        with(handleRequest(HttpMethod.Post, "/signin") {
            setBody(Json.encodeToString(SignUpCredentials(currentCredentials.email, currentCredentials.password)))
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }) {
            try {
                val principal = Json.decodeFromString<Token>(response.content ?: "")
                signUpToken = principal.token
                if (!signUpToken.matches(jwtRegex) || signUpToken.contains(currentCredentials.email))
                    return@withApplication CheckResult.wrong("Invalid JWT token")
            } catch (e: Exception) {
                return@withApplication CheckResult.wrong("Cannot get token form /signup request")
            }
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 7)
    fun wrongValidation(): CheckResult = withApplication(
        createTestEnvironment { config = HoconApplicationConfig(ConfigFactory.load("application.conf")) })
    {
        with(handleRequest(HttpMethod.Get, "/validate") {
            addHeader(
                HttpHeaders.Authorization,
                "Bearer lala${(100..999).random()}.blo${(100..999).random()}blo.kek${(100..999).random()}"
            )
        }) {
            if (response.status() != HttpStatusCode.Unauthorized)
                return@withApplication CheckResult.wrong("Wrong status code when authorizing with a completely wrong token using /validate")
        }
        with(handleRequest(HttpMethod.Get, "/validate") {
            addHeader(HttpHeaders.Authorization, signInToken)
        }) {
            if (response.status() != HttpStatusCode.Unauthorized)
                return@withApplication CheckResult.wrong("Wrong status code when authorizing with a JWT token using /validate. Do you use \"Bearer\" in header?")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 8)
    fun correctValidation(): CheckResult = withApplication(
        createTestEnvironment { config = HoconApplicationConfig(ConfigFactory.load("application.conf")) })
    {
        with(handleRequest(HttpMethod.Get, "/validate") {
            addHeader(HttpHeaders.Authorization, "Bearer $signInToken")
        }) {
            if (response.status() != HttpStatusCode.OK || response.content != "Hello, ${currentCredentials.userType} ${currentCredentials.email}")
                return@withApplication CheckResult.wrong("Token validation with signin token failed.\nStatus code should be \"200 OK\"\nMessage should be \"Hello, ${currentCredentials.userType} ${currentCredentials.email}\"")
        }
        with(handleRequest(HttpMethod.Get, "/validate") {
            addHeader(HttpHeaders.Authorization, "Bearer $signUpToken")
        }) {
            if (response.status() != HttpStatusCode.OK || response.content != "Hello, ${currentCredentials.userType} ${currentCredentials.email}")
                return@withApplication CheckResult.wrong("Token validation with signup token failed.\nStatus code should be \"200 OK\"\nMessage should be \"Hello, ${currentCredentials.userType} ${currentCredentials.email}\"")
        }
        return@withApplication CheckResult.correct()
    }
}