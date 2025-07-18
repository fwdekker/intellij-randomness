package com.fwdekker.randomness.uuid

import com.fwdekker.randomness.affix.AffixDecorator
import com.fwdekker.randomness.array.ArrayDecorator
import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.shouldValidateAsBundle
import com.fwdekker.randomness.testhelpers.stateDeepCopyTestFactory
import com.fwdekker.randomness.testhelpers.stateSerializationTestFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.row
import io.kotest.datatest.withData
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.beLowerCase
import io.kotest.matchers.string.beUpperCase
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.util.UUID


/**
 * Unit tests for [UuidScheme].
 */
object UuidSchemeTest : FunSpec({
    tags(Tags.PLAIN, Tags.SCHEME)


    context("generateStrings") {
        context("generates a UUID for all supported versions") {
            withData(UuidScheme.SUPPORTED_VERSIONS) { version ->
                UUID.fromString(UuidScheme(version = version).generateStrings()[0]).version() shouldBe version
            }
        }

        test("returns uppercase string") {
            UuidScheme(isUppercase = true).generateStrings()[0] should beUpperCase()
        }

        test("returns lowercase string") {
            UuidScheme(isUppercase = false).generateStrings()[0] should beLowerCase()
        }

        test("returns string with dashes") {
            UuidScheme(addDashes = true).generateStrings()[0] shouldContain "-"
        }

        test("returns string without dashes") {
            UuidScheme(addDashes = false).generateStrings()[0] shouldNotContain "-"
        }

        test("applies decorators in order affix, array") {
            UuidScheme(
                affixDecorator = AffixDecorator(enabled = true, descriptor = "#@"),
                arrayDecorator = ArrayDecorator(enabled = true, minCount = 3, maxCount = 3),
            ).generateStrings()[0].count { it == '#' } shouldBe 3
        }
    }

    context("doValidate") {
        withData(
            mapOf(
                "succeeds for default state" to
                    row(UuidScheme(), null),
                "fails for unsupported version" to
                    row(UuidScheme(version = 14), "uuid.error.unknown_version"),
                "fails if affix decorator is invalid" to
                    row(UuidScheme(affixDecorator = AffixDecorator(descriptor = """\""")), ""),
                "fails if array decorator is invalid" to
                    row(UuidScheme(arrayDecorator = ArrayDecorator(minCount = -539)), ""),
            )
        ) { (scheme, validation) -> scheme shouldValidateAsBundle validation }
    }

    include(stateDeepCopyTestFactory { UuidScheme() })

    include(stateSerializationTestFactory { UuidScheme() })
})
