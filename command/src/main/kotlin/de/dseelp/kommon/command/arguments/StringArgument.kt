package de.dseelp.kommon.command.arguments

class StringArgument(name: String, optional: Boolean = false) : Argument<String>(name, optional) {
    override fun get(value: String): String = value
    override fun getErrorMessage(): String = "This should not happen!"
    override fun complete(value: String): Array<String>? = null
}