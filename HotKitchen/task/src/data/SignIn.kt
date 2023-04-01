package hotkitchen.data

import kotlinx.serialization.Serializable

@Serializable
data class SignIn(
    val email: String,
    val password: String
)
