package org.me.qviperh.mCTemplate.command.example.test

import org.bukkit.command.CommandSender
import org.me.qviperh.mCTemplate.command.AsyncCommand
import org.me.qviperh.mCTemplate.command.CommandMetadata
import org.me.qviperh.mCTemplate.command.SubCommandRegistry
import org.me.qviperh.mCTemplate.command.example.test.sub.EchoSubcommand
import org.me.qviperh.mCTemplate.command.example.test.sub.HelloSubcommand

@CommandMetadata(
    command = "test",
    description = "Test command with subcommands",
    usage = "/test <subcommand> [args...]",
    aliases = ["t"]
)
class TestCommand : AsyncCommand<CommandSender>(CommandSender::class.java) {

    private val registry = SubCommandRegistry<CommandSender>("test").apply {
        registerAll(
            HelloSubcommand(),
            EchoSubcommand()
        )
    }

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            registry.sendHelp(sender)
            return
        }
        registry.execute(sender, args)
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> =
        registry.tabComplete(sender, args)
}
