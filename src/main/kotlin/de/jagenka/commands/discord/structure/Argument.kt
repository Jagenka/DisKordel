package de.jagenka.commands.discord.structure

interface Argument<T>
{
    val id: String
    val displayInHelp: String
        get() = "<$id>"

    fun isOfType(word: String): Boolean
    fun convertToType(word: String): T?

    companion object
    {
        fun literal(id: String) = LiteralArgument(id)
        fun string(id: String = "string") = StringArgument(id)
        fun int(id: String = "int") = IntArgument(id)
        fun double(id: String = "double") = DoubleArgument(id)
    }
}

class LiteralArgument(override val id: String) : Argument<String>
{
    override val displayInHelp: String
        get() = id

    override fun isOfType(word: String): Boolean
    {
        return word == id
    }

    override fun convertToType(word: String): String? = null
}

class StringArgument(override val id: String) : Argument<String>
{
    override fun isOfType(word: String) = true

    override fun convertToType(word: String) = word
}

class IntArgument(override val id: String) : Argument<Int>
{
    override fun isOfType(word: String): Boolean
    {
        return word.toIntOrNull() != null
    }

    override fun convertToType(word: String): Int?
    {
        return word.toIntOrNull()
    }
}

class DoubleArgument(override val id: String) : Argument<Double>
{
    override fun isOfType(word: String): Boolean
    {
        return word.toDoubleOrNull() != null
    }

    override fun convertToType(word: String): Double?
    {
        return word.toDoubleOrNull()
    }
}