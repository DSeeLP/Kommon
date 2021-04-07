package de.dseelp.kommon.command

import de.dseelp.kommon.command.arguments.ParsedArgument

data class CommandContext<T : Any>(
    val args: Map<String, ParsedArgument<*>>
) {
    lateinit var sender: T
        internal set

    inline operator fun <reified T> get(key: String): T {
        if (!args.containsKey(key)) throw IllegalArgumentException("An argument with the name $key doesn't exist")
        val value = args[key]!!
        if (value.optional) throw IllegalArgumentException("$key is optional use optional(key: String) instead")
        if (value.value is T) return value.value
        throw IllegalArgumentException("$key is not of the type ${T::class.simpleName}")
    }

    inline fun <reified T> optional(key: String): T? {
        if (!args.containsKey(key)) throw IllegalArgumentException("An argument with the name $key doesn't exist")
        val value = args[key]!!
        if (value.value == null) return null
        if (value.value is T) return value.value
        throw IllegalArgumentException("$key is not of the type ${T::class.simpleName}")
    }

    fun <T> get(key: String, type: Class<T>): T {
        if (!args.containsKey(key)) throw IllegalArgumentException("An argument with the name $key doesn't exist")
        val value = args[key]!!
        if (value.optional) throw IllegalArgumentException("$key is optional use optional(key: String) instead")
        if (type == value.value!!::class.java) return value.value as T
        throw IllegalArgumentException("$key is not of the type ${type.simpleName}")
    }

    fun <T> optional(key: String, type: Class<T>): T? {
        if (!args.containsKey(key)) throw IllegalArgumentException("An argument with the name $key doesn't exist")
        val value = args[key]!!
        if (value.value == null) return null
        if (type == value.value::class.java) return value.value as T
        throw IllegalArgumentException("$key is not of the type ${type.simpleName}")
    }
}