package com.fwdekker.randomness

import com.fwdekker.randomness.Timestamp.Companion.FORMAT
import com.fwdekker.randomness.ui.Validator
import com.fwdekker.randomness.ui.ValidatorDsl.Companion.validators
import com.github.sisyphsu.dateparser.DateParserUtils
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.random.Random


/**
 * A textual representation of a moment in time, with additional support for handling invalid user inputs.
 *
 * A [Timestamp] is similar in function to [LocalDateTime]: Both represent the notation of time rather than the moment
 * in time itself, and neither supports timezones. The most important difference with [LocalDateTime] is that the
 * [Timestamp]'s [value] is not guaranteed to be valid. Call [doValidate] to determine if (and why) the [value] is
 * (in)valid.
 *
 * The format for date-time timestamps is defined by [FORMAT]. However, [value]s are interpreted quite
 * liberally, so a [value] that deviates from [FORMAT] may still be valid. For example, a [value] of `5819` is
 * equivalent to a [value] of `5819-01-01 00:00:00.000`. This liberal interpretation is performed by [DateParserUtils].
 * The constructor of [Timestamp] proactively reformats the [value] into the [FORMAT]. Therefore, a
 * [Timestamp] contains a valid [value] if and only if [value] matches the [FORMAT] after the constructor
 * completes.
 *
 * To represent a [Timestamp] which is guaranteed to be valid, use [LocalDateTime] instead.
 *
 * @property value The textual representation of the timestamp.
 */
data class Timestamp(var value: String = "1970-01-01 00:00:00.000") : State() {
    /**
     * The epoch millisecond representation of [value], or `null` if [value] is invalid.
     */
    val epochMilli: Long?

    override val validators: List<Validator<*>>


    init {
        var value = value
        var epochMilli: Long? = null

        if (value.isBlank()) {
            epochMilli = null
        } else {
            try {
                val dateTime = DateParserUtils.parseDateTime(value)
                value = dateTime.format(FORMATTER)
                epochMilli = dateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
            } catch (_: DateTimeParseException) {
                // Swallow
            }
        }

        this.value = value
        this.epochMilli = epochMilli
        this.validators = validators {
            // Recall that a [value] is valid if [DateParserUtils] can interpret [value] as a date-time timestamp
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
        val thisEpoch = this.epochMilli ?: Long.MAX_VALUE
        val thatEpoch = that.epochMilli ?: Long.MIN_VALUE

        return thisEpoch < thatEpoch
    }


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
         * Returns a [Timestamp] that is set at the given [epochMilli].
         */
        fun fromEpochMilli(epochMilli: Long): Timestamp =
            Timestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneOffset.UTC).format(FORMATTER))
    }
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
