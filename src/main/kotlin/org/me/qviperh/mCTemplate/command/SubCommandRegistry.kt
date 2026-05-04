package org.me.qviperh.mCTemplate.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import java.util.Locale

class SubCommandRegistry<T : CommandSender>(private val parentCommand: String) {

    private val subcommands: MutableMap<String, SubCommand<T>> = LinkedHashMap()

    fun register(subcommand: SubCommand<T>) {
        subcommands[subcommand.name.lowercase(Locale.ROOT)] = subcommand
    }

    fun registerAll(vararg subcommands: SubCommand<T>) {
        subcommands.forEach { register(it) }
    }

    fun execute(sender: T, args: Array<String>): Boolean {
        if (args.isEmpty()) return false

        val subcommandName = args[0].lowercase(Locale.ROOT)
        val subcommand = subcommands[subcommandName]

        if (subcommand == null) {
            sender.sendMessage(Component.text("Unknown subcommand: $subcommandName").color(NamedTextColor.RED))
            return false
        }

        if (!subcommand.hasPermission(sender)) {
            sender.sendMessage(
                Component.text("You don't have permission to use this subcommand.").color(NamedTextColor.RED)
            )
            return false
        }

        val subArgs = args.copyOfRange(1, args.size)
        subcommand.execute(sender, subArgs)
        return true
    }

    fun tabComplete(sender: T, args: Array<String>): List<String> {
        if (args.size == 1) {
            val partial = args[0].lowercase(Locale.ROOT)
            return subcommands.entries
                .filter { it.key.startsWith(partial) && it.value.hasPermission(sender) }
                .map { it.key }
        }
        if (args.size > 1) {
            val subcommand = subcommands[args[0].lowercase(Locale.ROOT)] ?: return emptyList()
            if (!subcommand.hasPermission(sender)) return emptyList()
            val subArgs = args.copyOfRange(1, args.size)
            return subcommand.tabComplete(sender, subArgs) ?: emptyList()
        }
        return emptyList()
    }

    @Suppress("UNCHECKED_CAST")
    fun sendHelp(sender: CommandSender) {
        sender.sendMessage(
            Component.text("=== ${capitalize(parentCommand)} Command Help ===").color(NamedTextColor.GOLD)
        )
        for (subcommand in subcommands.values) {
            if (subcommand.hasPermission(sender as T)) {
                sender.sendMessage(
                    Component.text(subcommand.usage).color(NamedTextColor.YELLOW)
                        .append(Component.text(" - ${subcommand.description}").color(NamedTextColor.GRAY))
                )
            }
        }
    }

    fun getSubcommand(name: String): SubCommand<T>? = subcommands[name.lowercase(Locale.ROOT)]

    fun getAll(): Collection<SubCommand<T>> = subcommands.values.toList()

    fun contains(name: String): Boolean = subcommands.containsKey(name.lowercase(Locale.ROOT))

    private fun capitalize(word: String): String =
        if (word.isEmpty()) word else word.substring(0, 1).uppercase(Locale.ROOT) + word.substring(1)
}
