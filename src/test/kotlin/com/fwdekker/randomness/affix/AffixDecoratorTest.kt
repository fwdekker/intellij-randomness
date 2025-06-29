package com.fwdekker.randomness.affix

import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.shouldValidateAsBundle
import com.fwdekker.randomness.testhelpers.stateDeepCopyTestFactory
import com.fwdekker.randomness.testhelpers.stateSerializationTestFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.row
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe


/**
 * Unit tests for [AffixDecorator].
 */
object AffixDecoratorTest : FunSpec({
    tags(Tags.PLAIN, Tags.SCHEME)


    context("generateStrings") {
        withData(
            mapOf(
                "returns default input if disabled" to
                    row(AffixDecorator(enabled = false, descriptor = """<@>"""), """[i0]"""),
                "appends and prepends if no '@'" to
                    row(AffixDecorator(enabled = true, descriptor = """*"""), "*[i0]*"),
                "replaces 'at' with input" to
                    row(AffixDecorator(enabled = true, descriptor = "(@)"), "([i0])"),
                "replaces multiple 'at' with input" to
                    row(AffixDecorator(enabled = true, descriptor = "(@|@)"), "([i0]|[i0])"),
                "interprets escaped 'at' as literal" to
                    row(AffixDecorator(enabled = true, descriptor = """(\@)"""), """(@)[i0](@)"""),
                "interprets escaped 'backslash' as literal" to
                    row(AffixDecorator(enabled = true, descriptor = """(\\@)"""), """(\[i0])"""),
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
                    row(AffixDecorator(), null),
                "succeeds for empty descriptor" to
                    row(AffixDecorator(enabled = true, descriptor = ""), null),
                "succeeds for complex descriptor" to
                    row(AffixDecorator(enabled = true, descriptor = """\\@\@@\@\\"""), null),
                "fails descriptor has single trailing backslash" to
                    row(AffixDecorator(enabled = true, descriptor = """\"""), "affix.error.trailing_escape"),
                "succeeds if descriptor has double trailing backslash" to
                    row(AffixDecorator(enabled = true, descriptor = """\\"""), null),
                "fails if descriptors has triple trailing backslash" to
                    row(AffixDecorator(enabled = true, descriptor = """\\\"""), "affix.error.trailing_escape"),
                "fails for invalid settings even if disabled" to
                    row(AffixDecorator(enabled = false, descriptor = """\"""), "affix.error.trailing_escape"),
            )
        ) { (scheme, validation) ->
            scheme.generator = { count -> List(count) { "[i$it]" } }

            scheme shouldValidateAsBundle validation
        }
    }

    include(stateDeepCopyTestFactory { AffixDecorator() })

    include(stateSerializationTestFactory { AffixDecorator() })
})
