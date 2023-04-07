package hotkitchen.routing

import hotkitchen.data.Order
import hotkitchen.database.DatabaseController
import hotkitchen.utils.BadRequestException
import hotkitchen.utils.ForbiddenException
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Application.orderRouting() {
    routing {
        authenticate {
            post("/order") {
                val principal = call.principal<JWTPrincipal>()
                val email = principal!!.payload.getClaim("email").asString()

                val user = DatabaseController.getUserInfoByEmail(email) ?: throw BadRequestException()

                val mealsIds = call.receive<List<Int>>()
                var price = 0f
                mealsIds.forEach {
                    val meal = DatabaseController.getMealById(it) ?: throw BadRequestException()
                    price += meal.price
                }

                val order = user.address?.let { it ->
                    Order(
                        orderId = System.currentTimeMillis().toInt(),
                        userEmail = user.email,
                        mealsIds = mealsIds,
                        price = price,
                        address = it,
                        status = "COOK"
                    )
                }
                if (order != null) {
                    DatabaseController.saveOrder(order)
                }
                call.respondText(Json.encodeToString(order), ContentType.Application.Json )
            }

            post("/order/{orderId}/markReady") {
                val orderId = call.parameters["orderId"]!!.toInt()
                val principal = call.principal<JWTPrincipal>()
                val userType = principal!!.payload.getClaim("userType").asString()

                if (userType != "staff") throw ForbiddenException("Access denied")

                val order = DatabaseController.getOrderById(orderId) ?: throw BadRequestException()
                println("4")
                order.status = "COMPLETE"
                println("5")
                DatabaseController.updateOrderStatus(order)
                println("6")
                call.respondText(Json.encodeToString(order), ContentType.Application.Json)
            }

            get("/orderHistory") {
                call.respond(HttpStatusCode.OK, DatabaseController.getAllOrders())
            }

            get("/orderIncomplete") {
                call.respond(HttpStatusCode.OK, DatabaseController.getAllOrdersIncomplete())
            }
        }
    }
}