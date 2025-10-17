package com.fwdekker.randomness.nanoid

import com.fwdekker.randomness.affix.AffixDecorator
import com.fwdekker.randomness.array.ArrayDecorator
import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.shouldValidateAsBundle
import com.fwdekker.randomness.testhelpers.stateDeepCopyTestFactory
import com.fwdekker.randomness.testhelpers.stateSerializationTestFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeUnique
import io.kotest.matchers.shouldBe

/**
 * Unit tests for [NanoIdScheme].
 */
object NanoIdSchemeTest : FunSpec({
    tags(Tags.PLAIN, Tags.SCHEME)

    context("generateStrings") {
        test("generates IDs of configured length") {
            val scheme = NanoIdScheme(size = 13)
            val id = scheme.generateStrings()[0]
            id.length shouldBe 13
        }

        test("generated characters are from configured alphabet") {
            val scheme = NanoIdScheme(size = 100, alphabet = "abc")
            val id = scheme.generateStrings()[0]
            id.all { it in scheme.alphabet } shouldBe true
        }

        test("generates the requested count") {
            val scheme = NanoIdScheme(size = 8)
            val values = scheme.generateStrings(10)
            values.size shouldBe 10
            // Most likely unique; not strictly required but a good smoke test
            values.shouldBeUnique()
        }

        test("applies decorators in order affix, array") {
            val scheme = NanoIdScheme(
                size = 5,
                affixDecorator = AffixDecorator(enabled = true, descriptor = "#@"),
                arrayDecorator = ArrayDecorator(enabled = true, minCount = 3, maxCount = 3),
            )
            val out = scheme.generateStrings()[0]
            // Expect the affix applied to each element produced by the array decorator -> 3 prefixes
            out.count { it == '#' } shouldBe 3
        }
    }

    context("doValidate") {
        test("succeeds for default state") {
            NanoIdScheme() shouldValidateAsBundle null
        }

        test("fails for too small size") {
            NanoIdScheme(size = 0) shouldValidateAsBundle "nanoid.error.size_too_low"
        }

        test("fails for empty alphabet") {
            NanoIdScheme(alphabet = "") shouldValidateAsBundle "nanoid.error.alphabet_empty"
        }

        test("fails if affix decorator is invalid") {
            NanoIdScheme(affixDecorator = AffixDecorator(enabled = true, descriptor = "\\")) shouldValidateAsBundle ""
        }

        test("fails if array decorator is invalid") {
            NanoIdScheme(arrayDecorator = ArrayDecorator(enabled = true, minCount = -1)) shouldValidateAsBundle ""
        }
    }

    include(stateDeepCopyTestFactory { NanoIdScheme() })

    include(stateSerializationTestFactory { NanoIdScheme() })
})
