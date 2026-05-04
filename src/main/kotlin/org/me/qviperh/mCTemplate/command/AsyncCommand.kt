package org.me.qviperh.mCTemplate.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.command.TabCompleter
import java.util.concurrent.CompletableFuture

abstract class AsyncCommand<T : CommandSender>(
    private val senderClass: Class<T>
) : TabCompleter, CommandExecutor {

    private val subCommands: MutableList<AsyncCommand<*>> = mutableListOf()

    abstract fun execute(sender: T, args: Array<String>)

    open fun executeAsync(sender: T, args: Array<String>): CompletableFuture<Void> =
        CompletableFuture.completedFuture(null)

    abstract fun tabComplete(sender: T, args: Array<String>): List<String>?

    open fun tabCompleteAsync(sender: T, args: Array<String>): CompletableFuture<List<String>> =
        CompletableFuture.completedFuture(emptyList())

    val metadata: CommandMetadata?
        get() = runCatching { this::class.java.getAnnotation(CommandMetadata::class.java) }.getOrNull()

    override fun onCommand(
        sender: CommandSender,
        cmd: Command,
        commandName: String,
        args: Array<out String>
    ): Boolean {
        if (sender is ConsoleCommandSender && !senderClass.isAssignableFrom(ConsoleCommandSender::class.java)) {
            sender.sendMessage(
                Component.text("This command can only be executed by a ${senderClass.simpleName}.")
                    .color(NamedTextColor.RED)
            )
            return true
        }

        if (!senderClass.isAssignableFrom(sender.javaClass)) {
            sender.sendMessage(
                Component.text("This command can only be executed by a ${senderClass.simpleName}.")
                    .color(NamedTextColor.RED)
            )
            return true
        }

        val meta = metadata
        if (meta == null) {
            println("Command ${this::class.java.simpleName} has no registered annotation!")
            return true
        }

        if (meta.permission.isNotEmpty() && !sender.hasPermission(meta.permission)) {
            sender.sendMessage(
                Component.text("You do not have permission to execute this command!")
                    .color(NamedTextColor.RED)
            )
            return true
        }

        @Suppress("UNCHECKED_CAST")
        val typedSender = sender as T
        val safeArgs = args.toList().toTypedArray()

        if (safeArgs.isEmpty()) {
            runExecuteSafe(typedSender, safeArgs, sender)
            return true
        }

        val subcommand = getSubcommand(safeArgs[0])
        if (subcommand == null) {
            runExecuteSafe(typedSender, safeArgs, sender)
            return true
        }

        val subArgs = safeArgs.copyOfRange(1, safeArgs.size)
        subcommand.onCommand(sender, cmd, commandName, subArgs)
        return true
    }

    private fun runExecuteSafe(typedSender: T, args: Array<String>, sender: CommandSender) {
        try {
            execute(typedSender, args)
        } catch (exception: Exception) {
            sender.sendMessage(
                Component.text("Something went wrong when executing this command! See console for details!")
                    .color(NamedTextColor.RED)
            )
            exception.printStackTrace()
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        cmd: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (sender is ConsoleCommandSender && !senderClass.isAssignableFrom(ConsoleCommandSender::class.java)) {
            return emptyList()
        }

        if (!senderClass.isAssignableFrom(sender.javaClass)) {
            return emptyList()
        }

        @Suppress("UNCHECKED_CAST")
        val typedSender = sender as T
        val safeArgs = args.toList().toTypedArray()

        val meta = metadata
        if (meta == null || (meta.permission.isNotEmpty() && !sender.hasPermission(meta.permission))) {
            return emptyList()
        }

        if (safeArgs.isEmpty()) {
            val tab = mutableListOf<String>()
            for (subCommand in subCommands) {
                val subMeta = subCommand.metadata ?: continue
                if (subMeta.permission.isEmpty() || typedSender.hasPermission(subMeta.permission)) {
                    tab += subCommand.onTabComplete(sender, cmd, "", safeArgs)
                }
            }
            tabComplete(typedSender, safeArgs)?.let { tab += it }
            return tab
        }

        val subcommand = getSubcommand(safeArgs[0])
        if (subcommand != null) {
            val subArgs = safeArgs.copyOfRange(1, safeArgs.size)
            return subcommand.onTabComplete(sender, cmd, alias, subArgs)
        }

        val tab = mutableListOf<String>()
        for (subCommand in subCommands) {
            val subMeta = subCommand.metadata
            if (subMeta != null && (subMeta.permission.isEmpty() || typedSender.hasPermission(subMeta.permission))) {
                tab += subMeta.command
            }
        }
        tabComplete(typedSender, safeArgs)?.let { tab += it }
        return tab
    }

    private fun getSubcommand(name: String): AsyncCommand<*>? {
        for (sub in subCommands) {
            val meta = sub.metadata ?: continue
            if (meta.command.equals(name, ignoreCase = true)) return sub
            if (meta.aliases.any { it.equals(name, ignoreCase = true) }) return sub
        }
        return null
    }

    protected fun addSubCommand(subCommand: AsyncCommand<*>) {
        subCommands += subCommand
    }
}
