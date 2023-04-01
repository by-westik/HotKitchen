package hotkitchen

import hotkitchen.data.ResponseStatus
import hotkitchen.data.SignIn
import hotkitchen.data.User
import hotkitchen.database.DatabaseController
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Application.configureRouting() {
    routing {
        authenticate("myFormAuth") {
            authenticate("myFormAuth2") {
                post("/page") {
                    call.respondText("Protected content! Name: ${call.principal<UserIdPrincipal>()?.name}")
                }
            }
        }
        get("/page") {
            call.respondRedirect("/auth")
        }
        get("/auth") {
            val formHtml = """
    <form action="/page" method="post">
        <input type="text" name="username">
        <input type="password" name="password">
        <input type="submit" value="Auth">
    </form>
""".trimIndent()
            call.respondText(formHtml, ContentType.Text.Html)
        }
        get("/main"){
            call.respondText("Hello World!")
        }
    }
/*
        post("/signup") {
            val user = call.receive<User>()
            if (DatabaseController.getUserByEmail(user.email) == null) {
                DatabaseController.saveUser(user)
                call.respond(HttpStatusCode.OK, ResponseStatus("Signed In"))
            } else {
                call.respond(HttpStatusCode.Forbidden, ResponseStatus("Registration failed"))
            }
        }

        post("/signin") {
            val signInUser = call.receive<SignIn>()
            val user = DatabaseController.getUserByEmail(signInUser.email)
            if (user != null && user.password == signInUser.password) {
                call.respond(HttpStatusCode.OK, ResponseStatus("Signed Up"))
            } else {
                call.respond(HttpStatusCode.Forbidden, ResponseStatus("Authorization failed"))
            }
        }
    }*/
}