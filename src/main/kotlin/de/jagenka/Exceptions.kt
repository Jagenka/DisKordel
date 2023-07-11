package de.jagenka

class MissingConfigException(s: String) : RuntimeException(s)
class InvalidConfigException(s: String) : RuntimeException(s)
class BotInitializationException : RuntimeException
{
    constructor(s: String) : super(s)
    constructor(e: Exception) : super(e)
}