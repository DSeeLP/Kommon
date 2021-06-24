package de.dseelp.kommon.command

/**
 * @author DSeeLP
 */
data class ParsedResult<S : Any>(
    val root: CommandNode<S>,
    val context: CommandContext<S>,
    val node: CommandNode<S>? = null,
    val failed: Boolean,
    val cause: FailureCause? = null,
    val errorMessage: String? = null,
    val defaultCheckAccess: (result: ParsedResult<S>) -> Boolean = { result ->
        val rootAccess = result.root.checkAccess.invoke(context)
        val nodeAccess = result.node?.checkAccess?.invoke(context) ?: false
        if (nodeAccess) {
            rootAccess
        } else false
    }
) {
    enum class FailureCause {
        USAGE,
    }

    fun execute(
        checkAccess: ParsedResult<S>.() -> Boolean
    ): Throwable? {
        if (node == null) return null
        val context = context.copy(parameters = node.parameters)
        if (!checkAccess.invoke(this)) {
            return runCatching {
                (if (root === node) root.noAccess ?: {} else node!!.noAccess ?: (root.noAccess ?: {})).invoke(
                    context,
                    node
                )
            }.exceptionOrNull()
        }
        return runCatching { node!!.executor?.invoke(context) }.exceptionOrNull()
    }

    fun execute(bypassAccess: Boolean = false) = if (bypassAccess) execute { true } else execute(defaultCheckAccess)
}

