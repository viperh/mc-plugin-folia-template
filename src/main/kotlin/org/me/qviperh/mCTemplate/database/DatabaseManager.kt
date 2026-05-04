package org.me.qviperh.mCTemplate.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.plugin.java.JavaPlugin
import java.sql.Connection
import java.sql.SQLException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class DatabaseManager(private val plugin: JavaPlugin) : AutoCloseable {

    private var dataSource: HikariDataSource? = null
    private var executor: ExecutorService? = null

    var isEnabled: Boolean = false
        private set

    @Throws(SQLException::class)
    fun initialize() {
        val config = plugin.config
        isEnabled = config.getBoolean("database.enabled", false)

        if (!isEnabled) {
            plugin.logger.info("Database is disabled in config.yml")
            return
        }

        val jdbcUrl = config.getString("database.jdbcUrl", "") ?: ""
        val username = config.getString("database.username", "") ?: ""
        val password = config.getString("database.password", "") ?: ""

        if (jdbcUrl.isBlank()) {
            throw IllegalStateException("database.jdbcUrl is empty while database.enabled is true")
        }

        val maximumPoolSize = config.getInt("database.pool.maximumPoolSize", 10)
        val minimumIdle = config.getInt("database.pool.minimumIdle", 2)
        val connectionTimeout = config.getLong("database.pool.connectionTimeoutMs", 10_000L)
        val idleTimeout = config.getLong("database.pool.idleTimeoutMs", 600_000L)
        val maxLifetime = config.getLong("database.pool.maxLifetimeMs", 1_800_000L)

        val hikariConfig = HikariConfig().apply {
            poolName = "${plugin.name}Pool"
            this.jdbcUrl = jdbcUrl
            this.username = username
            this.password = password
            this.maximumPoolSize = maximumPoolSize
            this.minimumIdle = minimumIdle
            this.connectionTimeout = connectionTimeout
            this.idleTimeout = idleTimeout
            this.maxLifetime = maxLifetime
        }

        dataSource = HikariDataSource(hikariConfig)
        executor = Executors.newFixedThreadPool(
            maxOf(1, minimumIdle),
            NamedThreadFactory("${plugin.name.lowercase()}-db")
        )

        plugin.logger.info("Database connection initialized.")
    }

    fun executeAsync(consumer: (Connection) -> Unit): CompletableFuture<Void> {
        if (!isEnabled) return CompletableFuture.completedFuture(null)
        val ex = executor ?: return CompletableFuture.completedFuture(null)
        return CompletableFuture.runAsync({
            try {
                openConnection().use(consumer)
            } catch (e: SQLException) {
                throw CompletionException(e)
            }
        }, ex)
    }

    fun <T> queryAsync(function: (Connection) -> T): CompletableFuture<T?> {
        if (!isEnabled) return CompletableFuture.completedFuture(null)
        val ex = executor ?: return CompletableFuture.completedFuture(null)
        return CompletableFuture.supplyAsync({
            try {
                openConnection().use(function)
            } catch (e: Exception) {
                throw CompletionException(e)
            }
        }, ex)
    }

    @Throws(SQLException::class)
    fun openConnection(): Connection {
        val ds = dataSource ?: throw SQLException("Data source not initialized.")
        return ds.connection
    }

    override fun close() {
        executor?.let { ex ->
            ex.shutdown()
            try {
                if (!ex.awaitTermination(5, TimeUnit.SECONDS)) ex.shutdownNow()
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                ex.shutdownNow()
            }
        }
        dataSource?.close()
        dataSource = null
        executor = null
    }

    private class NamedThreadFactory(private val prefix: String) : ThreadFactory {
        private val counter = AtomicInteger(0)
        override fun newThread(runnable: Runnable): Thread =
            Thread(runnable, "$prefix-${counter.incrementAndGet()}").apply { isDaemon = true }
    }
}
