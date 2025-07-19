package com.fwdekker.randomness.array

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.DecoratorScheme
import com.fwdekker.randomness.OverlayIcon
import com.fwdekker.randomness.affix.AffixDecorator
import com.fwdekker.randomness.ui.ValidatorDsl.Companion.validators
import com.intellij.util.xmlb.annotations.OptionTag


/**
 * The user-configurable collection of schemes applicable to generating arrays.
 *
 * @property enabled `true` if and only if arrays should be generated instead of singular values.
 * @property minCount The minimum number of elements to generate, inclusive.
 * @property maxCount The maximum number of elements to generate, inclusive.
 * @property separatorEnabled Whether to separate elements using [separator].
 * @property separator The string to place between generated elements.
 * @property elementFormat The format according to which each element of the array should be formatted.
 * @property affixDecorator The affixation to apply to the generated values.
 */
data class ArrayDecorator(
    var enabled: Boolean = DEFAULT_ENABLED,
    var minCount: Int = DEFAULT_MIN_COUNT,
    var maxCount: Int = DEFAULT_MAX_COUNT,
    var separatorEnabled: Boolean = DEFAULT_SEPARATOR_ENABLED,
    var separator: String = DEFAULT_SEPARATOR,
    var elementFormat: String = DEFAULT_ELEMENT_FORMAT,
    @OptionTag val affixDecorator: AffixDecorator = DEFAULT_AFFIX_DECORATOR,
) : DecoratorScheme() {
    override val name = Bundle("array.title")
    override val overlayIcon get() = if (enabled) OverlayIcon.ARRAY else null
    override val decorators = listOf(affixDecorator)
    override val isEnabled get() = enabled
    override val validators = validators {
        of(::minCount).check({ it >= MIN_MIN_COUNT }, { Bundle("array.error.min_count_too_low", MIN_MIN_COUNT) })
        of(::maxCount).check({ it >= minCount }, { Bundle("array.error.min_count_above_max") })
        include(::affixDecorator)
    }


    override fun generateUndecoratedStrings(count: Int): List<String> {
        // Generates `count` arrays, with each array containing some number of elements.

        val elementsPerArray = List(count) { random.nextInt(minCount, maxCount + 1) }
        val elements = generator(elementsPerArray.sum())

        return elementsPerArray
            .foldIndexed(Pair(elements, emptyList<String>())) { aId, (remainingElements, createdArrays), elementCount ->
                val newArray = remainingElements
                    .take(elementCount)
                    .mapIndexed { eId, element ->
                        elementFormat
                            .replace("{aid}", aId.toString())
                            .replace("{eid}", eId.toString())
                            .replace("{val}", element) // Must be last, to avoid recursive replacement
                    }
                    .joinToString(separator = if (separatorEnabled) separator.replace("\\n", "\n") else "")

                Pair(remainingElements.drop(elementCount), createdArrays + newArray)
            }
            .second
    }


    override fun deepCopy(retainUuid: Boolean) = copy().deepCopyTransient(retainUuid)


    /**
     * Holds constants.
     */
    companion object {
        /**
         * The default value of the [enabled] field.
         */
        const val DEFAULT_ENABLED = false

        /**
         * The minimum valid value of the [minCount] field.
         */
        const val MIN_MIN_COUNT = 1

        /**
         * The default value of the [minCount] field.
         */
        const val DEFAULT_MIN_COUNT = 3

        /**
         * The default value of the [maxCount] field.
         */
        const val DEFAULT_MAX_COUNT = 3

        /**
         * The default value of the [separatorEnabled] field.
         */
        const val DEFAULT_SEPARATOR_ENABLED = true

        /**
         * The preset values for the [separator] field.
         */
        val PRESET_SEPARATORS = listOf(", ", "; ", "\\n")

        /**
         * The default value of the [separator] field.
         */
        const val DEFAULT_SEPARATOR = ", "

        /**
         * The preset values for the [format] field.
         */
        val PRESET_ELEMENT_FORMATS = listOf(
            "{val}",
            "{eid}: {val}",
            "{eid}={val}",
            "({aid}, {eid}): {val}",
            "({aid}, {eid})={val}",
        )

        /**
         * The default value of the [format] field.
         */
        const val DEFAULT_ELEMENT_FORMAT = "{val}"

        /**
         * The preset values for the [affixDecorator] descriptor.
         */
        val PRESET_AFFIX_DECORATOR_DESCRIPTORS = listOf("[@]", "{@}", "(@)")

        /**
         * The default value of the [affixDecorator] field.
         */
        val DEFAULT_AFFIX_DECORATOR get() = AffixDecorator(enabled = true, descriptor = "[@]")
    }
}
