package de.dseelp.kommon.command.arguments

class IntArgument(name: String, optional: Boolean = false) : Argument<Int>(name, optional) {
    override fun get(value: String): Int? = value.toIntOrNull()
    override fun getErrorMessage(): String = "%s is not an Integer"
    override fun complete(value: String): Array<String>? = null
}