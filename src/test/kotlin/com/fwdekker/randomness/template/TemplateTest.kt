package com.fwdekker.randomness.template

import com.fwdekker.randomness.CapitalizationMode
import com.fwdekker.randomness.DataGenerationException
import com.fwdekker.randomness.Icons
import com.fwdekker.randomness.Settings
import com.fwdekker.randomness.array.ArrayDecorator
import com.fwdekker.randomness.integer.IntegerScheme
import com.fwdekker.randomness.string.StringScheme
import com.fwdekker.randomness.testhelpers.DummyScheme
import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.shouldValidateAsBundle
import com.fwdekker.randomness.testhelpers.stateDeepCopyTestFactory
import com.fwdekker.randomness.testhelpers.stateSerializationTestFactory
import com.fwdekker.randomness.word.WordScheme
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.row
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.random.Random


/**
 * Unit tests for [Template].
 */
object TemplateTest : FunSpec({
    tags(Tags.PLAIN, Tags.SCHEME)


    context("typeIcon") {
        test("returns the default icon if the template contains no schemes") {
            val template = Template()

            template.typeIcon shouldBe Template.DEFAULT_ICON
        }

        test("returns the default icon if the single scheme has no icon") {
            val scheme = DummyScheme().also { it.typeIcon = null }
            val template = Template(schemes = mutableListOf(scheme))

            template.typeIcon shouldBe Template.DEFAULT_ICON
        }

        test("returns a template version of the single scheme's TypeIcon") {
            val scheme = IntegerScheme()
            val template = Template(schemes = mutableListOf(scheme))

            template.typeIcon.base shouldBe Icons.TEMPLATE
            template.typeIcon.text shouldBe scheme.typeIcon.text
            template.typeIcon.colors shouldBe scheme.typeIcon.colors
        }

        test("returns the scheme's combined icon if multiple are present") {
            val template = Template(schemes = mutableListOf(DummyScheme(), DummyScheme()))

            template.typeIcon.colors.size shouldBeGreaterThan 1
        }
    }

    context("applyContext") {
        test("applies the context to itself") {
            val newContext = Settings()
            val template = Template()

            template.applyContext(newContext)

            +template.context shouldBeSameInstanceAs newContext
        }

        test("applies the context to each scheme") {
            val newContext = Settings()
            val template = Template(schemes = mutableListOf(DummyScheme(), DummyScheme()))

            template.applyContext(newContext)

            template.schemes.forEach { +it.context shouldBeSameInstanceAs newContext }
        }
    }


    context("generateStrings") {
        withData(
            mapOf(
                "returns an empty string if it contains no schemes" to
                    row(Template(), ""),
                "returns the single scheme's output" to
                    row(Template(schemes = mutableListOf(DummyScheme())), "text0"),
                "returns the concatenation of the schemes' outputs" to
                    row(
                        Template(
                            schemes = mutableListOf(
                                DummyScheme(prefix = "a"),
                                DummyScheme(prefix = "b"),
                                DummyScheme(prefix = "c"),
                            ),
                        ),
                        "a0b0c0",
                    ),
            )
        ) { (scheme, output) -> scheme.generateStrings()[0] shouldBe output }

        test("throws an exception if the template is invalid") {
            val template = Template(schemes = mutableListOf(DummyScheme(valid = false)))

            shouldThrow<DataGenerationException> { template.generateStrings() }
        }

        context("rng") {
            test("returns the same given the same seed") {
                val template = Template(schemes = mutableListOf(StringScheme("[a-zA-Z]{10}"), IntegerScheme()))

                val outputs1 = template.also { it.random = Random(1) }.generateStrings(5)
                val outputs2 = template.also { it.random = Random(1) }.generateStrings(5)

                outputs1 shouldContainExactly outputs2
            }

            test("sets the rng of a scheme independent of scheme order") {
                val schemeA = IntegerScheme()
                val glue = StringScheme("-")
                val schemeB = IntegerScheme()

                val output1 = Template(schemes = mutableListOf(schemeA, glue, schemeB))
                    .also { it.random = Random(1) }
                    .generateStrings()[0]
                val output2 = Template(schemes = mutableListOf(schemeB, glue, schemeA))
                    .also { it.random = Random(1) }
                    .generateStrings()[0]

                output1.split("-").asReversed() shouldContainExactly output2.split("-")
            }

            test("sets the rng independent of the rng of other schemes") {
                val prefix = WordScheme(words = listOf("pre"))
                val scheme = IntegerScheme()
                val postfix = WordScheme(words = listOf("post"))
                val template = Template(schemes = mutableListOf(prefix, scheme, postfix))

                val output1 = template.also { it.random = Random(1) }.generateStrings()[0]
                prefix.capitalization = CapitalizationMode.RANDOM
                postfix.capitalization = CapitalizationMode.RANDOM
                val output2 = template.also { it.random = Random(1) }.generateStrings()[0]

                output1.drop(3).dropLast(4) shouldBe output2.drop(3).dropLast(4)
            }
        }
    }

    context("doValidate") {
        withData(
            mapOf(
                "succeeds for default state" to
                    row(Template(), null),
                "fails if name is blank" to
                    row(Template("  "), "template.error.no_name"),
                "fails if scheme is invalid" to
                    row(Template("Template", mutableListOf(DummyScheme(valid = false))), ""),
                "fails if array decorator is invalid" to
                    row(Template(arrayDecorator = ArrayDecorator(enabled = true, minCount = -24)), ""),
            )
        ) { (scheme, validation) -> scheme shouldValidateAsBundle validation }
    }

    include(stateDeepCopyTestFactory { Template() })

    include(stateSerializationTestFactory { Template(schemes = mutableListOf(IntegerScheme())) })
})
