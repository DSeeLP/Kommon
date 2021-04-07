package de.dseelp.kommon.command.arguments

import java.util.*

class UUIDArgument(name: String, optional: Boolean = false) : Argument<UUID>(name, optional) {
    override fun get(value: String): UUID? = value.toUUIDOrNull()
    override fun getErrorMessage(): String = "%s is not a UUID"
    override fun complete(value: String): Array<String>? = null
}

fun String.toUUIDOrNull(): UUID? = try {
    UUID.fromString(this)
}catch (ex: IllegalArgumentException) {
    null
}