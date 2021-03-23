package de.dseelp.kommon.command

import de.dseelp.kommon.command.arguments.BooleanArgument
import de.dseelp.kommon.command.arguments.IntArgument
import de.dseelp.kommon.command.arguments.ParsedArgument

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

    fun <T : Any> recursiveParse(
        parent: CommandNode<T>,
        args: Array<String>,
        currentResult: ParsedResult<T>
    ): ParsedResult<T> {
        if (args.isEmpty()) return currentResult.copy(node = parent)

        val copiedArgs = if (args.size > 1) args.copyOfRange(1, args.size) else arrayOf()
        val parseArgs = parseArgs(parent, args) ?: return currentResult.copy(
            failed = true,
            cause = ParsedResult.FailureCause.USAGE
        )
        if (parseArgs.first.isEmpty()) {
            return copy(currentResult, parseArgs.second).copy(node = parent)
        }
        val currentArg = parseArgs.first[0]
        //val child = getNode(currentArg, true, parent)
        //child?.let { return recursiveParse(it as CommandNode<T>, copiedArgs, currentResult) }
        for (child in parent.childs) {
            val parseArgs1 = parseArgs(child, parseArgs.first) ?: return currentResult.copy(
                failed = true,
                cause = ParsedResult.FailureCause.USAGE
            )
            val endArgs = if (parseArgs1.second.isEmpty()) copiedArgs else parseArgs1.first
            if (child.name?.equals(currentArg, child.ignoreCase) == true)
                return recursiveParse(
                child.target ?: child,
                endArgs,
                copy(currentResult, parseArgs.second+parseArgs1.second)
            )
            for (alias in child.aliases) {
                if (alias.equals(currentArg, child.ignoreCase))
                    return recursiveParse(
                    child.target ?: child,
                    endArgs,
                    copy(currentResult, parseArgs.second+parseArgs1.second)
                )
            }
            val idArg = child.argumentIdentifier
            val value = idArg?.get(currentArg) ?: continue
            //val shortend = (endArgs.toList()-endArgs[0]).toTypedArray()
            return recursiveParse(
                child.target ?: child,
                endArgs,
                copy(
                    currentResult,
                    parseArgs.second+parseArgs1.second + (idArg.name to ParsedArgument(idArg.name, idArg.optional, value))
                )
            )
        }
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

    fun <T : Any> parseArgs(
        node: CommandNode<T>,
        args: Array<String>
    ): Pair<Array<String>, Map<String, ParsedArgument<*>>>? {
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
                return null
            }

        }
        if (usedIndex != nArgs.lastIndex) {
            for (index in (if (usedIndex == -1) 0 else usedIndex)..nArgs.lastIndex) {
                if (!nArgs[index].optional) return null
            }
        }

        if (usedIndex == -1) return (args.toMutableList().apply { removeFirst() }.toTypedArray()) to parsed.map { it.name to it }.toMap()

        val data = if (args.isNotEmpty() && usedIndex > -1)
            args.copyOfRange(usedIndex+1, args.lastIndex+1)
        else
            arrayOf()

        return data to parsed.map { it.name to it }.toMap()
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
    val testSender = TestSender("Sender")
    dispatcher.register<TestSender>("test") {
        execute {
            println("Test")
        }
        argument(IntArgument("test1"))
        argument(BooleanArgument("bool")) {
            execute {
                println("Hi")
            }
            argument(BooleanArgument("bool2")) {
                execute {
                    println("Hi2")
                }
                argument(IntArgument("int")) {
                    execute {
                        println("Hi3")
                    }
                    argument(IntArgument("test2"))
                    argument(BooleanArgument("bool3")) {
                        execute {
                            println("Hi4")
                        }
                        argument(BooleanArgument("bool4")) {
                            execute {
                                println("Hi5")
                            }
                            argument(IntArgument("int1")) {
                                execute {
                                    println("Hi6")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    val result = dispatcher.parse<TestSender>("test 1 true false 5 1 true false 5")
    if (result?.node != null) {
        result.execute(testSender)
    }
    println(result)
}

class TestSender(val name: String)