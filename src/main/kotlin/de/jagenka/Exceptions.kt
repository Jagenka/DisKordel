package de.jagenka

class MissingConfigException(s: String) : RuntimeException(s)
class InvalidConfigException(s: String) : RuntimeException(s)
class BotInitializationException : RuntimeException
{
    constructor(s: String) : super(s)
    constructor(e: Exception) : super(e)
}

class KordInstanceMissingException(s: String) : RuntimeException(s)

class WebhookInitException(s: String) : RuntimeException(s)

class StatDataException(val type: StatDataExceptionType) : RuntimeException()

enum class StatDataExceptionType(val response: String)
{
    EMPTY("Nothing found."),
    INVALID_ID("Invalid stat identifier."),
    ONLY_ZERO("Only zero(es) found.")
}