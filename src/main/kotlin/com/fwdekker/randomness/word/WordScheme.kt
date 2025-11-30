package com.fwdekker.randomness.word

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.CapitalizationMode
import com.fwdekker.randomness.Icons
import com.fwdekker.randomness.Scheme
import com.fwdekker.randomness.TypeIcon
import com.fwdekker.randomness.affix.AffixDecorator
import com.fwdekker.randomness.array.ArrayDecorator
import com.fwdekker.randomness.template.Template
import com.fwdekker.randomness.template.TemplateList
import com.fwdekker.randomness.ui.ValidatorDsl.Companion.validators
import com.intellij.ui.JBColor
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.Transient
import java.awt.Color


/**
 * Contains settings for generating random words.
 *
 * @property words The list of words to choose from.
 * @property capitalization The way in which the generated word should be capitalized.
 * @property affixDecorator The affixation to apply to the generated values.
 * @property arrayDecorator Settings that determine whether the output should be an array of values.
 */
data class WordScheme(
    var wordListName: String? = null, // TODO: Use UUID instead of name
    var capitalization: CapitalizationMode = DEFAULT_CAPITALIZATION,
    @OptionTag val affixDecorator: AffixDecorator = DEFAULT_AFFIX_DECORATOR,
    @OptionTag val arrayDecorator: ArrayDecorator = DEFAULT_ARRAY_DECORATOR,
) : Scheme() {
    @get:Transient
    override val name = Bundle("word.title")
    override val typeIcon get() = BASE_ICON
    override val decorators get() = listOf(affixDecorator, arrayDecorator)
    override val validators = validators {
        of(::wordList).check({ it != null }, { "Invalid word list." }) // TODO: Use bundle
        include(::affixDecorator)
        include(::arrayDecorator)
    }

    /**
     * The [Template] that is being referenced, or `null` if it could not be found in the [context]'s [TemplateList].
     */
    @get:Transient
    var wordList: WordList?
        get() = (+context).wordListList.wordLists.find { it.name == wordListName }
        set(value) {
            wordListName = value?.name
        }


    /**
     * Returns [count] formatted random words.
     */
    override fun generateUndecoratedStrings(count: Int) =
        List(count) { capitalization.transform(wordList!!.words.random(random), random) }


    override fun deepCopy(retainUuid: Boolean) =
        copy(
            wordListName = wordListName,
            affixDecorator = affixDecorator.deepCopy(retainUuid),
            arrayDecorator = arrayDecorator.deepCopy(retainUuid),
        ).deepCopyTransient(retainUuid)


    /**
     * Holds constants.
     */
    companion object {
        /**
         * The base icon for words.
         */
        val BASE_ICON
            get() = TypeIcon(Icons.SCHEME, "cat", listOf(JBColor(Color(242, 101, 34, 154), Color(242, 101, 34, 154))))

        /**
         * The default value of the [words] field.
         */
        val DEFAULT_WORDS get() = listOf("lorem", "ipsum", "dolor", "sit", "amet")

        /**
         * The preset values for the [capitalization] field.
         */
        val PRESET_CAPITALIZATION = listOf(
            CapitalizationMode.RETAIN,
            CapitalizationMode.LOWER,
            CapitalizationMode.UPPER,
            CapitalizationMode.RANDOM,
            CapitalizationMode.SENTENCE,
            CapitalizationMode.FIRST_LETTER,
        )

        /**
         * The default value of the [capitalization] field.
         */
        val DEFAULT_CAPITALIZATION = CapitalizationMode.RETAIN

        /**
         * The preset values for the [affixDecorator] descriptor.
         */
        val PRESET_AFFIX_DECORATOR_DESCRIPTORS = listOf("'", "\"", "`")

        /**
         * The default value of the [affixDecorator] field.
         */
        val DEFAULT_AFFIX_DECORATOR get() = AffixDecorator(enabled = false, descriptor = "\"")

        /**
         * The default value of the [arrayDecorator] field.
         */
        val DEFAULT_ARRAY_DECORATOR get() = ArrayDecorator()
    }
}
