package com.fwdekker.randomness

import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.shouldMatchBundle
import io.kotest.assertions.retry
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.row
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldBeEqualIgnoringCase
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes


/**
 * Unit tests for [CapitalizationMode].
 */
object CapitalizationModeTest : FunSpec({
    tags(Tags.PLAIN)


    val random = Random.Default


    context("transform") {
        context("retain mode") {
            test("does nothing to a string") {
                CapitalizationMode.RETAIN.transform("LOREm IpSuM", random) shouldBe "LOREm IpSuM"
            }
        }

        context("sentence mode") {
            test("does nothing to an empty string") {
                CapitalizationMode.SENTENCE.transform("", random) shouldBe ""
            }

            test("changes a string to sentence case") {
                CapitalizationMode.SENTENCE.transform("LOrEM ipsUm", random) shouldBe "Lorem ipsum"
            }
        }

        context("uppercase mode") {
            test("changes all characters to uppercase") {
                CapitalizationMode.UPPER.transform("LoREm iPSUM", random) shouldBe "LOREM IPSUM"
            }
        }

        context("lowercase mode") {
            test("changes all characters to lowercase") {
                CapitalizationMode.LOWER.transform("lorEm IpSUM", random) shouldBe "lorem ipsum"
            }
        }

        context("first letter mode") {
            test("changes all first letters to uppercase") {
                CapitalizationMode.FIRST_LETTER.transform("lOReM IPSum", random) shouldBe "Lorem Ipsum"
            }
        }

        context("random mode") {
            test("changes the capitalization to something else") {
                retry(100, 1.minutes) {
                    val input = "lOReM ipsUm"
                    val output = CapitalizationMode.RANDOM.transform(input, random)

                    output shouldNotBe input // Has a chance of 0.002% of failing
                    output shouldBeEqualIgnoringCase input
                }
            }

            test("produces reproducibly random strings") {
                val transform = { CapitalizationMode.RANDOM.transform("lorem ipsum", Random(0)) }

                transform() shouldBe transform()
            }
        }
    }

    context("toLocalizedString") {
        test("returns the associated localized string") {
            CapitalizationMode.FIRST_LETTER.toLocalizedString() shouldMatchBundle "shared.capitalization.first_letter"
        }
    }
})


/**
 * Unit tests for capitalization-related helper functions in `CapitalizationModeKt`.
 */
object CapitalizationModeKtTest : FunSpec({
    context("lowerCaseFirst") {
        withData(
            mapOf(
                "outputs an empty string if the input is empty" to row("", ""),
                "does nothing if the first character is already in lowercase" to row("abcD eFgH", "abcD eFgH"),
                "changes the first character to lowercase" to row("AbcD EfGh", "abcD EfGh"),
            )
        ) { (input, expected) -> input.lowerCaseFirst() shouldBe expected }
    }

    context("upperCaseFirst") {
        withData(
            mapOf(
                "outputs an empty string if the input is empty" to row("", ""),
                "does nothing if the first character is already in uppercase" to row("AbcD eFgH", "AbcD eFgH"),
                "changes the first character to uppercase" to row("abCd eFgH", "AbCd eFgH"),
            )
        ) { (input, expected) -> input.upperCaseFirst() shouldBe expected }
    }

    context("camelPlus") {
        withData(
            mapOf(
                "outputs an empty string if both are empty" to row("", "", ""),
                "simply appends if the first part is empty" to row("", "fooBar", "fooBar"),
                "simply appends if the second part is empty" to row("fooBar", "", "fooBar"),
                "concatenates two words in camel case" to row("foo", "bar", "fooBar"),
                "concatenates two pairs of words to camel case" to row("fooBar", "bazQux", "fooBarBazQux"),
            )
        ) { (first, second, expected) -> first.camelPlus(second) shouldBe expected }
    }
})
