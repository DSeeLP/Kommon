package de.dseelp.kommon.command.arguments

abstract class Argument<T>(val name: String, val optional: Boolean = false) {
    abstract fun get(value: String): T?
    abstract fun complete(value: String): Array<String>?
    open fun getErrorMessage(): String = "The string %s does not match the argument"
}