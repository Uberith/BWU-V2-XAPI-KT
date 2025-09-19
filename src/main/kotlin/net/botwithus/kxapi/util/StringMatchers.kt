package net.botwithus.kxapi.util

/** Utility comparators that keep query helpers concise. */
object StringMatchers {
    val equalsIgnoreCase: (String, CharSequence) -> Boolean =
        { expected, actual -> actual.toString().equals(expected, ignoreCase = true) }

    val contains: (String, CharSequence) -> Boolean =
        { fragment, actual -> actual.toString().contains(fragment) }

    val containsIgnoreCase: (String, CharSequence) -> Boolean =
        { fragment, actual -> actual.toString().contains(fragment, ignoreCase = true) }
}