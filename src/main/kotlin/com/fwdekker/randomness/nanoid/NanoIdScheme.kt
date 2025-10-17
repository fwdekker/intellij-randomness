package com.fwdekker.randomness.nanoid

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.Icons
import com.fwdekker.randomness.Scheme
import com.fwdekker.randomness.TypeIcon
import com.fwdekker.randomness.affix.AffixDecorator
import com.fwdekker.randomness.array.ArrayDecorator
import com.fwdekker.randomness.ui.ValidatorDsl.Companion.validators
import com.intellij.ui.JBColor
import com.intellij.util.xmlb.annotations.OptionTag
import io.viascom.nanoid.NanoId
import java.awt.Color

/**
 * Contains settings for generating Nano IDs.
 *
 * @property size The length of the generated Nano ID.
 * @property alphabet The alphabet to use when generating the Nano ID.
 * @property affixDecorator The affixation to apply to the generated values.
 * @property arrayDecorator Settings that determine whether the output should be an array of values.
 */
data class NanoIdScheme(
    var size: Int = DEFAULT_SIZE,
    var alphabet: String = DEFAULT_ALPHABET,
    @OptionTag val affixDecorator: AffixDecorator = DEFAULT_AFFIX_DECORATOR,
    @OptionTag val arrayDecorator: ArrayDecorator = DEFAULT_ARRAY_DECORATOR,
) : Scheme() {
    override val name = Bundle("nanoid.title")
    override val typeIcon get() = BASE_ICON
    override val decorators get() = listOf(affixDecorator, arrayDecorator)
    override val validators = validators {
        of(::size)
            .check({ it >= MIN_SIZE }, { Bundle("nanoid.error.size_too_low", MIN_SIZE) })
        of(::alphabet)
            .check({ it.isNotEmpty() }, { Bundle("nanoid.error.alphabet_empty") })
        include(::affixDecorator)
        include(::arrayDecorator)
    }

    override fun generateUndecoratedStrings(count: Int): List<String> =
        List(count) { NanoId.generate(size, alphabet) }

    override fun deepCopy(retainUuid: Boolean) =
        copy(
            affixDecorator = affixDecorator.deepCopy(retainUuid),
            arrayDecorator = arrayDecorator.deepCopy(retainUuid),
        ).deepCopyTransient(retainUuid)

    /**
     * Holds constants.
     */
    companion object {
        /** The base icon for Nano IDs. */
        val BASE_ICON
            get() = TypeIcon(Icons.SCHEME, "id", listOf(JBColor(Color(185, 155, 248, 154), Color(185, 155, 248, 154))))


        /** The minimum allowed value of [size]. */
        const val MIN_SIZE = 1

        /** The default value of [size]. */
        const val DEFAULT_SIZE = 21

        /** The default value of [alphabet]. */
        const val DEFAULT_ALPHABET: String = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

        /** The preset values for the [affixDecorator] field. */
        val PRESET_AFFIX_DECORATOR_DESCRIPTORS = listOf("'", "\"", "`")

        /** The default value of the [affixDecorator] field. */
        val DEFAULT_AFFIX_DECORATOR get() = AffixDecorator(enabled = false, descriptor = "\"")

        /** The default value of the [arrayDecorator] field. */
        val DEFAULT_ARRAY_DECORATOR get() = ArrayDecorator()
    }
}
