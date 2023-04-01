package hotkitchen.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object UserTable : IntIdTable() {
    val email: Column<String> = text("email")
    val userType: Column<String> = text("userType")
    val password: Column<String> = text("password")
}