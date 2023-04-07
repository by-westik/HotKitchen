package hotkitchen.database

import org.jetbrains.exposed.sql.Table

object OrderTable: Table() {
    val orderId = integer("orderId")
    val userEmail = text("userEmail")
    val mealsIds = text("mealsIds")
    val price = float("price")
    val address = text("address")
    val status = text("status")
}