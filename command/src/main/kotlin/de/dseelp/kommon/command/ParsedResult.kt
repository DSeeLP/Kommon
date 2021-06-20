package de.dseelp.kommon.command

/**
 * @author DSeeLP
 */
data class ParsedResult<S: Any>(
    val root: CommandNode<S>,
    val context: CommandContext<S>,
    val node: CommandNode<S>? = null,
    val failed: Boolean,
    val cause: FailureCause? = null,
    val errorMessage: String? = null
) {
    enum class FailureCause {
        USAGE,
    }

    fun execute(bypassAccess: Boolean = false): Throwable? {
        if (node == null) return null
        val context = context.copy(parameters = node.parameters)
        val rootAccess = root.checkAccess.invoke(context)
        val nodeAccess = node.checkAccess.invoke(context)
        val access = if (nodeAccess) {
            rootAccess
        }else false
        if (!access && !bypassAccess) {
            return runCatching {
                (if (root === node) root.noAccess ?: {} else node!!.noAccess ?: (root.noAccess ?: {})).invoke(
                    context,
                    node
                )
            }.exceptionOrNull()?.cause
        }
        return runCatching { node!!.executor?.invoke(context) }.exceptionOrNull()?.cause
    }
}

