package com.fwdekker.randomness.testhelpers

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe


/**
 * Non-comprehensive tests and sanity checks for [DummyScheme].
 */
object DummySchemeTest : FunSpec({
    tags(Tags.PLAIN, Tags.SCHEME)


    test("does not equal another fresh instance") {
        DummyScheme() shouldNotBe DummyScheme()
    }

    test("equals its own copy if the UUID is retained") {
        val scheme = DummyScheme()

        scheme shouldBe scheme.deepCopy(retainUuid = true)
    }

    test("does not equal its copy if the UUID is not retained") {
        val scheme = DummyScheme()

        scheme shouldBe scheme.deepCopy(retainUuid = false)
    }
})
