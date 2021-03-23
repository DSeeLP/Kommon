package de.dseelp.kommon.logging

class Logger(val name: String, val parent: Logger? = null) {
    var displayName: String? = null
}