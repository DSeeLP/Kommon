package de.dseelp.kommon.command

import kotlinx.coroutines.runBlocking

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
    val defaultCheckAccess: suspend (result: ParsedResult<S>) -> Boolean = { result ->
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

    suspend fun execute(
        checkAccess: suspend ParsedResult<S>.() -> Boolean
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

    fun executeBlocking(checkAccess: ParsedResult<S>.() -> Boolean) =
        runBlocking {
            execute {
                checkAccess.invoke(this@ParsedResult)
            }
        }


    suspend fun execute(bypassAccess: Boolean = false) =
        if (bypassAccess) execute { true } else execute(defaultCheckAccess)

    fun executeBlocking(bypassAccess: Boolean = false) = runBlocking { execute(bypassAccess) }
}

