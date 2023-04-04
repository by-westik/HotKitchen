package hotkitchen

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import hotkitchen.database.configureDatabase
import hotkitchen.routing.configureRouting
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.serialization.*

import hotkitchen.data.ResponseStatus
import hotkitchen.utils.ForbiddenException

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module(testing: Boolean = false) {
    configureAuthentication()
    configurePages()
    configureRouting()
    configureDatabase()
    install(ContentNegotiation) { json() }
}

fun Application.configureAuthentication() {
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
        }
    }
}

fun Application.configurePages() {
    install(StatusPages) {
        exception<ForbiddenException> { cause ->
            call.respond(HttpStatusCode.Forbidden, ResponseStatus((cause.message ?: "")))
        }
    }
}