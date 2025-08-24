package com.fwdekker.randomness.word

import com.fwdekker.randomness.CapitalizationMode
import com.fwdekker.randomness.affix.AffixDecorator
import com.fwdekker.randomness.array.ArrayDecorator
import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.shouldValidateAsBundle
import com.fwdekker.randomness.testhelpers.stateDeepCopyTestFactory
import com.fwdekker.randomness.testhelpers.stateSerializationTestFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.row
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe


/**
 * Unit tests for [WordScheme].
 */
object WordSchemeTest : FunSpec({
    tags(Tags.PLAIN, Tags.SCHEME)


    context("generateStrings") {
        withData(
            mapOf(
                "returns a word" to
                    row(WordScheme(words = listOf("word")), "word"),
                "returns a word with whitespace" to
                    row(WordScheme(words = listOf("x y")), "x y"),
                "capitalizes the word" to
                    row(WordScheme(words = listOf("word"), capitalization = CapitalizationMode.UPPER), "WORD"),
                "applies decorators in order affix, array" to
                    row(
                        WordScheme(
                            words = listOf("word"),
                            affixDecorator = AffixDecorator(enabled = true, descriptor = "'"),
                            arrayDecorator = ArrayDecorator(enabled = true),
                        ),
                        "['word', 'word', 'word']",
                    ),
            )
        ) { (scheme, output) -> scheme.generateStrings()[0] shouldBe output }
    }

    context("doValidate") {
        withData(
            mapOf(
                "succeeds for default state" to
                    row(WordScheme(), null),
                "fails if word list is empty" to
                    row(WordScheme(words = emptyList()), "word.error.empty_word_list"),
                "fails if affix decorator is invalid" to
                    row(WordScheme(affixDecorator = AffixDecorator(enabled = true, descriptor = """\""")), ""),
                "fails if array decorator is invalid" to
                    row(WordScheme(arrayDecorator = ArrayDecorator(enabled = true, minCount = -24)), ""),
            )
        ) { (scheme, validation) -> scheme shouldValidateAsBundle validation }
    }

    include(stateDeepCopyTestFactory { WordScheme() })

    include(stateSerializationTestFactory { WordScheme() })
})
