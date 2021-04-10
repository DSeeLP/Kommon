package de.dseelp.kommon.command.arguments

class BooleanArgument(name: String, optional: Boolean = false, trueString: String = "true", falseString: String = "false") : Argument<Boolean>(name, optional) {
    constructor(name: String, optional: Boolean): this(name, optional, trueString = "true")
    override fun get(value: String): Boolean? = value.toBooleanOrNull()
    override fun getErrorMessage(): String = "%s is not a boolean"
    private val possibleValues = arrayOf(trueString, falseString)
    override fun complete(value: String): Array<String> = possibleValues.filter { value.toLowerCase().startsWith(it) }.toTypedArray()
}

fun String.toBooleanOrNull(trueString: String = "true", falseString: String = "false"): Boolean? = if (toLowerCase() == trueString) true else if (toLowerCase() == falseString) false else null