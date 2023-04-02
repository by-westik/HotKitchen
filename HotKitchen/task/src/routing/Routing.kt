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
            if (!checkEmail(user.email)) {
                call.respond(HttpStatusCode.Forbidden, ResponseStatus("Invalid email"))
            } else if (checkPassword(user.password)) {
                call.respond(HttpStatusCode.Forbidden, ResponseStatus("Invalid password"))
            } else {
                if (DatabaseController.getUserByEmail(user.email) == null) {
                    DatabaseController.saveUser(user)
                    call.respond(HttpStatusCode.OK, ResponseToken(generateToken(user)))
                } else {
                    call.respond(HttpStatusCode.Forbidden, ResponseStatus("User already exists"))
                }
            }
        }

        post("/signin") {
            val signInUser = call.receive<SignIn>()
            val user = DatabaseController.getUserByEmail(signInUser.email)
            if (user != null && user.password == signInUser.password) {
                call.respond(HttpStatusCode.OK, ResponseStatus("Signed In"))
            } else {
                call.respond(HttpStatusCode.Forbidden, ResponseStatus("Authorization failed"))
            }
        }
    }
}