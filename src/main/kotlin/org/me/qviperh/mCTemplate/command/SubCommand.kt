package org.me.qviperh.mCTemplate.command

import org.bukkit.command.CommandSender

abstract class SubCommand<T : CommandSender> {

    private var cachedMetadata: CommandMetadata? = null

    abstract fun execute(sender: T, args: Array<String>)

    open fun tabComplete(sender: T, args: Array<String>): List<String>? = emptyList()

    fun getMetadata(): CommandMetadata? {
        if (cachedMetadata == null) {
            cachedMetadata = this::class.java.getAnnotation(CommandMetadata::class.java)
        }
        return cachedMetadata
    }

    val name: String
        get() = getMetadata()?.command ?: "unknown"

    val description: String
        get() = getMetadata()?.description ?: ""

    val usage: String
        get() = getMetadata()?.usage ?: ""

    val permission: String
        get() = getMetadata()?.permission ?: ""

    fun hasPermission(sender: T): Boolean =
        permission.isEmpty() || sender.hasPermission(permission)
}
