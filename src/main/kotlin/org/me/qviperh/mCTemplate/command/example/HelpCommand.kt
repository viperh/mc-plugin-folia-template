package org.me.qviperh.mCTemplate.command.example

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.me.qviperh.mCTemplate.command.AsyncCommand
import org.me.qviperh.mCTemplate.command.CommandMetadata

@CommandMetadata(
    command = "help",
    description = "View all available commands",
    usage = "/help",
    aliases = ["?"]
)
class HelpCommand : AsyncCommand<CommandSender>(CommandSender::class.java) {

    override fun execute(sender: CommandSender, args: Array<String>) {
        sender.sendMessage(
            Component.text("=== Available Commands ===")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
        )

        for (command in Bukkit.getCommandMap().knownCommands.values) {
            val permission = command.permission
            if (!permission.isNullOrEmpty() && !sender.hasPermission(permission)) continue

            val description = command.description.ifEmpty { "No description" }
            sender.sendMessage(
                Component.text("/${command.name}").color(NamedTextColor.YELLOW)
                    .append(Component.text(" - ").color(NamedTextColor.GRAY))
                    .append(Component.text(description).color(NamedTextColor.GRAY))
            )

            if (command.usage.isNotEmpty()) {
                sender.sendMessage(
                    Component.text("  ${command.usage}").color(NamedTextColor.DARK_GRAY)
                )
            }
        }
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> = emptyList()
}
