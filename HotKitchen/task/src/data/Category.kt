package hotkitchen.data

import kotlinx.serialization.Serializable

@Serializable
data class Category (
    val categoryId: Int,
    val title: String,
    val description: String
)
