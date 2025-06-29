package com.fwdekker.randomness

import com.fwdekker.randomness.testhelpers.DummyState
import com.fwdekker.randomness.testhelpers.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs


/**
 * Unit tests for [State].
 */
object StateTest : FunSpec({
    tags(Tags.PLAIN, Tags.SCHEME)


    context("uuid") {
        test("generates a different UUID for each instance") {
            DummyState().uuid shouldNotBe DummyState().uuid
        }
    }

    context("context") {
        test("uses the applied context") {
            val context = Settings()
            val original = DummyState()

            original.applyContext(context)

            +original.context shouldBeSameInstanceAs context
        }
    }


    context("deepCopy") {
        test("retains UUID if indicated") {
            val original = DummyState()

            val copy = original.deepCopy(retainUuid = true)

            copy.uuid shouldBe original.uuid
        }

        test("does not retain UUID if not indicated") {
            val original = DummyState()

            val copy = original.deepCopy(retainUuid = false)

            copy.uuid shouldNotBe original.uuid
        }

        test("copies the context") {
            val context = Settings()
            val original = DummyState()
            original.applyContext(context)

            val copy = original.deepCopy()

            +copy.context shouldBeSameInstanceAs context
        }

        test("deep-copies fields") {
            val original = DummyState()
            val copy = original.deepCopy()

            copy.list.add(0)

            original.list shouldHaveSize 0
            copy.list shouldHaveSize 1
        }
    }
})
