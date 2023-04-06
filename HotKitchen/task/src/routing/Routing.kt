package hotkitchen.routing

import hotkitchen.data.*
import hotkitchen.database.DatabaseController
import hotkitchen.utils.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Application.configureRouting() {
    routing {
        get("/main"){
            call.respondText("Hello World!")
        }
        post("/signup") {
            println("dignup1")
            val user = call.receive<User>()
            checkEmail(user.email)
            checkPassword(user.password)
            if (DatabaseController.getUserByEmail(user.email) == null) {
                DatabaseController.saveUser(user)
                call.respond(HttpStatusCode.OK, ResponseToken(generateToken(user)))
            } else {
                call.respond(HttpStatusCode.Forbidden, ResponseStatus("User already exists"))
            }

        }

        post("/signin") {
            val signInUser = call.receive<SignIn>()
            val user = DatabaseController.getUserByEmail(signInUser.email)
            if (user != null && user.password == signInUser.password) {
                call.respond(HttpStatusCode.OK, ResponseToken(generateToken(user)))
            } else {
                call.respond(HttpStatusCode.Forbidden, ResponseStatus("Invalid email or password"))
            }
        }

        authenticate {
            get("/validate") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val email = principal!!.payload.getClaim("email").asString()
                    val userType = principal.payload.getClaim("userType").asString()
                    //call.respond(HttpStatusCode.OK)
                    call.respondText("Hello, $userType $email")
                } catch (e: Exception) {
                    throw UnauthorizedException()
                }
            }

            get("/me") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val email = principal!!.payload.getClaim("email").asString()
                    val user = DatabaseController.getUserInfoByEmail(email)
                    if (user != null) {
                        if (user.name.isNullOrBlank() || user.phone.isNullOrBlank() || user.address.isNullOrBlank() ) {
                            call.respond(HttpStatusCode.BadRequest)
                        } else {
                            call.respondText(Json.encodeToString(user), ContentType.Application.Json)
                        }
                    } else {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                } catch (e: Exception) {
                    throw BadRequestException()
                }
            }

            put("/me") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val email = principal!!.payload.getClaim("email").asString()
                    val user = DatabaseController.getUserInfoByEmail(email)
                    val userInfo = call.receive<UserInfo>()

                    if (user != null) {
                        DatabaseController.updateUserByEmail(email, userInfo)
                    } else {
                        DatabaseController.saveUser(userInfo)
                    }
                    call.respond(HttpStatusCode.OK)
                } catch (e: Exception) {
                    throw BadRequestException()
                }
            }
            delete("/me") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val email = principal!!.payload.getClaim("email").asString()

                    if (!DatabaseController.deleteUserByEmail(email))
                        throw BadRequestException()
                    call.respond(HttpStatusCode.OK)
                } catch (e: Exception) {
                    throw BadRequestException()
                }

            }


        }

    }
}