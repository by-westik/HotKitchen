import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.hyperskill.hstest.dynamic.DynamicTest
import org.hyperskill.hstest.stage.StageTest
import org.hyperskill.hstest.testcase.CheckResult
import testdata.*

class HotKitchenTest : StageTest<Any>() {

    private val time = System.currentTimeMillis()
    private val jwtRegex = """^[a-zA-Z0-9]+?\.[a-zA-Z0-9]+?\..+""".toRegex()
    private val currentCredentialsClient = Credentials("$time@client.com", "client", "password$time")
    private var currentUserClient = User(
        time.toString() + "name",
        "client",
        "+79999999999",
        currentCredentialsClient.email,
        time.toString() + "address"
    )
    private val currentCredentialsStaff = Credentials("$time@staff.com", "staff", "password$time")
    private val currentMeals = arrayOf(
        Meal(
            time.toInt(),
            "$time title1",
            (time.toInt() % 100).toFloat(),
            "image $time url",
            listOf((0..10).random(), (0..10).random(), (0..10).random())
        ),
        Meal(
            time.toInt() + 1,
            "$time title1",
            (time.toInt() % 100).toFloat(),
            "image $time url",
            listOf((0..10).random(), (0..10).random(), (0..10).random())
        ),
        Meal(
            time.toInt() + 2,
            "$time title1",
            (time.toInt() % 100).toFloat(),
            "image $time url",
            listOf((0..10).random(), (0..10).random(), (0..10).random())
        )
    )
    private val accessDenied = """{"status":"Access denied"}"""

    private val price = currentMeals[0].price + currentMeals[1].price + currentMeals[2].price
    private val mealsIds = listOf(currentMeals[0].mealId, currentMeals[1].mealId, currentMeals[2].mealId)
    private val currentOrder =
        Order(time.toInt(), currentCredentialsClient.email, mealsIds, price, currentUserClient.address, "COOK")

    private lateinit var signInTokenClient: String
    private lateinit var signInTokenStaff: String

    private var incompleteSize = 0


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
                "testdata.Token validation with signin token failed.\nStatus code should be \"200 OK\"\nMessage should be \"Hello, ${currentCredentialsClient.userType} ${currentCredentialsClient.email}\""
            )
        }
        with(handleRequest(HttpMethod.Get, "/validate") {
            addHeader(HttpHeaders.Authorization, "Bearer $signInTokenStaff")
        }) {
            if (response.status() != HttpStatusCode.OK || response.content != "Hello, ${currentCredentialsStaff.userType} ${currentCredentialsStaff.email}") return@withApplication CheckResult.wrong(
                "testdata.Token validation with signin token failed.\nStatus code should be \"200 OK\"\nMessage should be \"Hello, ${currentCredentialsStaff.userType} ${currentCredentialsStaff.email}\""
            )
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 3)
    fun createUser(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Put, "/me") {
            setBody(Json.encodeToString(currentUserClient))
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            addHeader(HttpHeaders.Authorization, "Bearer $signInTokenClient")
        }) {
            if (response.status() != HttpStatusCode.OK)
                return@withApplication CheckResult.wrong("Cannot add user by put method")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 4)
    fun successAdditionMeal(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        for (meal in currentMeals)
            with(handleRequest(HttpMethod.Post, "/meals") {
                setBody(Json.encodeToString(meal))
                addHeader(HttpHeaders.Authorization, "Bearer $signInTokenStaff")
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }) {
                if (response.status() != HttpStatusCode.OK)
                    return@withApplication CheckResult.wrong("The meal was not added. Wrong status code.")
            }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 5)
    fun invalidOrderCreation(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Post, "/order") {
            setBody(Json.encodeToString(listOf(1, 2, (-9999999..-9999).random())))
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            addHeader(HttpHeaders.Authorization, "Bearer $signInTokenClient")
        }) {
            if (response.status() != HttpStatusCode.BadRequest)
                return@withApplication CheckResult.wrong("Created an order with the wrong meal id. Wrong status code.")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 6)
    fun validOrderCreation(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Post, "/order") {
            setBody(Json.encodeToString(mealsIds))
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            addHeader(HttpHeaders.Authorization, "Bearer $signInTokenClient")
        }) {
            if (response.status() != HttpStatusCode.OK)
                return@withApplication CheckResult.wrong("Unable to create order. Wrong status code.")
            val order = Json.decodeFromString<Order>(response.content ?: "")
            if (order.userEmail != currentOrder.userEmail || order.price != currentOrder.price || order.address != currentOrder.address || order.status != currentOrder.status)
                return@withApplication CheckResult.wrong("Wrong order.")
            else currentOrder.orderId = order.orderId
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 7)
    fun invalidMarkAsReady(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Post, "/order/${currentOrder.orderId}/markReady") {
            setBody(Json.encodeToString(mealsIds))
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            addHeader(HttpHeaders.Authorization, "Bearer $signInTokenClient")
        }) {
            if (response.status() != HttpStatusCode.Forbidden || response.content != accessDenied)
                return@withApplication CheckResult.wrong("Only staff can mark order as COMPLETE. Wrong status code.")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 8)
    fun validMarkAsReady(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Post, "/order/${currentOrder.orderId}/markReady") {
            setBody(Json.encodeToString(mealsIds))
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            addHeader(HttpHeaders.Authorization, "Bearer $signInTokenStaff")
        }) {
            if (response.status() != HttpStatusCode.OK)
                return@withApplication CheckResult.wrong("Unable to mark order as COMPLETE. Wrong status code.")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 9)
    fun getOrders(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Get, "/orderHistory") {
            addHeader(HttpHeaders.Authorization, "Bearer $signInTokenClient")
        }) {
            val orders: List<Order> = Json.decodeFromString(response.content ?: "")
            var flag = true
            for (order in orders) {
                if (order.status == "COOK") incompleteSize++
                if (order.orderId == currentOrder.orderId) flag = false
            }
            if (flag)
                return@withApplication CheckResult.wrong("Wrong orders list. The newly added order is missing.")
            if (response.status() != HttpStatusCode.OK)
                return@withApplication CheckResult.wrong("Wrong status code in /orderHistory")
        }
        return@withApplication CheckResult.correct()
    }

    @DynamicTest(order = 10)
    fun getIncompleteOrders(): CheckResult = withApplication(createTestEnvironment {
        config = HoconApplicationConfig(ConfigFactory.load("application.conf"))
    }) {
        with(handleRequest(HttpMethod.Get, "/orderIncomplete") {
            addHeader(HttpHeaders.Authorization, "Bearer $signInTokenClient")
        }) {
            val orders: List<Order> = Json.decodeFromString(response.content ?: "")
            for (order in orders)
                if (order.status != "COOK") return@withApplication CheckResult.wrong("One of the orders is COMPLETE.")
            if (orders.size != incompleteSize) return@withApplication CheckResult.wrong("Invalid size of Incomplete orders.")
            if (response.status() != HttpStatusCode.OK)
                return@withApplication CheckResult.wrong("Wrong status code in /orderHistory")
        }
        return@withApplication CheckResult.correct()
    }

}