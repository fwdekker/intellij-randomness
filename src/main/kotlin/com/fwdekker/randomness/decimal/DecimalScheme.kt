package com.fwdekker.randomness.decimal

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.Icons
import com.fwdekker.randomness.Scheme
import com.fwdekker.randomness.TypeIcon
import com.fwdekker.randomness.affix.AffixDecorator
import com.fwdekker.randomness.array.ArrayDecorator
import com.fwdekker.randomness.ui.ValidatorDsl.Companion.validators
import com.intellij.ui.JBColor
import com.intellij.util.xmlb.annotations.OptionTag
import java.awt.Color
import java.text.DecimalFormat
import kotlin.math.nextUp


/**
 * Contains settings for generating random decimals.
 *
 * @property minValue The minimum value to be generated, inclusive.
 * @property maxValue The maximum value to be generated, inclusive.
 * @property decimalCount The number of decimals to display.
 * @property showTrailingZeroes Whether to include trailing zeroes in the decimals.
 * @property decimalSeparator The character that should separate decimals.
 * @property groupingSeparatorEnabled `true` if and only if the [groupingSeparator] should be used to separate groups.
 * @property groupingSeparator The character that should separate groups if [groupingSeparatorEnabled] is `true`.
 * @property affixDecorator The affixation to apply to the generated values.
 * @property arrayDecorator Settings that determine whether the output should be an array of values.
 */
data class DecimalScheme(
    var minValue: Double = DEFAULT_MIN_VALUE,
    var maxValue: Double = DEFAULT_MAX_VALUE,
    var decimalCount: Int = DEFAULT_DECIMAL_COUNT,
    var showTrailingZeroes: Boolean = DEFAULT_SHOW_TRAILING_ZEROES,
    var decimalSeparator: String = DEFAULT_DECIMAL_SEPARATOR,
    var groupingSeparatorEnabled: Boolean = DEFAULT_GROUPING_SEPARATOR_ENABLED,
    var groupingSeparator: String = DEFAULT_GROUPING_SEPARATOR,
    @OptionTag val affixDecorator: AffixDecorator = DEFAULT_AFFIX_DECORATOR,
    @OptionTag val arrayDecorator: ArrayDecorator = DEFAULT_ARRAY_DECORATOR,
) : Scheme() {
    override val name = Bundle("decimal.title")
    override val typeIcon get() = BASE_ICON
    override val decorators get() = listOf(affixDecorator, arrayDecorator)
    override val validators = validators {
        of(::maxValue)
            .check({ it >= minValue }, { Bundle("decimal.error.min_value_above_max") })
            .check(
                { it - minValue <= MAX_VALUE_DIFFERENCE },
                { Bundle("decimal.error.value_range", MAX_VALUE_DIFFERENCE) }
            )
        of(::decimalCount)
            .check({ it >= MIN_DECIMAL_COUNT }, { Bundle("decimal.error.decimal_count_too_low", MIN_DECIMAL_COUNT) })
        of(::decimalSeparator).check({ it.length == 1 }, { Bundle("decimal.error.decimal_separator_length") })
        of(::groupingSeparator).check({ it.length == 1 }, { Bundle("decimal.error.grouping_separator_length") })
        include(::affixDecorator)
        include(::arrayDecorator)
    }


    /**
     * Returns [count] random formatted decimals in the range from [minValue] until [maxValue], inclusive.
     */
    override fun generateUndecoratedStrings(count: Int) =
        List(count) { doubleToString(random.nextDouble(minValue, maxValue.nextUp())) }

    /**
     * Returns a nicely formatted representation of [decimal].
     */
    private fun doubleToString(decimal: Double): String {
        val format = DecimalFormat()

        if (showTrailingZeroes) format.minimumFractionDigits = decimalCount
        format.isGroupingUsed = groupingSeparatorEnabled
        format.maximumFractionDigits = decimalCount
        format.decimalFormatSymbols =
            format.decimalFormatSymbols.also {
                it.groupingSeparator = groupingSeparator.getOrElse(0) { DEFAULT_GROUPING_SEPARATOR[0] }
                it.decimalSeparator = decimalSeparator[0]
            }

        return format.format(decimal)
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
         * The base icon for decimals.
         */
        val BASE_ICON
            get() = TypeIcon(Icons.SCHEME, "4.2", listOf(JBColor(Color(98, 181, 67, 154), Color(98, 181, 67, 154))))

        /**
         * The maximum valid difference between the [minValue] and [maxValue] fields.
         */
        const val MAX_VALUE_DIFFERENCE = 1E53

        /**
         * The default value of the [minValue] field.
         */
        const val DEFAULT_MIN_VALUE = 0.0

        /**
         * The default value of the [maxValue] field.
         */
        const val DEFAULT_MAX_VALUE = 1_000.0

        /**
         * The minimum valid value for the [decimalCount] field.
         */
        const val MIN_DECIMAL_COUNT = 0

        /**
         * The default value of the [decimalCount] field.
         */
        const val DEFAULT_DECIMAL_COUNT = 2

        /**
         * The default value of the [showTrailingZeroes] field.
         */
        const val DEFAULT_SHOW_TRAILING_ZEROES = false

        /**
         * The preset values for the [decimalSeparator] field.
         */
        val PRESET_DECIMAL_SEPARATORS = listOf(",", ".")

        /**
         * The default value of the [decimalSeparator] field.
         */
        const val DEFAULT_DECIMAL_SEPARATOR = "."

        /**
         * The default value of the [groupingSeparatorEnabled] field.
         */
        const val DEFAULT_GROUPING_SEPARATOR_ENABLED = false

        /**
         * The preset values for the [groupingSeparator] field.
         */
        val PRESET_GROUPING_SEPARATORS = listOf(".", ",", "_")

        /**
         * The default value of the [groupingSeparator] field.
         */
        const val DEFAULT_GROUPING_SEPARATOR = ","

        /**
         * The preset values for the [affixDecorator] descriptor.
         */
        val PRESET_AFFIX_DECORATOR_DESCRIPTORS = listOf("@f", "@d")

        /**
         * The default value of the [affixDecorator] field.
         */
        val DEFAULT_AFFIX_DECORATOR get() = AffixDecorator(enabled = false, descriptor = "f@")

        /**
         * The default value of the [arrayDecorator] field.
         */
        val DEFAULT_ARRAY_DECORATOR get() = ArrayDecorator()
    }
}
