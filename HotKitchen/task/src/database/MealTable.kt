package hotkitchen.database

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object MealTable : Table() {
    val mealId = integer("mealId")
    val title = text("title")
    val price = float("price")
    val imageUrl = text("imageUrl")
    val categoryIds = text("categoryIds")

    override val primaryKey = PrimaryKey(mealId)
}