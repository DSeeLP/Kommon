package de.dseelp.kommon.command

data class ParsedResult<T: Any>(
    val context: CommandContext<T>,
    val node: CommandNode<T>? = null,
    val failed: Boolean,
    val cause: FailureCause? = null
) {
    enum class FailureCause {
        USAGE
    }
    fun execute(sender: T) {
        node?.executor?.invoke(context.copy().apply { this.sender = sender })
    }
}

