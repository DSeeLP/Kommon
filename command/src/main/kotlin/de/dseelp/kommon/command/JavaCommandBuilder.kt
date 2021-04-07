package de.dseelp.kommon.command

import de.dseelp.kommon.command.arguments.Argument
import java.util.function.Consumer

class JavaCommandBuilder<T : Any>(builder: CommandBuilder<T>) {
    private val builder = builder

    constructor(
        name: String? = null,
        argument: Argument<*>? = null,
        aliases: Array<String> = arrayOf()
    ) : this(CommandBuilder(name, argument, aliases))

    constructor(
        name: String? = null,
        aliases: Array<String> = arrayOf()
    ) : this(CommandBuilder(name, aliases = aliases))

    constructor(argument: Argument<*>) : this(CommandBuilder(argument = argument))

    fun node(builder: JavaCommandBuilder<T>): JavaCommandBuilder<T> {
        this.builder.node(builder.builder)
        return this
    }

    fun node(node: CommandNode<T>): JavaCommandBuilder<T> {
        this.builder.node(node)
        return this
    }

    fun execute(consumer: Consumer<CommandContext<T>>): JavaCommandBuilder<T> {
        builder.execute { consumer.accept(this) }
        return this
    }

    fun forward(node: CommandNode<T>): JavaCommandBuilder<T> {
        builder.forward(node)
        return this
    }

    fun build(): CommandNode<T> = builder.build()
}