package com.fwdekker.randomness.integer

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.CapitalizationMode
import com.fwdekker.randomness.Icons
import com.fwdekker.randomness.Scheme
import com.fwdekker.randomness.TypeIcon
import com.fwdekker.randomness.affix.AffixDecorator
import com.fwdekker.randomness.array.ArrayDecorator
import com.fwdekker.randomness.fixedlength.FixedLengthDecorator
import com.fwdekker.randomness.ui.ValidatorDsl.Companion.validators
import com.intellij.ui.JBColor
import com.intellij.util.xmlb.annotations.OptionTag
import java.awt.Color
import java.text.DecimalFormat


/**
 * Contains settings for generating random integers.
 *
 * @property minValue The minimum value to be generated, inclusive.
 * @property maxValue The maximum value to be generated, inclusive.
 * @property base The base the generated value should be displayed in.
 * @property isUppercase `true` if and only if all letters are uppercase.
 * @property groupingSeparatorEnabled `true` if and only if the [groupingSeparator] should be applied.
 * @property groupingSeparator The character that should separate groups if [groupingSeparatorEnabled] is `true`.
 * @property fixedLengthDecorator Settings that determine whether the output should be fixed to a specific length.
 * @property affixDecorator The affixation to apply to the generated values.
 * @property arrayDecorator Settings that determine whether the output should be an array of values.
 */
data class IntegerScheme(
    var minValue: Long = DEFAULT_MIN_VALUE,
    var maxValue: Long = DEFAULT_MAX_VALUE,
    var base: Int = DEFAULT_BASE,
    var isUppercase: Boolean = DEFAULT_IS_UPPERCASE,
    var groupingSeparatorEnabled: Boolean = DEFAULT_GROUPING_SEPARATOR_ENABLED,
    var groupingSeparator: String = DEFAULT_GROUPING_SEPARATOR,
    @OptionTag val fixedLengthDecorator: FixedLengthDecorator = DEFAULT_FIXED_LENGTH_DECORATOR,
    @OptionTag val affixDecorator: AffixDecorator = DEFAULT_AFFIX_DECORATOR,
    @OptionTag val arrayDecorator: ArrayDecorator = DEFAULT_ARRAY_DECORATOR,
) : Scheme() {
    override val name = Bundle("integer.title")
    override val typeIcon get() = BASE_ICON
    override val decorators get() = listOf(fixedLengthDecorator, affixDecorator, arrayDecorator)
    override val validators = validators {
        of(::maxValue).check({ it >= minValue }, { Bundle("integer.error.min_value_above_max") })
        of(::base).check({ it in MIN_BASE..MAX_BASE }, { Bundle("integer.error.base_range", "$MIN_BASE..$MAX_BASE") })
        of(::groupingSeparator).check({ it.length == 1 }, { Bundle("integer.error.grouping_separator_length") })
        include(::fixedLengthDecorator)
        include(::affixDecorator)
        include(::arrayDecorator)
    }


    /**
     * Returns [count] random formatted integers from [minValue] (inclusive) to [maxValue] (inclusive).
     */
    override fun generateUndecoratedStrings(count: Int) =
        List(count) { longToString(randomLong(minValue, maxValue)) }

    /**
     * Returns a random long in the range from [from] (inclusive) to [until] (inclusive) without causing overflow.
     */
    private fun randomLong(from: Long, until: Long) =
        if (from == Long.MIN_VALUE && until == Long.MAX_VALUE) random.nextLong()
        else if (until == Long.MAX_VALUE) random.nextLong(from - 1, until) + 1
        else random.nextLong(from, until + 1)

    /**
     * Returns a nicely formatted representation of [value].
     */
    private fun longToString(value: Long): String {
        if (base != DECIMAL_BASE) {
            val capitalization = if (isUppercase) CapitalizationMode.UPPER else CapitalizationMode.LOWER
            return capitalization.transform(value.toString(base), random)
        }

        val format = DecimalFormat()
        format.isGroupingUsed = groupingSeparatorEnabled
        format.minimumFractionDigits = 0
        format.maximumFractionDigits = 0
        format.decimalFormatSymbols =
            format.decimalFormatSymbols.also {
                it.groupingSeparator = groupingSeparator.getOrElse(0) { DEFAULT_GROUPING_SEPARATOR[0] }
            }

        return format.format(value)
    }


    override fun deepCopy(retainUuid: Boolean) =
        copy(
            fixedLengthDecorator = fixedLengthDecorator.deepCopy(retainUuid),
            affixDecorator = affixDecorator.deepCopy(retainUuid),
            arrayDecorator = arrayDecorator.deepCopy(retainUuid),
        ).deepCopyTransient(retainUuid)


    /**
     * Holds constants.
     */
    companion object {
        /**
         * The base icon for integers.
         */
        val BASE_ICON
            get() = TypeIcon(Icons.SCHEME, "123", listOf(JBColor(Color(64, 182, 224, 154), Color(64, 182, 224, 154))))

        /**
         * The default value of the [minValue] field.
         */
        const val DEFAULT_MIN_VALUE = 0L

        /**
         * The default value of the [maxValue] field.
         */
        const val DEFAULT_MAX_VALUE = 1000L

        /**
         * The minimum value of the [base] field.
         */
        const val MIN_BASE = Character.MIN_RADIX

        /**
         * The maximum value of the [base] field.
         */
        const val MAX_BASE = Character.MAX_RADIX

        /**
         * The definition of decimal base.
         */
        const val DECIMAL_BASE = 10

        /**
         * The default value of the [base] field.
         */
        const val DEFAULT_BASE = DECIMAL_BASE

        /**
         * The default value of the [isUppercase] field.
         */
        const val DEFAULT_IS_UPPERCASE = false

        /**
         * The default value of the [groupingSeparatorEnabled] field.
         */
        const val DEFAULT_GROUPING_SEPARATOR_ENABLED = false

        /**
         * The preset values for the [groupingSeparator] descriptor.
         */
        val PRESET_GROUPING_SEPARATORS = listOf(".", ",", "_")

        /**
         * The default value of the [groupingSeparator] field.
         */
        const val DEFAULT_GROUPING_SEPARATOR = ","

        /**
         * The preset values for the [affixDecorator] descriptor.
         */
        val PRESET_AFFIX_DECORATOR_DESCRIPTORS = listOf("@b", "$@", "0x@")

        /**
         * The default value of the [fixedLengthDecorator] field.
         */
        val DEFAULT_FIXED_LENGTH_DECORATOR get() = FixedLengthDecorator()

        /**
         * The default value of the [affixDecorator] field.
         */
        val DEFAULT_AFFIX_DECORATOR get() = AffixDecorator(enabled = false, descriptor = "0x@")

        /**
         * The default value of the [arrayDecorator] field.
         */
        val DEFAULT_ARRAY_DECORATOR get() = ArrayDecorator()
    }
}
