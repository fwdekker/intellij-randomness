package com.fwdekker.randomness.datetime

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.Icons
import com.fwdekker.randomness.Scheme
import com.fwdekker.randomness.Timestamp
import com.fwdekker.randomness.TimestampConverter
import com.fwdekker.randomness.TypeIcon
import com.fwdekker.randomness.array.ArrayDecorator
import com.fwdekker.randomness.nextTimestampInclusive
import com.fwdekker.randomness.ui.ValidatorDsl.Companion.validators
import com.intellij.ui.JBColor
import com.intellij.util.xmlb.annotations.OptionTag
import java.awt.Color
import java.time.format.DateTimeFormatter


/**
 * Contains settings for generating random date-times.
 *
 * @property minDateTime The minimum date-time to be generated, inclusive.
 * @property maxDateTime The maximum date-time to be generated, inclusive.
 * @property pattern The pattern in which the generated date-time is formatted.
 * @property arrayDecorator Settings that determine whether the output should be an array of values.
 */
data class DateTimeScheme(
    @OptionTag(converter = TimestampConverter::class) var minDateTime: Timestamp = DEFAULT_MIN_DATE_TIME,
    @OptionTag(converter = TimestampConverter::class) var maxDateTime: Timestamp = DEFAULT_MAX_DATE_TIME,
    var pattern: String = DEFAULT_PATTERN,
    @OptionTag val arrayDecorator: ArrayDecorator = ArrayDecorator(),
) : Scheme() {
    override val name = Bundle("datetime.title")
    override val typeIcon get() = BASE_ICON
    override val decorators get() = listOf(arrayDecorator)
    override val validators = validators {
        include(::minDateTime)
        include(::maxDateTime)
        of(::maxDateTime).check({ !it.isBefore(minDateTime) }, { Bundle("datetime.error.min_datetime_above_max") })
        of(::pattern).checkNoException { DateTimeFormatter.ofPattern(it) }
    }


    override fun generateUndecoratedStrings(count: Int): List<String> {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return List(count) { random.nextTimestampInclusive(minDateTime, maxDateTime).format(formatter) }
    }


    override fun deepCopy(retainUuid: Boolean) =
        copy(arrayDecorator = arrayDecorator.deepCopy(retainUuid)).deepCopyTransient(retainUuid)


    /**
     * Holds constants.
     */
    companion object {
        /**
         * The base icon for date-times.
         */
        val BASE_ICON
            get() = TypeIcon(Icons.SCHEME, "2:3", listOf(JBColor(Color(249, 139, 158, 154), Color(249, 139, 158, 154))))

        /**
         * The default value of the [minDateTime] field.
         */
        val DEFAULT_MIN_DATE_TIME: Timestamp = Timestamp("0001-01-01 00:00:00.000")

        /**
         * The default value of the [maxDateTime] field.
         */
        val DEFAULT_MAX_DATE_TIME: Timestamp = Timestamp("9999-12-31 23:59:59.999")

        /**
         * The default value of the [pattern] field.
         */
        const val DEFAULT_PATTERN: String = "yyyy-MM-dd HH:mm:ss.SSS"
    }
}
