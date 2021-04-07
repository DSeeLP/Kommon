package de.dseelp.kommon.command

import de.dseelp.kommon.command.arguments.ParsedArgument
import de.dseelp.kommon.command.arguments.StringArgument

class CommandDispatcher {
    private val nodes = mutableListOf<CommandNode<*>>()

    fun register(node: CommandNode<*>) {
        if (node.name == null) throw IllegalArgumentException("The root node must have a name!")
        nodes.add(node)
    }

    fun <T : Any> register(name: String, block: CommandBuilder<T>.() -> Unit) {
        register(command(name, block))
    }

    fun unregister(name: String) {
        val node = getNode(name) ?: return
        nodes.remove(node)
    }

    private fun getNode(name: String, useAliases: Boolean = false, parent: CommandNode<*>? = null): CommandNode<*>? {
        val lowercaseName = name.toLowerCase()
        for (node in parent?.childs?.toMutableList() ?: nodes) {
            if (node.name!!.toLowerCase() == lowercaseName) return node
            if (useAliases) {
                for (alias in node.aliases) {
                    if (alias.toLowerCase() == lowercaseName) return node
                }
            }
        }
        return null
    }

    private fun <T : Any> recursiveParse(
        parent: CommandNode<T>,
        args: Array<String>,
        currentResult: ParsedResult<T>
    ): ParsedResult<T> {
        if (args.isEmpty()) return currentResult.copy(node = parent)

        val copiedArgs = if (args.size > 1) args.copyOfRange(1, args.size) else arrayOf()
        val currentArg = args[0]

        //val child = getNode(currentArg, true, parent)
        //child?.let { return recursiveParse(it as CommandNode<T>, copiedArgs, currentResult) }
        for (child in parent.childs) {

            val endArgs = copiedArgs
            if (child.name?.equals(currentArg, child.ignoreCase) == true)
                return recursiveParse(
                    child.target ?: child,
                    endArgs,
                    currentResult.copy(node = child)
                )
            for (alias in child.aliases) {
                if (alias.equals(currentArg, child.ignoreCase))
                    return recursiveParse(
                        child.target ?: child,
                        endArgs,
                        currentResult.copy(node = child)
                    )
            }
            val idArg = child.argumentIdentifier
            val value = idArg?.get(currentArg) ?: continue
            //val shortend = (endArgs.toList()-endArgs[0]).toTypedArray()
            return recursiveParse(
                child.target ?: child,
                endArgs,
                copy(
                    currentResult, mapOf(idArg.name to ParsedArgument(idArg.name, idArg.optional, value))
                )
            )
        }
        val parseArgs = parseArgs(parent, args)
        if (parseArgs.ok && parseArgs.noArg && parent.arguments.isNotEmpty()) return currentResult.copy(node = parent)
        if (parent.executor != null) return currentResult.copy(node = parent)
        return currentResult.copy(failed = true, cause = ParsedResult.FailureCause.USAGE)
    }

    private fun <T : Any> copy(result: ParsedResult<T>, parseArgs: Map<String, ParsedArgument<*>>) =
        result.copy(context = result.context.copy(args = result.context.args + parseArgs))

    fun <T : Any> parse(command: String): ParsedResult<T>? {
        val splitted = parseRaw(command)
        if (splitted.isEmpty()) {
            return null
        }
        val name = splitted[0]
        val node = getNode(name, useAliases = true) ?: return null
        return recursiveParse(
            node as CommandNode<T>,
            if (splitted.size == 1) arrayOf() else splitted.copyOfRange(1, splitted.size),
            ParsedResult(
                CommandContext(
                    mapOf()
                ), failed = false
            )
        )
    }

    fun <T : Any> parse(command: String, type: Class<T>) = parse<T>(command)

    private data class ParseArgsResult(
        val ok: Boolean,
        val newArgs: Array<String> = arrayOf(),
        val parsed: Map<String, ParsedArgument<*>> = mapOf(),
        val failedArg: Boolean = false,
        val usage: Boolean = false,
        val noArg: Boolean = false
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ParseArgsResult

            if (ok != other.ok) return false
            if (!newArgs.contentEquals(other.newArgs)) return false
            if (parsed != other.parsed) return false
            if (failedArg != other.failedArg) return false

            return true
        }

        override fun hashCode(): Int {
            var result = ok.hashCode()
            result = 31 * result + newArgs.contentHashCode()
            result = 31 * result + parsed.hashCode()
            result = 31 * result + failedArg.hashCode()
            return result
        }
    }

    private fun <T : Any> parseArgs(
        node: CommandNode<T>,
        args: Array<String>
    ): ParseArgsResult {
        val parsed = mutableListOf<ParsedArgument<*>>()
        val nArgs = node.arguments
        var usedIndex = -1

        for (index in args.indices) {
            val s = args[index]
            if (nArgs.size <= index) break
            val arg = nArgs[index]
            val value = arg.get(s)
            if (value != null) {
                usedIndex = index
                parsed.add(ParsedArgument(arg.name, arg.optional, value))
            } else {
                if (arg.optional) continue
                //TODO: Error Handling
                return ParseArgsResult(false, usage = true)
            }

        }
        if (usedIndex != nArgs.lastIndex) {
            for (index in (if (usedIndex == -1) 0 else usedIndex)..nArgs.lastIndex) {
                if (!nArgs[index].optional) return ParseArgsResult(false, failedArg = true)
            }
        }

        if (usedIndex == -1) return ParseArgsResult(true, noArg = true)

        val data = if (args.isNotEmpty() && usedIndex > -1)
            args.copyOfRange(usedIndex + 1, args.lastIndex + 1)
        else
            arrayOf()

        return ParseArgsResult(true, data, parsed.map { it.name to it }.toMap())
    }

    private fun parseRaw(value: String): Array<String> {
        val rawSplitted = value.split(' ')
        val splitted = mutableListOf<String>()
        var inS = false
        var raw = ""
        for (s in rawSplitted) {
            if (!inS && s.startsWith('"')) {
                if (s == "\"\"") continue
                inS = true
                raw = if (s == "\" ") " "
                else "${s.replaceFirst("\"", "")} "
                continue
            }
            if (inS) {
                if (s.endsWith('"')) {
                    inS = false
                    if (s.length in 1..1) {
                        raw += " "
                    } else if (s.length > 1) {
                        raw += s.substring(0 until s.lastIndex)
                    }
                    splitted.add(raw)
                    raw = ""
                    continue
                }
                raw += "$s "
            } else splitted.add(s)
        }
        if (inS) splitted.add(raw.substring(0 until raw.lastIndex))
        return splitted.toTypedArray()
    }
}

fun main() {
    val dispatcher = CommandDispatcher()

    command<String>("foo") {
        execute { println("Foo") }
        node("test") {
            execute { println("Hallo") }
            argument(StringArgument("test")) {
                execute {
                    println(get<String>("test"))
                }
                node("string") {
                    execute {
                        println("String called with arg: " + get<String>("test"))
                    }
                }
            }
        }
    }

    val parsed = dispatcher.parse<String>("foo test bareee foo2")
    parsed?.execute("TestSender")
    println(parsed)
}