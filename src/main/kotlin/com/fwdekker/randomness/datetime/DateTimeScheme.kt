package com.fwdekker.randomness.datetime

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.Icons
import com.fwdekker.randomness.Scheme
import com.fwdekker.randomness.Timestamp
import com.fwdekker.randomness.TypeIcon
import com.fwdekker.randomness.array.ArrayDecorator
import com.fwdekker.randomness.nextTimestampInclusive
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.JBColor
import com.intellij.util.xmlb.annotations.OptionTag
import java.awt.Color
import java.time.format.DateTimeFormatter
import javax.swing.JLabel


/**
 * Contains settings for generating random date-times.
 *
 * @property minDateTime The minimum date-time as a millisecond epoch to be generated, inclusive.
 * @property maxDateTime The maximum date-time as a millisecond epoch to be generated, inclusive.
 * @property pattern The pattern in which the generated date-time is formatted.
 * @property arrayDecorator Settings that determine whether the output should be an array of values.
 */
data class DateTimeScheme(
    @OptionTag var minDateTime: Timestamp = DEFAULT_MIN_DATE_TIME,
    @OptionTag var maxDateTime: Timestamp = DEFAULT_MAX_DATE_TIME,
    var pattern: String = DEFAULT_PATTERN,
    @OptionTag val arrayDecorator: ArrayDecorator = ArrayDecorator(),
) : Scheme() {
    override val name = Bundle("datetime.title")
    override val typeIcon get() = BASE_ICON
    override val decorators get() = listOf(arrayDecorator)


    override fun generateUndecoratedStrings(count: Int): List<String> {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return List(count) { random.nextTimestampInclusive(minDateTime, maxDateTime).format(formatter) }
    }


    override fun doValidate(): String? =
        minDateTime.doValidate()
            ?: maxDateTime.doValidate()
            ?: (if (maxDateTime.isBefore(minDateTime)) Bundle("datetime.error.min_datetime_above_max") else null)
            ?: pattern.doValidateDateTimePattern()

    override fun doValidate2(): ValidationInfo? =
        (minDateTime.doValidate()?.let { ValidationInfo(it, JLabel("minDateTime")) })
            ?: (maxDateTime.doValidate()?.let { ValidationInfo(it, JLabel("maxDateTime")) })
            ?: (if (maxDateTime.isBefore(minDateTime)) ValidationInfo(Bundle("datetime.error.min_datetime_above_max")) else null)
            ?: (pattern.doValidateDateTimePattern()?.let { ValidationInfo(it, JLabel("pattern")) })

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


/**
 * Returns `null` if this [String] is a valid date-time pattern, or a string describing why it is invalid otherwise.
 *
 * @see DateTimeFormatter
 */
private fun String.doValidateDateTimePattern(): String? =
    try {
        DateTimeFormatter.ofPattern(this)
        null
    } catch (exception: IllegalArgumentException) {
        exception.message
    }
