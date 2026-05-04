package org.me.qviperh.mCTemplate.command

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.plugin.java.JavaPlugin
import java.io.IOException
import java.lang.reflect.Modifier
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.LinkedHashSet
import java.util.Locale
import java.util.jar.JarFile
import java.util.logging.Level

class AsyncCommandManager(private val plugin: JavaPlugin) {

    private val commandPackage: String = AsyncCommandManager::class.java.packageName

    fun registerCommands() {
        val commands = discoverAsyncCommands()
        var registered = 0
        val registeredNames = mutableSetOf<String>()

        for (command in commands) {
            val metadata = command.metadata
            if (metadata == null || metadata.command.isBlank()) {
                plugin.logger.warning("Skipping command with missing metadata: ${command::class.java.name}")
                continue
            }

            val normalized = metadata.command.lowercase(Locale.ROOT)
            if (!registeredNames.add(normalized)) {
                plugin.logger.warning("Duplicate command name '${metadata.command}' from ${command::class.java.name}")
                continue
            }

            if (registerCommand(command, metadata)) registered++
        }

        plugin.logger.info("Registered $registered async command(s).")
    }

    private fun registerCommand(command: AsyncCommand<*>, metadata: CommandMetadata): Boolean {
        val name = metadata.command
        val pluginCommand = plugin.getCommand(name)
        val wrapper = CommandWrapper(command, metadata)

        if (pluginCommand == null) {
            applyMetadata(wrapper, metadata)
            val ok = Bukkit.getCommandMap().register(plugin.name.lowercase(Locale.ROOT), wrapper)
            if (!ok) plugin.logger.warning("Command '$name' failed dynamic registration.")
            return ok
        }

        pluginCommand.setExecutor(wrapper)
        pluginCommand.tabCompleter = wrapper
        applyMetadata(pluginCommand, metadata)
        return true
    }

    private fun applyMetadata(command: Command, metadata: CommandMetadata) {
        if (metadata.description.isNotBlank()) command.description = metadata.description
        if (metadata.usage.isNotBlank()) command.usage = metadata.usage
        if (metadata.aliases.isNotEmpty()) command.aliases = metadata.aliases.toMutableList()
        if (metadata.permission.isNotBlank()) command.permission = metadata.permission
    }

    private fun discoverAsyncCommands(): List<AsyncCommand<*>> {
        val classes = findAsyncCommandClasses()
        val commands = mutableListOf<AsyncCommand<*>>()
        for (cls in classes) {
            createCommandInstance(cls)?.let { commands += it }
        }
        return commands
    }

    private fun findAsyncCommandClasses(): Set<Class<out AsyncCommand<*>>> {
        val classes = LinkedHashSet<Class<out AsyncCommand<*>>>()
        val packagePath = commandPackage.replace('.', '/')
        val location = plugin::class.java.protectionDomain.codeSource?.location

        if (location == null) {
            plugin.logger.warning("Unable to determine plugin location for command discovery.")
            return classes
        }

        try {
            val root: Path = Paths.get(location.toURI())
            when {
                Files.isRegularFile(root) && root.toString().endsWith(".jar") -> scanJar(root, packagePath, classes)
                Files.isDirectory(root) -> scanDirectory(root, packagePath, classes)
                else -> plugin.logger.warning("Unknown plugin location type for command discovery: $root")
            }
        } catch (e: URISyntaxException) {
            plugin.logger.log(Level.WARNING, "Failed to resolve plugin location for command discovery.", e)
        }

        return classes
    }

    private fun scanJar(jarPath: Path, packagePath: String, classes: MutableSet<Class<out AsyncCommand<*>>>) {
        try {
            JarFile(jarPath.toFile()).use { jarFile ->
                val entries = jarFile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name
                    if (!name.startsWith(packagePath) || !name.endsWith(".class") || name.contains('$')) continue
                    val className = name.substring(0, name.length - ".class".length).replace('/', '.')
                    tryLoadAsyncCommandClass(className, classes)
                }
            }
        } catch (e: IOException) {
            plugin.logger.log(Level.WARNING, "Failed to scan plugin jar for async commands.", e)
        }
    }

    private fun scanDirectory(root: Path, packagePath: String, classes: MutableSet<Class<out AsyncCommand<*>>>) {
        val baseDir = root.resolve(packagePath)
        if (!Files.exists(baseDir)) {
            plugin.logger.warning("Command package not found for discovery: $baseDir")
            return
        }

        try {
            Files.walk(baseDir).use { stream ->
                stream.filter { it.toString().endsWith(".class") }.forEach { path ->
                    val relative = baseDir.relativize(path).toString()
                    var className = ("$commandPackage.$relative")
                        .replace('\\', '.')
                        .replace('/', '.')
                    className = className.substring(0, className.length - ".class".length)
                    if (!className.contains('$')) tryLoadAsyncCommandClass(className, classes)
                }
            }
        } catch (e: IOException) {
            plugin.logger.log(Level.WARNING, "Failed to scan classes directory for async commands.", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun tryLoadAsyncCommandClass(className: String, classes: MutableSet<Class<out AsyncCommand<*>>>) {
        try {
            val clazz = Class.forName(className, false, plugin::class.java.classLoader)
            if (!AsyncCommand::class.java.isAssignableFrom(clazz)) return
            if (clazz.isInterface || Modifier.isAbstract(clazz.modifiers)) return
            classes += clazz as Class<out AsyncCommand<*>>
        } catch (e: ClassNotFoundException) {
            plugin.logger.log(Level.WARNING, "Failed to load async command class: $className", e)
        } catch (e: LinkageError) {
            plugin.logger.log(Level.WARNING, "Failed to load async command class: $className", e)
        }
    }

    private fun createCommandInstance(commandClass: Class<out AsyncCommand<*>>): AsyncCommand<*>? {
        try {
            val ctor = commandClass.getDeclaredConstructor(plugin::class.java)
            return ctor.newInstance(plugin)
        } catch (_: NoSuchMethodException) {
            // fall through to no-arg constructor
        } catch (e: ReflectiveOperationException) {
            plugin.logger.log(Level.WARNING, "Failed to instantiate async command: ${commandClass.name}", e)
            return null
        }

        return try {
            val ctor = commandClass.getDeclaredConstructor()
            ctor.newInstance()
        } catch (_: NoSuchMethodException) {
            plugin.logger.warning("No suitable constructor found for async command: ${commandClass.name}")
            null
        } catch (e: ReflectiveOperationException) {
            plugin.logger.log(Level.WARNING, "Failed to instantiate async command: ${commandClass.name}", e)
            null
        }
    }
}
