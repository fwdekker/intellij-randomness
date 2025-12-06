package com.fwdekker.randomness.uid

import com.fasterxml.uuid.EthernetAddress
import com.fasterxml.uuid.NoArgGenerator
import com.fasterxml.uuid.UUIDClock
import com.fasterxml.uuid.UUIDTimer
import com.fasterxml.uuid.UUIDType
import com.fasterxml.uuid.impl.RandomBasedGenerator
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator
import com.fasterxml.uuid.impl.TimeBasedGenerator
import com.fasterxml.uuid.impl.TimeBasedReorderedGenerator
import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.CapitalizationMode
import com.fwdekker.randomness.Timestamp
import com.fwdekker.randomness.TimestampConverter
import com.intellij.util.xmlb.annotations.OptionTag
import java.util.UUID
import kotlin.random.Random
import kotlin.random.asJavaRandom


/**
 * Configuration for generating UUIDs.
 *
 * @property version The version of UUIDs to generate.
 * @property minDateTime The minimum date-time to use, applicable only for time-based UUIDs.
 * @property maxDateTime The maximum date-time to use, applicable only for time-based UUIDs.
 * @property isUppercase `true` if and only if all letters are uppercase.
 * @property addDashes `true` if and only if the UUID should have dashes in it.
 */
data class UuidConfig(
    var version: Int = DEFAULT_VERSION,
    @OptionTag(converter = TimestampConverter::class) var minDateTime: Timestamp = DEFAULT_MIN_DATE_TIME,
    @OptionTag(converter = TimestampConverter::class) var maxDateTime: Timestamp = DEFAULT_MAX_DATE_TIME,
    var isUppercase: Boolean = DEFAULT_IS_UPPERCASE,
    var addDashes: Boolean = DEFAULT_ADD_DASHES,
) {
    /**
     * Generates [count] random UUIDs using the given [random] instance.
     */
    @Suppress("detekt:MagicNumber") // UUID versions are well-defined
    fun generate(count: Int, random: Random): List<String> {
        val generator = when (version) {
            1 -> TimeBasedGenerator(random.nextAddress(), random.uuidTimer(minDateTime, maxDateTime))
            4 -> RandomBasedGenerator(random.asJavaRandom())
            6 -> TimeBasedReorderedGenerator(random.nextAddress(), random.uuidTimer(minDateTime, maxDateTime))
            7 -> TimeBasedEpochGenerator(random.asJavaRandom(), random.uuidClock(minDateTime, maxDateTime))
            8 -> FreeFormGenerator(random)
            else -> error(Bundle("uuid.error.unknown_version", version))
        }

        return List(count) { generator.generate().toString() }
            .map {
                val capitalization = if (isUppercase) CapitalizationMode.UPPER else CapitalizationMode.LOWER
                capitalization.transform(it, random)
            }
            .map {
                if (addDashes) it
                else it.replace("-", "")
            }
    }


    /**
     * Creates a deep copy of this configuration.
     */
    fun deepCopy() = copy()


    companion object {
        /**
         * The default value of the [version] field.
         */
        const val DEFAULT_VERSION = 4

        /**
         * The list of supported [version]s.
         */
        val SUPPORTED_VERSIONS = listOf(1, 4, 6, 7, 8)

        /**
         * The list of supported [version]s that use the [minDateTime] and [maxDateTime] fields.
         */
        val TIME_BASED_VERSIONS = listOf(1, 6, 7)

        /**
         * The default value of the [isUppercase] field.
         */
        const val DEFAULT_IS_UPPERCASE = false

        /**
         * The default value of the [addDashes] field.
         */
        const val DEFAULT_ADD_DASHES = true

        /**
         * The minimum valid value of [minDateTime].
         */
        val MIN_MIN_DATE_TIME: Timestamp = Timestamp("1970-01-01 00:00:00.000")

        /**
         * The default value of the [minDateTime] field.
         */
        val DEFAULT_MIN_DATE_TIME: Timestamp = MIN_MIN_DATE_TIME

        /**
         * The maximum valid value of [maxDateTime].
         */
        val MAX_MAX_DATE_TIME: Timestamp = Timestamp("5236-03-31 21:21:00.684")

        /**
         * The default value of the [maxDateTime] field.
         */
        val DEFAULT_MAX_DATE_TIME: Timestamp = MAX_MAX_DATE_TIME

        /**
         * The preset values for affix decorators.
         */
        val PRESET_AFFIX_DECORATOR_DESCRIPTORS = listOf("'", "\"", "`")
    }
}


/**
 * Constants about UUIDs.
 */
@Suppress("unused") // Useful to keep for future reference
object UuidMeta {
    /**
     * The epoch of UUIDv1 timestamps, expressed as a Unix millisecond epoch.
     */
    const val V1_TIMESTAMP_EPOCH: Long = -0xB1D069B5400L

    /**
     * The modulo under which UUIDv1 timestamps are stored.
     */
    const val V1_TIMESTAMP_MODULO: Long = 0x1000000000000000L

    /**
     * The epoch of UUIDv6 timestamps, expressed as a Unix millisecond epoch.
     */
    const val V6_TIMESTAMP_EPOCH: Long = V1_TIMESTAMP_EPOCH

    /**
     * The modulo under which UUIDv6 timestamps are stored.
     */
    const val V6_TIMESTAMP_MODULO: Long = V1_TIMESTAMP_MODULO

    /**
     * The epoch of UUIDv7 timestamps, expressed as a Unix millisecond epoch.
     */
    const val V7_TIMESTAMP_EPOCH: Long = 0L

    /**
     * The modulo under which UUIDv7 timestamps are stored.
     */
    const val V7_TIMESTAMP_MODULO: Long = 0x1000000000000L
}


/**
 * Returns a random [EthernetAddress].
 */
private fun Random.nextAddress() = EthernetAddress(nextLong())

/**
 * Similar to [Random.nextLong], but [min] and [max] are inclusive.
 */
private fun Random.nextLongInclusive(min: Long = Long.MIN_VALUE, max: Long = Long.MAX_VALUE): Long =
    if (max == Long.MAX_VALUE)
        if (min == Long.MIN_VALUE) nextLong()
        else nextLong(min - 1, max) + 1
    else
        nextLong(min, max + 1)

/**
 * Returns a [UUIDClock] that generates random times between [min] and [max] using this [Random] instance.
 */
private fun Random.uuidClock(min: Timestamp, max: Timestamp) =
    object : UUIDClock() {
        override fun currentTimeMillis() = nextLongInclusive(min.epochMilli!!, max.epochMilli!!)
    }

/**
 * Returns a [UUIDTimer] that generates random times between [min] and [max] using this [Random] instance.
 */
private fun Random.uuidTimer(min: Timestamp, max: Timestamp) =
    UUIDTimer(asJavaRandom(), null, uuidClock(min, max))


/**
 * Generates v8 UUIDs.
 *
 * Works by generating a v4 UUID and then replacing the version nibble.
 */
private class FreeFormGenerator(random: Random) : NoArgGenerator() {
    /**
     * Generates v4 UUIDs.
     */
    private val innerGenerator = RandomBasedGenerator(random.asJavaRandom())


    /**
     * Returns [UUIDType.FREE_FORM].
     */
    override fun getType(): UUIDType = UUIDType.FREE_FORM

    /**
     * Generates a v8 UUID.
     */
    override fun generate(): UUID =
        innerGenerator.generate().toString()
            .split('-')
            .toMutableList()
            .also { it[2] = "8${it[2].drop(1)}" }
            .joinToString(separator = "-")
            .let { UUID.fromString(it) }
}
