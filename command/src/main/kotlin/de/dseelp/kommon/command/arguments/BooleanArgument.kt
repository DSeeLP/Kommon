package de.dseelp.kommon.command.arguments

import de.dseelp.kommon.command.CommandContext

/**
 * @author DSeeLP
 */
class BooleanArgument<S : Any> @JvmOverloads constructor(
    name: String,
    trueString: String = "true",
    falseString: String = "false"
) : Argument<S, Boolean>(name) {
    override suspend fun get(context: CommandContext<S>, value: String): Boolean? = value.toBooleanOrNull()
    override fun getErrorMessage(): String = "%s is not a boolean"
    private val possibleValues = arrayOf(trueString, falseString)
    override fun complete(context: CommandContext<S>, value: String): Array<String> =
        possibleValues.filter { it.lowercase().startsWith(value) }.toTypedArray()
}

fun String.toBooleanOrNull(trueString: String = "true", falseString: String = "false"): Boolean? =
    if (lowercase() == trueString.lowercase()) true else if (lowercase() == falseString.lowercase()) false else null