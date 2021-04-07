package de.dseelp.kommon.command

import de.dseelp.kommon.command.arguments.Argument

class CommandBuilder<T: Any>(
    val name: String? = null,
    val argument: Argument<*>? = null,
    val aliases: Array<String> = arrayOf(),
) {
    var target: CommandNode<T>? = null
        private set
    var arguments: Array<Argument<*>> = arrayOf()
        private set
    var childs: Array<CommandNode<T>> = arrayOf()
        private set

    var executeBlock: (CommandContext<T>.() -> Unit)? = null
    private set

    fun execute(block: CommandContext<T>.() -> Unit) {
        executeBlock = block
    }

    fun forward(node: CommandNode<T>) {
        target = node
    }

    fun node(name: String, aliases: Array<String> = arrayOf(), block: CommandBuilder<T>.() -> Unit) {
        childs += CommandBuilder<T>(name, aliases = aliases).apply(block).build()
    }

    fun node(builder: CommandBuilder<T>) {
        childs += builder.build()
    }

    fun node(node: CommandNode<T>) {
        childs += node
    }

    fun argument(argument: Argument<*>, block: CommandBuilder<T>.() -> Unit) {
        childs += CommandBuilder<T>(argument = argument).apply(block).build()
    }

    /*fun argument(argument: Argument<*>) {
        arguments+=argument
    }*/

    fun build(): CommandNode<T> {
        if (name == null && argument == null) throw IllegalStateException("An command node must have a name or an argument!")
        if (target != null && childs.isNotEmpty()) throw IllegalStateException("Cannot forward a node with children")
        if (target != null && arguments.isNotEmpty()) throw IllegalStateException("Cannot forward a node with arguments")
        return CommandNode(name, aliases, argument, target, arguments, childs, executeBlock)
    }

    operator fun invoke(block: CommandBuilder<T>.() -> Unit) {
        this.apply(block)
    }
}

fun <T: Any> command(name: String, block: CommandBuilder<T>.() -> Unit): CommandNode<T> = CommandBuilder<T>(name).apply(block).build()