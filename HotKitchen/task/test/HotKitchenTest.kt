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
    private data class Token(val token: String)

    @Serializable
    data class Meal(
        val mealId: Int,
        val title: String,
        val price: Float,
        val imageUrl: String,
        val categoryIds: List<Int>
    )

    @Serializable
    data class Category(
        val categoryId: Int,
        val title: String,
        val description: String
    )

    private val time = System.currentTimeMillis()
    private val jwtRegex = """^[a-zA-Z0-9]+?\.[a-zA-Z0-9]+?\..+""".toRegex()
    private val accessDenied = """{"status":"Access denied"}"""
    private val currentCredentialsClient = Credentials("$time@client.com", "client", "password$time")
    private val currentCredentialsStaff = Credentials("$time@staff.com", "staff", "password$time")
    private val currentMeal = Meal(
        time.toInt(),
        "$time title",
        (time.toInt() % 100).toFloat(),
        "image $time url",
        listOf((0..10).random(), (0..10).random(), (0..10).random())
    )
    private val currentCategory = Category(
        time.toInt(),
        "$time TITLE",
        "Awesome $time description"
    )
    private lateinit var signInTokenClient: String
    private lateinit var signInTokenStaff: String


    @DynamicTest(order = 1)
    fun getSignInJWTToken(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Post, "/signup") {
            setBody(Json.encodeToString(currentCredentialsClient))
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }) {
            try {
                val principal = Json.decodeFromString<Token>(response.content ?: "")
                signInTokenClient = principal.token
                if (!signInTokenClient.matches(jwtRegex) || signInTokenClient.contains(currentCredentialsClient.email))
                    return@withApplication CheckResult.wrong("Invalid JWT token")
            } catch (e: Exception) {
                return@withApplication CheckResult.wrong("Cannot get token form /signin request")
            }
        }
        with(handleRequest(HttpMethod.Post, "/signup") {
            setBody(Json.encodeToString(currentCredentialsStaff))
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }) {
            try {
                val principal = Json.decodeFromString<Token>(response.content ?: "")
                signInTokenStaff = principal.token
                if (!signInTokenStaff.matches(jwtRegex) || signInTokenStaff.contains(currentCredentialsStaff.email))
                    return@withApplication CheckResult.wrong("Invalid JWT token")
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
            addHeader(HttpHeaders.Authorization, "Bearer $signInTokenClient")
        }) {
            if (response.status() != HttpStatusCode.OK || response.content != "Hello, ${currentCredentialsClient.userType} ${currentCredentialsClient.email}") return@withApplication CheckResult.wrong(
                "Token validation with signin token failed.\nStatus code should be \"200 OK\"\nMessage should be \"Hello, ${currentCredentialsClient.userType} ${currentCredentialsClient.email}\""
            )
        }
        with(handleRequest(HttpMethod.Get, "/validate") {
            addHeader(HttpHeaders.Authorization, "Bearer $signInTokenStaff")
        }) {
            if (response.status() != HttpStatusCode.OK || response.content != "Hello, ${currentCredentialsStaff.userType} ${currentCredentialsStaff.email}") return@withApplication CheckResult.wrong(
                "Token validation with signin token failed.\nStatus code should be \"200 OK\"\nMessage should be \"Hello, ${currentCredentialsStaff.userType} ${currentCredentialsStaff.email}\""
            )
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 3)
    fun accessDeniedAdditionMeal(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Post, "/meals") {
            setBody(Json.encodeToString(currentMeal))
            addHeader(HttpHeaders.Authorization, "Bearer $signInTokenClient")
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }) {
            if (response.status() != HttpStatusCode.Forbidden || response.content != accessDenied)
                return@withApplication CheckResult.wrong("Only staff can add meal. Wrong response or status code")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 4)
    fun accessDeniedAdditionCategory(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Post, "/categories") {
            setBody(Json.encodeToString(currentCategory))
            addHeader(HttpHeaders.Authorization, "Bearer $signInTokenClient")
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }) {
            if (response.status() != HttpStatusCode.Forbidden || response.content != accessDenied)
                return@withApplication CheckResult.wrong("Only staff can add category. Wrong response or status code")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 5)
    fun successAdditionMeal(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Post, "/meals") {
            setBody(Json.encodeToString(currentMeal))
            addHeader(HttpHeaders.Authorization, "Bearer $signInTokenStaff")
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }) {
            if (response.status() != HttpStatusCode.OK)
                return@withApplication CheckResult.wrong("The meal was not added. Wrong status code.")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 6)
    fun failedAdditionMeal(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Post, "/meals") {
            setBody(Json.encodeToString(currentMeal))
            addHeader(HttpHeaders.Authorization, "Bearer $signInTokenStaff")
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }) {
            if (response.status() != HttpStatusCode.BadRequest)
                return@withApplication CheckResult.wrong("The meal was added twice. Wrong status code.")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 7)
    fun successAdditionCategory(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Post, "/categories") {
            setBody(Json.encodeToString(currentCategory))
            addHeader(HttpHeaders.Authorization, "Bearer $signInTokenStaff")
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }) {
            if (response.status() != HttpStatusCode.OK)
                return@withApplication CheckResult.wrong("The category was not added. Wrong status code.")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 8)
    fun failedAdditionCategory(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Post, "/categories") {
            setBody(Json.encodeToString(currentCategory))
            addHeader(HttpHeaders.Authorization, "Bearer $signInTokenStaff")
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }) {
            if (response.status() != HttpStatusCode.BadRequest)
                return@withApplication CheckResult.wrong("The category was added twice. Wrong status code.")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 9)
    fun getMealById(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Get, "/meals?id=${currentMeal.mealId}") {
            addHeader(HttpHeaders.Authorization, "Bearer $signInTokenClient")
        }) {
            if (response.content != Json.encodeToString(currentMeal))
                return@withApplication CheckResult.wrong("Wrong meal by id.")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 10)
    fun getCategoryById(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Get, "/categories?id=${currentCategory.categoryId}") {
            addHeader(HttpHeaders.Authorization, "Bearer $signInTokenClient")
        }) {
            if (response.content != Json.encodeToString(currentCategory))
                return@withApplication CheckResult.wrong("Wrong category by id.")
        }
        return@withApplication CheckResult.correct()
    }


    @DynamicTest(order = 11)
    fun getMeals(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Get, "/meals") {
            addHeader(HttpHeaders.Authorization, "Bearer $signInTokenClient")
        }) {
            val meals: List<Meal> = Json.decodeFromString(response.content ?: "")
            var flag = true
            for (meal in meals) {
                if (meal.mealId == currentMeal.mealId) {
                    flag = false
                    break
                }
            }
            if (flag) return@withApplication CheckResult.wrong("Wrong meals list. The newly added meal is missing.")
            if (response.status() != HttpStatusCode.OK)
                return@withApplication CheckResult.wrong("Wrong status code in /meals")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 12)
    fun getCategories(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Get, "/categories") {
            addHeader(HttpHeaders.Authorization, "Bearer $signInTokenClient")
        }) {
            val categories: List<Category> = Json.decodeFromString(response.content ?: "")
            var flag = true
            for (category in categories) {
                if (category.categoryId == currentCategory.categoryId) {
                    flag = false
                    break
                }
            }
            if (flag)
                return@withApplication CheckResult.wrong("Wrong categories list. The newly added category is missing.")
            if (response.status() != HttpStatusCode.OK)
                return@withApplication CheckResult.wrong("Wrong status code in /categories")
        }
        return@withApplication CheckResult.correct()
    }

}