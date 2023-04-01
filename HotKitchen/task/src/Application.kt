package hotkitchen

import hotkitchen.database.configureDatabase
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
/*
fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)
*/
fun main() {
    embeddedServer(Netty, port = 28852, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module(testing: Boolean = false) {
    install(ContentNegotiation) { json() }
    configureRouting()
    configureDatabase()
}