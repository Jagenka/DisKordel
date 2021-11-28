package de.jagenka

abstract class BiMap<K, V> //TODO abstract weg
{
    private val map = HashMap<K, V>()
    private val inv = HashMap<V, K>()

    fun put(key: K, value: V)
    {
        inv.remove(map[key])
        map.remove(inv[value])
        map[key] = value
        inv[value] = key
    }

    fun containsKey(key: K): Boolean
    {
        return map.containsKey(key)
    }

    fun containsValue(value: V): Boolean
    {
        return inv.containsKey(value)
    }

    fun getValueForKey(key: K): V?
    {
        return map[key]
    }

    fun getKeyForValue(value: V): K?
    {
        return inv[value]
    }

    fun keys(): Set<K>
    {
        return map.keys
    }

    fun values(): Set<V>
    {
        return inv.keys
    }

    fun getAsSet(): Set<Pair<K, V>>
    {
        val set = HashSet<Pair<K, V>>()
        map.forEach { (key, value) -> set.add(Pair(key, value)) }
        return set
    }
}