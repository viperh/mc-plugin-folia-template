package org.me.qviperh.mCTemplate.command.example.test.sub

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.me.qviperh.mCTemplate.command.CommandMetadata
import org.me.qviperh.mCTemplate.command.SubCommand

@CommandMetadata(
    command = "hello",
    description = "Say hello",
    usage = "/test hello"
)
class HelloSubcommand : SubCommand<CommandSender>() {

    override fun execute(sender: CommandSender, args: Array<String>) {
        sender.sendMessage(
            Component.text("Hello, ").color(NamedTextColor.GREEN)
                .append(Component.text(sender.name).color(NamedTextColor.GOLD))
                .append(Component.text("!").color(NamedTextColor.GREEN))
        )
    }
}
