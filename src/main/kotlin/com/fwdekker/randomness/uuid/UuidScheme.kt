package com.fwdekker.randomness.uuid

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
import com.fwdekker.randomness.Icons
import com.fwdekker.randomness.Scheme
import com.fwdekker.randomness.Timestamp
import com.fwdekker.randomness.TypeIcon
import com.fwdekker.randomness.affix.AffixDecorator
import com.fwdekker.randomness.array.ArrayDecorator
import com.fwdekker.randomness.ui.ValidatorDsl.Companion.validators
import com.intellij.ui.JBColor
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.Transient
import java.awt.Color
import java.util.UUID
import kotlin.random.Random
import kotlin.random.asJavaRandom


/**
 * Contains settings for generating random UUIDs.
 *
 * @property version The version of UUIDs to generate.
 * @property minDateTime The minimum date-time to use, applicable only for time-based UUIDs.
 * @property maxDateTime The maximum date-time to use, applicable only for time-based UUIDs.
 * @property isUppercase `true` if and only if all letters are uppercase.
 * @property addDashes `true` if and only if the UUID should have dashes in it.
 * @property affixDecorator The affixation to apply to the generated values.
 * @property arrayDecorator Settings that determine whether the output should be an array of values.
 */
data class UuidScheme(
    var version: Int = DEFAULT_VERSION,
    @OptionTag var minDateTime: Timestamp = DEFAULT_MIN_DATE_TIME,
    @OptionTag var maxDateTime: Timestamp = DEFAULT_MAX_DATE_TIME,
    var isUppercase: Boolean = DEFAULT_IS_UPPERCASE,
    var addDashes: Boolean = DEFAULT_ADD_DASHES,
    @OptionTag val affixDecorator: AffixDecorator = DEFAULT_AFFIX_DECORATOR,
    @OptionTag val arrayDecorator: ArrayDecorator = DEFAULT_ARRAY_DECORATOR,
) : Scheme() {
    @get:Transient
    override val name = Bundle("uuid.title")
    override val typeIcon get() = BASE_ICON
    override val decorators get() = listOf(affixDecorator, arrayDecorator)
    override val validators = validators {
        of(::version).check({ it in SUPPORTED_VERSIONS }, { Bundle("uuid.error.unknown_version", it) })
        include(::affixDecorator)
        include(::arrayDecorator)
    }


    /**
     * Returns [count] random UUIDs.
     */
    @Suppress("detekt:MagicNumber") // UUID versions are well-defined
    override fun generateUndecoratedStrings(count: Int): List<String> {
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


    override fun deepCopy(retainUuid: Boolean) =
        copy(
            affixDecorator = affixDecorator.deepCopy(retainUuid),
            arrayDecorator = arrayDecorator.deepCopy(retainUuid),
        ).deepCopyTransient(retainUuid)


    /**
     * Holds constants.
     */
    companion object {
        /**
         * The base icon for UUIDs.
         */
        val BASE_ICON
            get() = TypeIcon(Icons.SCHEME, "id", listOf(JBColor(Color(185, 155, 248, 154), Color(185, 155, 248, 154))))

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
         * The default value of the [minDateTime] field.
         */
        val DEFAULT_MIN_DATE_TIME: Timestamp = Timestamp("0001-01-01 00:00:00.000")

        /**
         * The default value of the [maxDateTime] field.
         */
        val DEFAULT_MAX_DATE_TIME: Timestamp = Timestamp("9999-12-31 23:59:59.999")

        /**
         * The preset values for the [affixDecorator] field.
         */
        val PRESET_AFFIX_DECORATOR_DESCRIPTORS = listOf("'", "\"", "`")

        /**
         * The default value of the [affixDecorator] field.
         */
        val DEFAULT_AFFIX_DECORATOR get() = AffixDecorator(enabled = false, descriptor = "\"")

        /**
         * The default value of the [arrayDecorator] field.
         */
        val DEFAULT_ARRAY_DECORATOR get() = ArrayDecorator()
    }
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
 *
 * Both [min] and [max] are inclusive, and are assumed to be valid.
 */
private fun Random.uuidClock(min: Timestamp, max: Timestamp) =
    object : UUIDClock() {
        override fun currentTimeMillis() = nextLongInclusive(min.epochMilli!!, max.epochMilli!!)
    }

/**
 * Returns a [UUIDTimer] that generates random times between [min] and [max] using this [Random] instance.
 *
 * Both [min] and [max] are inclusive, and are assumed to be valid.
 */
private fun Random.uuidTimer(min: Timestamp, max: Timestamp) =
    UUIDTimer(asJavaRandom(), null, uuidClock(min, max))


/**
 * Generates v8 UUIDs.
 *
 * Works by generating a v4 UUID and then replacing the version nibble.
 *
 * TODO\[Workaround]: Remove class after https://github.com/cowtowncoder/java-uuid-generator/issues/47 has been fixed
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
