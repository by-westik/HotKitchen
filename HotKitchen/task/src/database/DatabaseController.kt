package hotkitchen.database

import hotkitchen.data.User
import hotkitchen.data.UserInfo
import hotkitchen.utils.BadRequestException
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

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

    suspend fun saveUser(user: UserInfo) = transaction {
        UserTable.insert {
            it[email] = user.email
            it[userType] = user.userType
            it[address] = user.address
            it[phone] = user.phone
            it[name] = user.name
        }
    }

    suspend fun saveUser(user: User) = transaction {
        UserTable.insert {
            it[email] = user.email
            it[password] = user.password
            it[userType] = user.userType
        }
    }

    suspend fun getUserInfoByEmail(email: String) : UserInfo? = transaction {
        val query = UserTable.select { UserTable.email eq email }
        println ("aaaaaaa")
        query.mapNotNull {
            UserInfo(
                name = it[UserTable.name],
                userType = it[UserTable.userType],
                phone = it[UserTable.phone],
                email = it[UserTable.email],
                address = it[UserTable.address]
            )
        }.singleOrNull()
    }


    suspend fun updateUserByEmail(email: String, user: UserInfo) = transaction {
        if (email != user.email)
            throw BadRequestException()

        UserTable.update({ UserTable.email eq email }) {
            it[name] = user.name
            it[address] = user.address
            it[userType] = user.userType
            it[phone] = user.phone
            it[UserTable.email] = user.email
        }
    }

    suspend fun deleteUserByEmail(email: String): Boolean = transaction {
        UserTable.deleteWhere { UserTable.email eq email } > 0
    }
}