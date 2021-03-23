package de.dseelp.kommon.command.arguments

data class ParsedArgument<T>(val name: String, val optional: Boolean, val value: T?)