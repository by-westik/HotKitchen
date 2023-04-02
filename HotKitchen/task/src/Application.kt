package hotkitchen

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import hotkitchen.database.configureDatabase
import hotkitchen.routing.configureRouting
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.serialization.*


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)
/*
fun main() {
    embeddedServer(Netty, port = 28852, host = "0.0.0.0", module = Application::module).start(wait = true)
}*/

fun Application.module(testing: Boolean = false) {
    install(Authentication) {
        jwt {
            val secret = environment.config.property("jwt.secret").getString()
            verifier(JWT.require(Algorithm.HMAC256(secret)).build())
            validate { credential ->
                if (credential.payload.getClaim("userType").asString() != "" &&
                    credential.payload.getClaim("email").asString() != "") {
                        JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            // Configure jwt authentication
        }
    }
    configureRouting()
    configureDatabase()
    install(ContentNegotiation) { json() }
}