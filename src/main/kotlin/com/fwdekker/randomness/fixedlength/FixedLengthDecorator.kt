package com.fwdekker.randomness.fixedlength

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.DecoratorScheme
import com.fwdekker.randomness.ui.ValidatorDsl.Companion.validators


/**
 * Forces generated strings to be exactly [length] characters.
 *
 * @property enabled Whether to apply this decorator.
 * @property length The enforced length.
 * @property filler The character to pad strings that are too short with.
 */
data class FixedLengthDecorator(
    var enabled: Boolean = DEFAULT_ENABLED,
    var length: Int = DEFAULT_LENGTH,
    var filler: String = DEFAULT_FILLER,
) : DecoratorScheme() {
    override val name = Bundle("fixed_length.title")
    override val decorators = emptyList<DecoratorScheme>()
    override val isEnabled get() = enabled
    override val validators = validators {
        case({ enabled }) {
            of(::length).check({ it >= MIN_LENGTH }, { Bundle("fixed_length.error.length_too_low", MIN_LENGTH) })
            of(::filler).check({ it.length == 1 }, { Bundle("fixed_length.error.filler_length") })
        }
    }


    override fun generateUndecoratedStrings(count: Int): List<String> =
        generator(count).map { it.take(length).padStart(length, filler[0]) }

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
         * The minimum valid value of the [length] field.
         */
        const val MIN_LENGTH = 1

        /**
         * The default value of the [length] field.
         */
        const val DEFAULT_LENGTH = 3

        /**
         * The default value of the [filler] field.
         */
        const val DEFAULT_FILLER = "0"
    }
}
