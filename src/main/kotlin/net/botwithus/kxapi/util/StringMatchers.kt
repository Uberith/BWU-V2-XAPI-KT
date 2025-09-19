package net.botwithus.kxapi.util

import java.util.function.BiFunction

/** Utility comparators that keep query helpers concise. */
object StringMatchers {
    val equalsIgnoreCase: BiFunction<String, CharSequence, Boolean> =
        BiFunction { expected, actual -> actual?.toString()?.equals(expected, ignoreCase = true) ?: false }

    val contains: BiFunction<String, CharSequence, Boolean> =
        BiFunction { fragment, actual -> actual?.toString()?.contains(fragment) ?: false }

    val containsIgnoreCase: BiFunction<String, CharSequence, Boolean> =
        BiFunction { fragment, actual -> actual?.toString()?.contains(fragment, ignoreCase = true) ?: false }
}