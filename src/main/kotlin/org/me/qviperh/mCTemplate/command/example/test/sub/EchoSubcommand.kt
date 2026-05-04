package org.me.qviperh.mCTemplate.command.example.test.sub

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.me.qviperh.mCTemplate.command.CommandMetadata
import org.me.qviperh.mCTemplate.command.SubCommand

@CommandMetadata(
    command = "echo",
    description = "Echo a message",
    usage = "/test echo <message>"
)
class EchoSubcommand : SubCommand<CommandSender>() {

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Usage: /test echo <message>").color(NamedTextColor.RED))
            return
        }
        val message = args.joinToString(" ")
        sender.sendMessage(
            Component.text("Echo: ").color(NamedTextColor.GRAY)
                .append(Component.text(message).color(NamedTextColor.WHITE))
        )
    }
}
