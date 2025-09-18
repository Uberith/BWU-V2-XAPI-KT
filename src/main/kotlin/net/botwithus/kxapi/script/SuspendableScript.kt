package net.botwithus.kxapi.script

import net.botwithus.rs3.client.Client
import net.botwithus.rs3.entities.LocalPlayer
import net.botwithus.scripts.Script
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.suspendCoroutine

/**
 * Suspendable tick-driven script base.
 * Use onLoop() and wait()/waitUntil() for precise tick scheduling.
 */
abstract class SuspendableScript : Script() {

    private var continuation: Continuation<Unit>? = null
    private var isSuspended = false
    private var targetTick = 0

    abstract suspend fun onLoop()

    override fun run() {
        if (isSuspended && Client.getServerTick() >= targetTick) {
            isSuspended = false
            val current = continuation
            continuation = null
            current?.resumeWith(Result.success(Unit))
        } else if (continuation == null) {
            startCoroutine()
        }
    }

    suspend fun awaitTicks(ticks: Int) {
        suspendCoroutine<Unit> { cont ->
            continuation = cont
            isSuspended = true
            targetTick = Client.getServerTick() + ticks
        }
    }

    suspend fun awaitUntil(timeout: Int = 5, condition: () -> Boolean) {
        val timeoutTick = Client.getServerTick() + timeout
        while (!condition()) {
            if (Client.getServerTick() >= timeoutTick) return
            awaitTicks(1)
        }
    }

    suspend fun awaitIdle(timeout: Int = 10, includeMovement: Boolean = true): Boolean {
        val deadline = Client.getServerTick() + timeout
        while (Client.getServerTick() <= deadline) {
            val player = LocalPlayer.self()
            val animationIdle = (player?.animationId ?: -1) == -1
            val movementIdle = !includeMovement || player?.isMoving != true
            if (animationIdle && movementIdle) {
                return true
            }
            awaitTicks(1)
        }
        return false
    }

    // Kotlin-facing aliases preserving the original example names while avoiding JVM method name clashes
    @JvmName("waitTicksAlias")
    @Suppress("unused")
    suspend fun wait(ticks: Int) = awaitTicks(ticks)

    @JvmName("waitUntilAlias")
    @Suppress("unused")
    suspend fun waitUntil(timeout: Int = 5, condition: () -> Boolean) = awaitUntil(timeout, condition)

    private val coroutineBlock: suspend () -> Unit = {
        while (true) {
            onLoop()
            awaitTicks(1)
        }
    }

    private fun startCoroutine() {
        val c = coroutineBlock.createCoroutine(LoggingCompletion)
        c.resumeWith(Result.success(Unit))
    }

    private object LoggingCompletion : Continuation<Unit> {
        override val context: CoroutineContext = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {
            result.exceptionOrNull()?.let { println(it) }
        }
    }
}