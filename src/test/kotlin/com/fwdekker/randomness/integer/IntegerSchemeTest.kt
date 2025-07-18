package com.fwdekker.randomness.integer

import com.fwdekker.randomness.affix.AffixDecorator
import com.fwdekker.randomness.array.ArrayDecorator
import com.fwdekker.randomness.fixedlength.FixedLengthDecorator
import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.shouldValidateAsBundle
import com.fwdekker.randomness.testhelpers.stateDeepCopyTestFactory
import com.fwdekker.randomness.testhelpers.stateSerializationTestFactory
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.row
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe


/**
 * Unit tests for [IntegerScheme].
 */
object IntegerSchemeTest : FunSpec({
    tags(Tags.PLAIN, Tags.SCHEME)


    context("generateStrings") {
        withData(
            mapOf(
                "returns zero" to
                    row(IntegerScheme().withValue(0L), "0"),
                "returns one" to
                    row(IntegerScheme().withValue(1L), "1"),
                "returns a negative value" to
                    row(IntegerScheme().withValue(-660L), "-660"),
                "returns a positive value" to
                    row(IntegerScheme().withValue(708L), "708"),
                "returns Long.MIN_VALUE" to
                    row(IntegerScheme().withValue(Long.MIN_VALUE), "-9223372036854775808"),
                "returns Long.MAX_VALUE" to
                    row(IntegerScheme().withValue(Long.MAX_VALUE), "9223372036854775807"),
                "converts to base <10" to
                    row(IntegerScheme(base = 2).withValue(141L), "10001101"),
                "converts to base >10" to
                    row(IntegerScheme(base = 12).withValue(248L), "188"),
                "uses separator" to
                    row(
                        IntegerScheme(groupingSeparatorEnabled = true, groupingSeparator = "#").withValue(949_442L),
                        "949#442",
                    ),
                "uses no separator if disabled" to
                    row(
                        IntegerScheme(groupingSeparatorEnabled = false, groupingSeparator = "#").withValue(179_644L),
                        "179644",
                    ),
                "uses no separator in base <10" to
                    row(
                        IntegerScheme(
                            groupingSeparatorEnabled = true,
                            base = 8,
                            groupingSeparator = "#"
                        ).withValue(731_942L),
                        "2625446"
                    ),
                "uses no separator in base >10" to
                    row(
                        IntegerScheme(
                            groupingSeparatorEnabled = true,
                            base = 12,
                            groupingSeparator = "#"
                        ).withValue(586_394L),
                        "243422",
                    ),
                "retains lowercase in base >10 if disabled" to
                    row(IntegerScheme(base = 14, isUppercase = false).withValue(829_960L), "17866c"),
                "converts to uppercase in base >10 if enabled" to
                    row(IntegerScheme(base = 13, isUppercase = true).withValue(451_922L), "12A913"),
                "applies decorators in order affix, fixed length, array" to
                    row(
                        IntegerScheme(
                            affixDecorator = AffixDecorator(enabled = true, descriptor = "@L"),
                            fixedLengthDecorator = FixedLengthDecorator(enabled = true),
                            arrayDecorator = ArrayDecorator(enabled = true),
                        ).withValue(53L),
                        "[053L, 053L, 053L]",
                    ),
            )
        ) { (scheme, output) -> scheme.generateStrings()[0] shouldBe output }

        test("correctly generates distinct values at maximum range size") {
            val scheme = IntegerScheme(minValue = Long.MIN_VALUE, maxValue = Long.MAX_VALUE)

            withClue("Should have distinct elements") { scheme.generateStrings(count = 50).toSet() shouldHaveSize 50 }
        }
    }

    context("doValidate") {
        withData(
            mapOf(
                "succeeds for default state" to
                    row(IntegerScheme(), null),
                "fails if min value is above max value" to
                    row(IntegerScheme(minValue = 671L, maxValue = 93L), "integer.error.min_value_above_max"),
                "fails if base is negative" to
                    row(IntegerScheme(base = -48), "integer.error.base_range"),
                "fails if base is zero" to
                    row(IntegerScheme(base = -0), "integer.error.base_range"),
                "fails if base is one" to
                    row(IntegerScheme(base = 1), "integer.error.base_range"),
                "fails if base is greater than 30" to
                    row(IntegerScheme(base = 704), "integer.error.base_range"),
                "fails if grouping separator is empty" to
                    row(IntegerScheme(groupingSeparator = ""), "integer.error.grouping_separator_length"),
                "fails if grouping separator is non-char" to
                    row(IntegerScheme(groupingSeparator = "long"), "integer.error.grouping_separator_length"),
                "fails if fixed-length decorator is invalid" to
                    row(IntegerScheme(fixedLengthDecorator = FixedLengthDecorator(length = -3)), ""),
                "fails if affix decorator is invalid" to
                    row(IntegerScheme(affixDecorator = AffixDecorator(descriptor = """\""")), ""),
                "fails if array decorator is invalid" to
                    row(IntegerScheme(arrayDecorator = ArrayDecorator(minCount = -24)), ""),
            )
        ) { (scheme, validation) -> scheme shouldValidateAsBundle validation }
    }

    include(stateDeepCopyTestFactory { IntegerScheme() })

    include(stateSerializationTestFactory { IntegerScheme() })
})


/**
 * Sets the [IntegerScheme.minValue] and [IntegerScheme.maxValue] to [value].
 */
private fun IntegerScheme.withValue(value: Long): IntegerScheme {
    minValue = value
    maxValue = value
    return this
}
