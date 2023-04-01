package hotkitchen.data

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val email: String,
    val userType: String,
    val password: String
)
