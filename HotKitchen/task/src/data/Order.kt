package hotkitchen.data

import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val orderId: Int,
    val userEmail: String,
    val mealsIds: List<Int>,
    val price: Float,
    val address: String,
    val status: String
)
