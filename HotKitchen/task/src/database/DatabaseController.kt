package hotkitchen.database

import hotkitchen.data.User
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseController {

    suspend fun getUserByEmail(email: String) = transaction {
        val query = UserTable.select { UserTable.email eq email }
        query.mapNotNull {
            User(
                email = it[UserTable.email],
                password = it[UserTable.password],
                userType = it[UserTable.userType]
            )
        }.singleOrNull()
    }

    suspend fun saveUser(user: User) = transaction {
        UserTable.insert {
            it[email] = user.email
            it[password] = user.password
            it[userType] = user.userType
        }
    }
}