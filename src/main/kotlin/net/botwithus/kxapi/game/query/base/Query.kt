package net.botwithus.kxapi.game.query.base

import net.botwithus.kxapi.game.query.result.ResultSet


interface Query<T> : MutableIterable<T> {
    fun results(): ResultSet<T>
    fun test(t: T): Boolean
}

