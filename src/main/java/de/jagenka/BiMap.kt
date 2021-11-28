package de.jagenka

class BiMap<K, V>
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

    fun getValueForKey(key: K): V?
    {
        return map[key]
    }

    fun getKeyForValue(value: V): K?
    {
        return inv[value]
    }

    fun getAsSet(): Set<Pair<K, V>>
    {
        val set = HashSet<Pair<K, V>>()
        map.forEach { (key, value) -> set.add(Pair(key, value)) }
        return set
    }
}