package org.me.qviperh.mCTemplate

import org.bukkit.plugin.java.JavaPlugin
import org.me.qviperh.mCTemplate.command.AsyncCommandManager
import org.me.qviperh.mCTemplate.database.DatabaseManager
import org.me.qviperh.mCTemplate.listener.PlayerListener

class MCTemplate : JavaPlugin() {

    lateinit var databaseManager: DatabaseManager
        private set
    lateinit var commandManager: AsyncCommandManager
        private set

    override fun onEnable() {
        try {
            saveDefaultConfig()
            instance = this

            databaseManager = DatabaseManager(this)
            databaseManager.initialize()

            commandManager = AsyncCommandManager(this)
            commandManager.registerCommands()

            PlayerListener(this)

            logger.info("$name has been enabled!")
        } catch (exception: Exception) {
            logger.severe("An error occurred during plugin initialization. Disabling plugin....")
            exception.printStackTrace()
            server.pluginManager.disablePlugin(this)
        }
    }

    override fun onDisable() {
        if (::databaseManager.isInitialized) {
            databaseManager.close()
        }
        logger.info("$name has been disabled!")
    }

    companion object {
        lateinit var instance: MCTemplate
            private set
    }
}
