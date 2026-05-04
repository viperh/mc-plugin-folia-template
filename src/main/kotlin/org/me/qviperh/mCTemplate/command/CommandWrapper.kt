package org.me.qviperh.mCTemplate.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class CommandWrapper(
    private val command: AsyncCommand<*>,
    val metadata: CommandMetadata
) : Command(
    metadata.command,
    metadata.description,
    metadata.usage,
    metadata.aliases.toList()
), CommandExecutor, TabCompleter {

    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean =
        command.onCommand(sender, this, commandLabel, args)

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): Boolean =
        execute(sender, label, args)

    override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): List<String> =
        command.onTabComplete(sender, this, alias, args)

    override fun onTabComplete(sender: CommandSender, cmd: Command, alias: String, args: Array<out String>): List<String> =
        tabComplete(sender, alias, args)
}
