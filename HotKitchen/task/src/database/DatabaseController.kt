package hotkitchen.database

import hotkitchen.data.Category
import hotkitchen.data.Meal
import hotkitchen.data.User
import hotkitchen.data.UserInfo
import hotkitchen.utils.BadRequestException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
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

    private fun resultRowToMeal(row: ResultRow) = Meal(
        mealId = row[MealTable.mealId],
        title = row[MealTable.title],
        price = row[MealTable.price],
        imageUrl = row[MealTable.imageUrl],
        categoryIds = row[MealTable.categoryIds].map { it.code }
    )

    private fun resultRowToCategory(row: ResultRow) = Category(
        categoryId = row[CategoryTable.categoryId],
        title = row[CategoryTable.title],
        description = row[CategoryTable.description]
    )


    suspend fun getMealById(mealId: Int): Meal? = transaction {
        val query = MealTable.select { MealTable.mealId eq mealId }
        query.mapNotNull {
            Meal(
                mealId = it[MealTable.mealId],
                title = it[MealTable.title],
                price = it[MealTable.price],
                imageUrl = it[MealTable.imageUrl],
                categoryIds = Json.decodeFromString(it[MealTable.categoryIds])
            )
        }.singleOrNull()
    }

    suspend fun getCategoryById(categoryId: Int): Category? = transaction {
        val query = CategoryTable.select { CategoryTable.categoryId eq categoryId}
        query.mapNotNull {
            Category(
                categoryId = it[CategoryTable.categoryId],
                title = it[CategoryTable.title],
                description = it[CategoryTable.description]
            )
        }.singleOrNull()
    }

    suspend fun addMeal(meal: Meal) = transaction {
        MealTable.insert {
            it[mealId] = meal.mealId
            it[title] = meal.title
            it[price] = meal.price
            it[imageUrl] = meal.imageUrl
            it[categoryIds] = Json.encodeToString(meal.categoryIds)
        }
    }
    suspend fun addCategory(category: Category)= transaction {
         CategoryTable.insert {
            it[categoryId] = category.categoryId
            it[title] = category.title
            it[description] = category.description
        }
    }

    suspend fun getAllMeals(): List<Meal> = transaction {
        MealTable.selectAll().map {
            Meal(
                mealId = it[MealTable.mealId],
                title = it[MealTable.title],
                price = it[MealTable.price],
                imageUrl = it[MealTable.imageUrl],
                categoryIds = Json.decodeFromString(it[MealTable.categoryIds])
            )
        }
    }

    suspend fun getAllCategories(): List<Category> = transaction {
        CategoryTable.selectAll().map {
            Category(
                categoryId = it[CategoryTable.categoryId],
                title = it[CategoryTable.title],
                description = it[CategoryTable.description]
            )
        }
    }
}