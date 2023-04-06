package hotkitchen.routing


import hotkitchen.data.Meal
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

fun Application.mealRouting() {
    routing {
        authenticate {
            post("/meals") {
                val principal = call.principal<JWTPrincipal>()
                val userType = principal!!.payload.getClaim("userType").asString()
                if (userType != "staff") throw ForbiddenException("Access denied")
                val meal = call.receive<Meal>()
                if (DatabaseController.getMealById(meal.mealId) != null) {
                    throw BadRequestException()
                }
                DatabaseController.addMeal(meal)
                call.respondText(Json.encodeToString(meal), ContentType.Application.Json)
            }

            get("/meals") {
                val mealId = call.request.queryParameters["id"]
                if (mealId == null) {
                    call.respond(HttpStatusCode.OK, DatabaseController.getAllMeals())
                } else {
                    val meal = DatabaseController.getMealById(mealId.toInt())
                    if (meal == null)  {
                        throw BadRequestException()
                    } else {
                        call.respondText(Json.encodeToString(meal), ContentType.Application.Json)
                    }

                }
            }
        }
    }

}