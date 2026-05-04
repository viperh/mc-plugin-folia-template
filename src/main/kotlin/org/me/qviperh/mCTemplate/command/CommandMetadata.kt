package org.me.qviperh.mCTemplate.command

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class CommandMetadata(
    val command: String,
    val aliases: Array<String> = [],
    val description: String = "",
    val usage: String = "",
    val permission: String = ""
)
