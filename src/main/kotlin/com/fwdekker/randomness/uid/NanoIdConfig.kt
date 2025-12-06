package com.fwdekker.randomness.uid

import io.viascom.nanoid.NanoId
import kotlin.random.Random


/**
 * Configuration for generating Nano IDs.
 *
 * @property size The length of the generated Nano ID.
 * @property alphabet The alphabet to use when generating the Nano ID.
 */
data class NanoIdConfig(
    var size: Int = DEFAULT_SIZE,
    var alphabet: String = DEFAULT_ALPHABET,
) {
    /**
     * Generates [count] random Nano IDs using the given [random] instance.
     *
     * Note: The [random] parameter is currently unused as the NanoId library uses its own random source,
     * but it's included for API consistency with other config classes.
     */
    @Suppress("UNUSED_PARAMETER") // random parameter kept for API consistency
    fun generate(count: Int, random: Random): List<String> =
        List(count) { NanoId.generate(size, alphabet) }


    /**
     * Creates a deep copy of this configuration.
     */
    fun deepCopy() = copy()


    companion object {
        /**
         * The minimum allowed value of [size].
         */
        const val MIN_SIZE = 1

        /**
         * The default value of [size].
         */
        const val DEFAULT_SIZE = 21

        /**
         * The default value of [alphabet].
         */
        const val DEFAULT_ALPHABET: String = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

        /**
         * The preset values for affix decorators.
         */
        val PRESET_AFFIX_DECORATOR_DESCRIPTORS = listOf("'", "\"", "`")
    }
}
