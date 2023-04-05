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

class SimpleTest : StageTest<Any>() {

    @Serializable
    private data class Credentials(var email: String, var userType: String, var password: String)

    @Serializable
    private data class User(
        val name: String, val userType: String, val phone: String, val email: String, val address: String
    )

    private fun User.isEquals(user: User) =
        name == user.name && userType == user.userType && phone == user.phone && email == user.email && address == user.address


    @Serializable
    private data class Token(val token: String)

    private val time = System.currentTimeMillis().toString()
    private val jwtRegex = """^[a-zA-Z0-9]+?\.[a-zA-Z0-9]+?\..+""".toRegex()
    private val currentCredentials = Credentials("$time@mail.com", "client", "password$time")
    private var currentUser = User(time + "name", "client", "+79999999999", currentCredentials.email, time + "address")
    private lateinit var signInToken: String


    @DynamicTest(order = 1)
    fun getSignInJWTToken(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Post, "/signup") {
            setBody(Json.encodeToString(currentCredentials))
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }) {
            try {
                val principal = Json.decodeFromString<Token>(response.content ?: "")
                signInToken = principal.token
                if (!signInToken.matches(jwtRegex) || signInToken.contains(currentCredentials.email)) return@withApplication CheckResult.wrong(
                    "Invalid JWT token"
                )
            } catch (e: Exception) {
                return@withApplication CheckResult.wrong("Cannot get token form /signin request")
            }
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 2)
    fun correctValidation(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Get, "/validate") {
            addHeader(HttpHeaders.Authorization, "Bearer $signInToken")
        }) {
            if (response.status() != HttpStatusCode.OK || response.content != "Hello, ${currentCredentials.userType} ${currentCredentials.email}") return@withApplication CheckResult.wrong(
                "Token validation with signin token failed.\nStatus code should be \"200 OK\"\nMessage should be \"Hello, ${currentCredentials.userType} ${currentCredentials.email}\""
            )
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 3)
    fun getNonExistentUser(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Get, "/me") {
            addHeader(HttpHeaders.Authorization, "Bearer $signInToken")
        }) {
            if (response.status() != HttpStatusCode.BadRequest) return@withApplication CheckResult.wrong("Status code for a getting non-existent user should be \"400 Bad Request\"")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 4)
    fun createUser(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Put, "/me") {
            setBody(Json.encodeToString(currentUser))
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            addHeader(HttpHeaders.Authorization, "Bearer $signInToken")
        }) {
            if (response.status() != HttpStatusCode.OK)
                return@withApplication CheckResult.wrong("Cannot add user by put method")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 5)
    fun getExistentUser(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Get, "/me") {
            addHeader(HttpHeaders.Authorization, "Bearer $signInToken")
        }) {
            val user = Json.decodeFromString<User>(response.content ?: "")
            if (!user.isEquals(currentUser)) return@withApplication CheckResult.wrong("Get method responded with different user information.")
            if (response.status() != HttpStatusCode.OK) return@withApplication CheckResult.wrong("Status code for a getting existent user should be \"200 OK\"")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 6)
    fun putDifferentEmail(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Put, "/me") {
            val newUser = currentUser.copy(email = "different@mail.com")
            setBody(Json.encodeToString(newUser))
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            addHeader(HttpHeaders.Authorization, "Bearer $signInToken")
        }) {
            if (response.status() != HttpStatusCode.BadRequest)
                return@withApplication CheckResult.wrong("You can not change the user's email! Wrong status code.")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 7)
    fun updateCurrentUser(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Put, "/me") {
            currentUser = currentUser.copy(name = "newName$time", userType = "newType", address = "newAddress$time")
            setBody(Json.encodeToString(currentUser))
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            addHeader(HttpHeaders.Authorization, "Bearer $signInToken")
        }) {
            if (response.status() != HttpStatusCode.OK)
                return@withApplication CheckResult.wrong("Cannot update user information by put method")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 8)
    fun getNewExistentUser(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Get, "/me") {
            addHeader(HttpHeaders.Authorization, "Bearer $signInToken")
        }) {
            val user = Json.decodeFromString<User>(response.content ?: "")
            if (!user.isEquals(currentUser)) return@withApplication CheckResult.wrong("Get method responded with different user information after updating user info.")
            if (response.status() != HttpStatusCode.OK) return@withApplication CheckResult.wrong("Status code for a getting existent user should be \"200 OK\"")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 9)
    fun deleteExistentUser(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Delete, "/me") {
            addHeader(HttpHeaders.Authorization, "Bearer $signInToken")
        }) {
            if (response.status() != HttpStatusCode.OK)
                return@withApplication CheckResult.wrong("Status code for a deleting existent user should be \"200 OK\"")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 10)
    fun deleteNonExistentUser(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Delete, "/me") {
            addHeader(HttpHeaders.Authorization, "Bearer $signInToken")
        }) {
            if (response.status() != HttpStatusCode.BadRequest)
                return@withApplication CheckResult.wrong("Status code for a deleting non-existent user should be \"400 Bad Request\"")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 11)
    fun getDeletedUser(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Get, "/me") {
            addHeader(HttpHeaders.Authorization, "Bearer $signInToken")
        }) {
            if (response.status() != HttpStatusCode.BadRequest)
                return@withApplication CheckResult.wrong("Status code for a getting deleted user should be \"400 Bad Request\"")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 12)
    fun checkDeletedCredentials(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Post, "/signup") {
            setBody(Json.encodeToString(currentCredentials))
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }) {
            if (response.status() != HttpStatusCode.OK)
                return@withApplication CheckResult.wrong("Unable to signin after deleting user information. Did you forget to delete user credentials?")
        }
        return@withApplication CheckResult.correct()
    }
}