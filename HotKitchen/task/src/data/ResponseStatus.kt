package hotkitchen.data

import kotlinx.serialization.Serializable

@Serializable
data class ResponseStatus (
    val status: String
)