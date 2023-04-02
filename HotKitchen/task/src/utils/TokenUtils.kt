package hotkitchen.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.typesafe.config.ConfigFactory
import hotkitchen.data.User
import io.ktor.config.*
import java.util.*

//val secret = environment.config.property("jwt.secret").getString()

val secret = HoconApplicationConfig(ConfigFactory.load()).property("jwt.secret").getString()
fun generateToken(user: User): String = JWT.create()
        .withClaim("email", user.email)
        .withClaim("userType", user.userType)
        .withExpiresAt(Date(System.currentTimeMillis() + 60000))
        .sign(Algorithm.HMAC256(secret))

