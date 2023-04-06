package hotkitchen.database

import org.jetbrains.exposed.sql.Table

object CategoryTable : Table() {
    val categoryId = integer("categoryId")
    val title = text("title")
    val description = text("description")

    override val primaryKey = PrimaryKey(categoryId)
}