package com.fwdekker.randomness

import com.fwdekker.randomness.Timestamp.Companion.FORMAT
import com.fwdekker.randomness.Timestamp.Companion.NOW
import com.fwdekker.randomness.ui.Validator
import com.fwdekker.randomness.ui.ValidatorDsl.Companion.validators
import com.github.sisyphsu.dateparser.DateParserUtils
import com.intellij.util.xmlb.Converter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.random.Random


/**
 * A textual representation of a moment in time, with additional support handling invalid user inputs and for the
 * special timestamp [NOW]. Allowing invalid user inputs to be stored in a [Timestamp] ensures that
 * [com.fwdekker.randomness.ui.JDateTimeField] does not reset the input field after an invalid input has been written.
 *
 * A [Timestamp] is similar in function to [LocalDateTime]: Both represent the notation of time rather than the moment
 * in time itself, and neither supports timezones. The most important differences with [LocalDateTime] are that the
 * [Timestamp]'s [value] is not guaranteed to be valid, and that the [Timestamp]'s value may be the special value [NOW].
 * Call [doValidate] to determine if (and why) the [value] is (in)valid.
 *
 * The canonical format for the [value] is defined by [FORMAT]. However, [value]s are interpreted quite liberally, so a
 * [value] that deviates from [FORMAT] may still be valid. For example, a [value] of `5819` is equivalent to a [value]
 * of `5819-01-01 00:00:00.000`. This liberal interpretation is performed by [DateParserUtils]. The constructor of
 * [Timestamp] proactively reformats the [value] into the [FORMAT]. Therefore, a [Timestamp] contains a valid [value] if
 * and only if [value] matches the [FORMAT] after the constructor completes. The only exception is for the
 * case-sensitive special [value] [NOW], which always refers to the current moment in time, is never re-formatted, and
 * is always valid.
 *
 * To represent a [Timestamp] which is guaranteed to be valid, use [LocalDateTime] instead.
 *
 * Note that this class is not a data class. This is because the [value] field must be re-formatted in the constructor,
 * but the field must also remain immutable. This requirement cannot be fulfilled with a data class.
 */
class Timestamp(value: String = "1970-01-01 00:00:00.000") : State() {
    /**
     * The textual representation of the timestamp.
     */
    val value: String

    /**
     * The cached value of [epochMilli], or `null` if [value] is invalid or if [value] is [NOW].
     */
    private val _epochMilli: Long?

    /**
     * The epoch millisecond representation of [value], or `null` if [value] is invalid.
     */
    val epochMilli: Long?
        get() = if (value == NOW) Instant.now().toEpochMilli() else this._epochMilli

    override val validators: List<Validator<*>> // Gets set in `init`


    init {
        var value = value
        var epochMilli: Long? = null

        if (value.isNotBlank() && value != NOW) {
            try {
                val dateTime = DateParserUtils.parseDateTime(value)
                value = dateTime.format(FORMATTER)
                epochMilli = dateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
            } catch (_: DateTimeParseException) {
                // Swallow
            }
        }

        this.value = value
        this._epochMilli = epochMilli
        this.validators = validators {
            // [epochMilli] is still `null` only if [value] is invalid
            of(this@Timestamp::epochMilli).check({ it != null }, { Bundle("timestamp.error.parse") })
        }
    }


    /**
     * Returns `true` if `this` occurs before [that], and returns `false` otherwise.
     *
     * If the [value] of either `this` or [that] is not valid, then this method returns `false`.
     *
     * Standard assumptions for [Comparable] are not guaranteed to hold. For example, it is entirely possible that
     * `a == b`, `a.isBefore(b)`, `b.isBefore(a)` are all `false` at the same time; for example if `a` and `b` have
     * different invalid [value]s. For this reason, [Timestamp] does not implement [Comparable]. However, if `this` and
     * [that] both have valid [value]s, then these strange situations do not occur.
     */
    fun isBefore(that: Timestamp): Boolean {
        if (this.value == NOW && that.value == NOW) return false

        val thisEpoch = this.epochMilli ?: Long.MAX_VALUE
        val thatEpoch = that.epochMilli ?: Long.MIN_VALUE

        return thisEpoch < thatEpoch
    }


    /**
     * Returns `true` if and only if `this` and [other] refer to the exact same moment in time.
     */
    override fun equals(other: Any?): Boolean = other is Timestamp && this.value == other.value

    /**
     * Returns the hash code of this timestamp.
     */
    override fun hashCode(): Int = value.hashCode()

    /**
     * Returns the string representation of this timestamp, exactly like a data class would.
     */
    override fun toString(): String = "Timestamp(value=$value)"

    override fun deepCopy(retainUuid: Boolean): Timestamp = Timestamp(value).deepCopyTransient(retainUuid)


    /**
     * Holds constants.
     */
    companion object {
        /**
         * The canonical format by which [Timestamp]s are represented.
         */
        const val FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"

        /**
         * Formatter for the [FORMAT].
         */
        val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(FORMAT)

        /**
         * The special [Timestamp.value] that always refers to the current moment in time.
         */
        const val NOW = "NOW"


        /**
         * Returns a [Timestamp] that is set at the given [epochMilli].
         */
        fun fromEpochMilli(epochMilli: Long): Timestamp =
            Timestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneOffset.UTC).format(FORMATTER))
    }
}

/**
 * Serializes [Timestamp]s to and from [String]s.
 */
class TimestampConverter : Converter<Timestamp>() {
    /**
     * Serializes the [Timestamp] to a string.
     *
     * Works even if the [value] is invalid.
     */
    override fun toString(value: Timestamp): String = value.value

    /**
     * Deserializes a [Timestamp] from the given string.
     *
     * Works even if the value is not valid for a [Timestamp].
     */
    override fun fromString(value: String): Timestamp = Timestamp(value)
}


/**
 * Returns a random [LocalDateTime] between [min] (inclusive) and [max] (inclusive).
 *
 * Despite the method's name, this method returns a [LocalDateTime] instead of a [Timestamp]. As per the documentation
 * of [Timestamp], the class [LocalDateTime] can be seen as a [Timestamp] of which the [Timestamp.value] is guaranteed
 * to be valid.
 */
fun Random.nextTimestampInclusive(min: Timestamp, max: Timestamp): LocalDateTime {
    val epoch = nextLong(min.epochMilli!!, max.epochMilli!! + 1)
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC)
}
