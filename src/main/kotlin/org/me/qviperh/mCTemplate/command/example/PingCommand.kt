package org.me.qviperh.mCTemplate.command.example

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.me.qviperh.mCTemplate.command.AsyncCommand
import org.me.qviperh.mCTemplate.command.CommandMetadata

@CommandMetadata(
    command = "ping",
    description = "Pings the server",
    usage = "/ping",
    aliases = ["pong"]
)
class PingCommand : AsyncCommand<CommandSender>(CommandSender::class.java) {

    override fun execute(sender: CommandSender, args: Array<String>) {
        sender.sendMessage(Component.text("Pong!").color(NamedTextColor.GREEN))
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> = emptyList()
}
