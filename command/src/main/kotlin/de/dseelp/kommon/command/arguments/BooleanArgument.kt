package de.dseelp.kommon.command.arguments

class BooleanArgument(name: String, optional: Boolean = false) : Argument<Boolean>(name, optional) {
    override fun get(value: String): Boolean? = value.toBooleanOrNull()
    override val errorMessage = "%s is not a boolean"
    private val possibleValues = arrayOf("true", "false")
    override fun complete(value: String): Array<String> = possibleValues.filter { value.toLowerCase().startsWith(it) }.toTypedArray()
}

fun String.toBooleanOrNull(): Boolean? = if (toLowerCase() == "true") true else if (toLowerCase() == "false") false else null