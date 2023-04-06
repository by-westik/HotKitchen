package hotkitchen.routing

import hotkitchen.data.Category
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

fun Application.categoryRouting() {
    routing {
        authenticate {
            post("/categories") {
                val principal = call.principal<JWTPrincipal>()
                val userType = principal!!.payload.getClaim("userType").asString()

                if (userType != "staff") throw ForbiddenException("Access denied")

                val category = call.receive<Category>()
                if (DatabaseController.getCategoryById(category.categoryId) != null)
                    throw BadRequestException()
                DatabaseController.addCategory(category)
                call.respondText(Json.encodeToString(category), ContentType.Application.Json)

            }

            get("/categories") {
                val categoryId = call.request.queryParameters["id"]
                if (categoryId == null){
                    call.respond(HttpStatusCode.OK, DatabaseController.getAllCategories())
                } else {
                    val category = DatabaseController.getCategoryById(categoryId.toInt())
                    if (category == null) {
                        throw BadRequestException()
                    } else {
                        call.respondText(Json.encodeToString(category), ContentType.Application.Json)
                    }
                }

            }
        }
    }

}