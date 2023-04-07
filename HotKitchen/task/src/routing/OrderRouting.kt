package hotkitchen.routing

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.routing.*

fun Application.orderRouting() {
    routing {
        authenticate {

        }
    }
}