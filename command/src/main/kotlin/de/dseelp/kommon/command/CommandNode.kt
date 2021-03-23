package de.dseelp.kommon.command

import de.dseelp.kommon.command.arguments.Argument

data class CommandNode<T: Any>(
    val name: String? = null,
    val aliases: Array<String> = arrayOf(),
    val argumentIdentifier: Argument<*>? = null,
    val target: CommandNode<T>? = null,
    val arguments: Array<Argument<*>> = arrayOf(),
    val childs: Array<CommandNode<T>> = arrayOf(),
    val parent: CommandNode<T>? = null,
    val executor: (CommandContext<T>.() -> Unit)?,
    val ignoreCase: Boolean = true
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CommandNode<*>

        if (name != other.name) return false
        if (!aliases.contentEquals(other.aliases)) return false
        if (argumentIdentifier != other.argumentIdentifier) return false
        if (target != other.target) return false
        if (!arguments.contentEquals(other.arguments)) return false
        if (!childs.contentEquals(other.childs)) return false
        if (executor != other.executor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + aliases.contentHashCode()
        result = 31 * result + (argumentIdentifier?.hashCode() ?: 0)
        result = 31 * result + (target?.hashCode() ?: 0)
        result = 31 * result + arguments.contentHashCode()
        result = 31 * result + childs.contentHashCode()
        result = 31 * result + (executor?.hashCode() ?: 0)
        return result
    }
}