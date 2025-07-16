package com.fwdekker.randomness.array

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.DecoratorScheme
import com.fwdekker.randomness.OverlayIcon
import com.fwdekker.randomness.affix.AffixDecorator
import com.intellij.util.xmlb.annotations.OptionTag


/**
 * The user-configurable collection of schemes applicable to generating arrays.
 *
 * @property enabled `true` if and only if arrays should be generated instead of singular values.
 * @property minCount The minimum number of elements to generate, inclusive.
 * @property maxCount The maximum number of elements to generate, inclusive.
 * @property separatorEnabled Whether to separate elements using [separator].
 * @property separator The string to place between generated elements.
 * @property showIndices `true` if and only if array indices should be shown with each element.
 * @property indicesFormat The format string for displaying array indices with elements.
 * @property useTupleIndices `true` if and only if indices should be formatted as tuples for multi-dimensional arrays.
 * @property affixDecorator The affixation to apply to the generated values.
 */
data class ArrayDecorator(
    var enabled: Boolean = DEFAULT_ENABLED,
    var minCount: Int = DEFAULT_MIN_COUNT,
    var maxCount: Int = DEFAULT_MAX_COUNT,
    var separatorEnabled: Boolean = DEFAULT_SEPARATOR_ENABLED,
    var separator: String = DEFAULT_SEPARATOR,
    var showIndices: Boolean = DEFAULT_SHOW_INDICES,
    var indicesFormat: String = DEFAULT_INDICES_FORMAT,
    var useTupleIndices: Boolean = DEFAULT_USE_TUPLE_INDICES,
    @OptionTag val affixDecorator: AffixDecorator = DEFAULT_AFFIX_DECORATOR,
) : DecoratorScheme() {
    override val name = Bundle("array.title")
    override val overlayIcon get() = if (enabled) OverlayIcon.ARRAY else null
    override val decorators = listOf(affixDecorator)
    override val isEnabled get() = enabled


    override fun generateUndecoratedStrings(count: Int): List<String> {
        val partsPerString = List(count) { random.nextInt(minCount, maxCount + 1) }
        val parts = generator(partsPerString.sum())

        return partsPerString
            .foldIndexed(
                Pair(
                    parts,
                    emptyList<String>()
                )
            ) { arrayIndex, (remainingParts, createdStrings), nextPartCount ->
                val nextParts = remainingParts.take(nextPartCount)
                val formattedParts = if (showIndices) {
                    nextParts.mapIndexed { elementIndex, value ->
                        val indexStr = if (useTupleIndices) {
                            // Format as tuple for multi-dimensional arrays
                            "($arrayIndex,$elementIndex)"
                        } else {
                            elementIndex.toString()
                        }
                        indicesFormat.replace("{index}", indexStr).replace("{value}", value)
                    }
                } else {
                    nextParts
                }

                val nextString =
                    formattedParts.joinToString(if (separatorEnabled) separator.replace("\\n", "\n") else "")

                Pair(remainingParts.drop(nextPartCount), createdStrings + nextString)
            }
            .second
    }


    override fun doValidate() =
        if (minCount < MIN_MIN_COUNT) Bundle("array.error.min_count_too_low", MIN_MIN_COUNT)
        else if (maxCount < minCount) Bundle("array.error.min_count_above_max")
        else affixDecorator.doValidate()

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
         * The default value of the [showIndices] field.
         */
        const val DEFAULT_SHOW_INDICES = false

        /**
         * The preset values for the [indicesFormat] field.
         */
        val PRESET_INDICES_FORMATS = listOf("{index}: {value}", "{index}={value}", "\"{index}\": {value}")

        /**
         * The default value of the [indicesFormat] field.
         */
        const val DEFAULT_INDICES_FORMAT = "{index}: {value}"

        /**
         * The default value of the [useTupleIndices] field.
         */
        const val DEFAULT_USE_TUPLE_INDICES = false

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
