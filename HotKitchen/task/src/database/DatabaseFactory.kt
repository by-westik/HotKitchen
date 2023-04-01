package hotkitchen.database

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.*
import io.ktor.config.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    private val appConfig = HoconApplicationConfig(ConfigFactory.load())
    private val driverClassName = appConfig.property("storage.driverClassName").getString()
    private val dbUrl = appConfig.property("storage.jdbcURL").getString()
    private val dbUser = appConfig.property("storage.username").getString()
    private val dbUserPassword = appConfig.property("storage.password").getString()

    fun init() {
        Database.connect(hikari())
    }

    private fun hikari(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = driverClassName
        config.username = dbUser
        config.jdbcUrl = dbUrl
        config.password = dbUserPassword
        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        return HikariDataSource(config)
    }
}

fun Application.configureDatabase() {
    DatabaseFactory.init()
    transaction {
       SchemaUtils.create(UserTable)
    }
    transaction {
        addLogger(StdOutSqlLogger)
    }
}