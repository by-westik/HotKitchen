package hotkitchen.routing

import hotkitchen.data.ResponseStatus
import hotkitchen.data.ResponseToken
import hotkitchen.data.SignIn
import hotkitchen.data.User
import hotkitchen.database.DatabaseController
import hotkitchen.utils.checkEmail
import hotkitchen.utils.checkPassword
import hotkitchen.utils.generateToken
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Application.configureRouting() {
    routing {
        get("/main"){
            call.respondText("Hello World!")
        }
        post("/signup") {
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
                    call.respond(HttpStatusCode.Unauthorized)
                }
            }
        }
    }
}