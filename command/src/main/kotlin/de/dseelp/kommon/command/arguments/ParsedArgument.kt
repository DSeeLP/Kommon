package de.dseelp.kommon.command.arguments

data class ParsedArgument<T: Any>(val name: String, val optional: Boolean, val value: T?)