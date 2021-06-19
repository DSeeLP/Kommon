package de.dseelp.kommon.command.arguments

import de.dseelp.kommon.command.CommandContext

/**
 * @author DSeeLP
 */
abstract class Argument<S : Any, T: Any>(val name: String) {
    @Deprecated(
        "This is deprecated in favor of get(context, value) and will later be removed",
        level = DeprecationLevel.WARNING
    )
    open fun get(value: String): T? = null
    open fun get(context: CommandContext<S>, value: String): T? = get(value)
    abstract fun complete(context: CommandContext<S>, value: String): Array<String>
    open fun getErrorMessage(): String = "The string %s does not match the argument"

    protected fun Array<String>.filterPossibleMatches(value: String): Array<String> {
        if (value.isBlank() || value.isEmpty()) return this
        return this.filter { it.startsWith(value, ignoreCase = true) }.toTypedArray()
    }

    protected fun Collection<String>.filterPossibleMatches(value: String) = toTypedArray().filterPossibleMatches(value)
}