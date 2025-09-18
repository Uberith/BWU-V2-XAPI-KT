package net.botwithus.kxapi.game.query.internal

import java.util.stream.Stream

class ResultSet<T>(private val data: List<T>) : MutableIterable<T> {
    fun first(): T? = data.firstOrNull()
    fun firstOrNull(): T? = data.firstOrNull()
    fun size(): Int = data.size
    fun isEmpty(): Boolean = data.isEmpty()
    fun stream(): Stream<T> = (data as java.util.Collection<T>).stream()
    fun toList(): List<T> = data
    override fun iterator(): MutableIterator<T> = data.toMutableList().iterator()
}

