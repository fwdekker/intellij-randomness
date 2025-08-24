package com.fwdekker.randomness.fixedlength

import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.shouldValidateAsBundle
import com.fwdekker.randomness.testhelpers.stateDeepCopyTestFactory
import com.fwdekker.randomness.testhelpers.stateSerializationTestFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.row
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe


/**
 * Unit tests for [FixedLengthDecorator].
 */
object FixedLengthDecoratorTest : FunSpec({
    tags(Tags.PLAIN, Tags.SCHEME)


    context("generateStrings") {
        withData(
            mapOf(
                "returns default input if disabled" to
                    row(FixedLengthDecorator(enabled = false, length = 1), "[i0]"),
                "returns shortened string" to
                    row(FixedLengthDecorator(enabled = true, length = 3), "[i0"),
                "returns padded string" to
                    row(FixedLengthDecorator(enabled = true, length = 5, filler = "f"), "f[i0]"),
                "returns default input if correct length" to
                    row(FixedLengthDecorator(enabled = true, length = 4), "[i0]"),
            )
        ) { (scheme, output) ->
            scheme.generator = { count -> List(count) { "[i$it]" } }

            scheme.generateStrings()[0] shouldBe output
        }
    }

    context("doValidate") {
        withData(
            mapOf(
                "succeeds for default state" to
                    row(FixedLengthDecorator(), null),
                "succeeds for one length" to
                    row(FixedLengthDecorator(enabled = true, length = 1), null),
                "fails for zero length" to
                    row(FixedLengthDecorator(enabled = true, length = 0), "fixed_length.error.length_too_low"),
                "fails for negative length" to
                    row(FixedLengthDecorator(enabled = true, length = -4), "fixed_length.error.length_too_low"),
                "fails for empty filler" to
                    row(FixedLengthDecorator(enabled = true, filler = ""), "fixed_length.error.filler_length"),
                "fails for non-char filler" to
                    row(FixedLengthDecorator(enabled = true, filler = "long"), "fixed_length.error.filler_length"),
                "ignores invalid settings if disabled" to
                    row(FixedLengthDecorator(enabled = false, filler = "long"), null),
            )
        ) { (scheme, validation) ->
            scheme.generator = { List(it) { "[in]" } }

            scheme shouldValidateAsBundle validation
        }
    }

    include(stateDeepCopyTestFactory { FixedLengthDecorator() })

    include(stateSerializationTestFactory { FixedLengthDecorator() })
})
