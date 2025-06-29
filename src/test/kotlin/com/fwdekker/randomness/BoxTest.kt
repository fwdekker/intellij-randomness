package com.fwdekker.randomness

import com.fwdekker.randomness.testhelpers.Tags
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe


/**
 * Unit tests for [Box].
 */
object BoxTest : FunSpec({
    tags(Tags.PLAIN)


    context("unaryPlus") {
        test("returns the generator's value when de-referenced") {
            val box = Box({ "contents" })

            +box shouldBe "contents"
        }

        test("returns the same value when de-referenced again") {
            val box = Box({ "contents" })

            +box

            +box shouldBe "contents"
        }

        test("does not invoke the generator when de-referenced again") {
            var generatorInvokeCount = 0
            val box = Box({
                generatorInvokeCount++
                "contents"
            })
            withClue("Generator should not be invoked during construction") { generatorInvokeCount shouldBe 0 }

            +box
            +box

            generatorInvokeCount shouldBe 1
        }
    }


    context("copy") {
        test("creates an independent copy if copied before first de-reference") {
            val box = Box({ mutableListOf("old") })

            val copy = box.copy()
            (+copy)[0] = "new"

            (+box)[0] shouldBe "old"
            (+copy)[0] shouldBe "new"
        }

        test("copies the generated value if copied after first de-reference") {
            val box = Box({ mutableListOf("old") })
            +box

            val copy = box.copy()
            (+copy)[0] = "new"

            (+box)[0] shouldBe "new"
            (+copy)[0] shouldBe "new"
        }
    }
})
