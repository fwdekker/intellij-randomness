package com.fwdekker.randomness

import java.util.MissingResourceException
import java.util.ResourceBundle


/**
 * Simple accessor for working with internationalized strings.
 */
object Bundle {
    /**
     * The main bundle for Randomness.
     */
    private val RESOURCE_BUNDLE = ResourceBundle.getBundle("randomness")


    /**
     * Returns the string at [key].
     *
     * @throws MissingResourceException if no string with [key] can be found
     */
    @Throws(MissingResourceException::class)
    operator fun invoke(key: String): String = RESOURCE_BUNDLE.getString(key)

    /**
     * Returns the string at [key] formatted with [arguments].
     *
     * @throws MissingResourceException if no string with [key] can be found
     */
    @Throws(MissingResourceException::class)
    operator fun invoke(key: String, vararg arguments: Any?): String = this(key).format(*arguments)
}


/**
 * Returns `true` if [format] is a format string for `this` string, optionally after inserting [args] into [format].
 *
 * @throws java.util.MissingFormatArgumentException if [args] has fewer arguments than required for [format]
 */
fun String.matchesFormat(format: String, vararg args: String) =
    Regex("%[0-9]+\\$[Ssd]").findAll(format)
        .toList()
        .asReversed()
        .fold(format) { acc, match ->
            if (match.value.drop(1).dropLast(2).toInt() > args.size)
                acc.replaceRange(match.range, ".*")
            else
                acc
        }
        .format(*args)
        .let { Regex(it) }
        .matches(this)
