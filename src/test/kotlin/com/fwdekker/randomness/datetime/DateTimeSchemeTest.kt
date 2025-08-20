package com.fwdekker.randomness.datetime

import com.fwdekker.randomness.Timestamp
import com.fwdekker.randomness.integer.IntegerScheme
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
 * Unit tests for [DateTimeScheme].
 */
object DateTimeSchemeTest : FunSpec({
    tags(Tags.PLAIN, Tags.SCHEME)


    context("generateStrings") {
        withData(
            mapOf(
                "returns date at given timestamp" to
                    row(DateTimeScheme().withDateTime("2768-06-30 18:01:48.695"), "2768-06-30 18:01:48.695"),
                "returns date with given format" to
                    row(DateTimeScheme(pattern = "yyyy.MM").withDateTime("6999-03-29"), "6999.03"),
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
                "succeeds for default state" to row(DateTimeScheme(), null),
                "fails for invalid min date-time" to
                    row(DateTimeScheme(minDateTime = Timestamp("invalid")), "timestamp.error.parse"),
                "fails for invalid max date-time" to
                    row(DateTimeScheme(maxDateTime = Timestamp("invalid")), "timestamp.error.parse"),
                "fails if min date-time is above max date-time" to
                    row(
                        DateTimeScheme(minDateTime = Timestamp("4434"), maxDateTime = Timestamp("1853")),
                        "datetime.error.min_datetime_above_max",
                    ),
                "fails if pattern is invalid" to row(DateTimeScheme(pattern = "yyyy-ffff"), ""),
                "succeeds if invalid pattern is escaped" to row(DateTimeScheme(pattern = "yyyy-'ffff'"), null),
            )
        ) { (scheme, validation) -> scheme shouldValidateAsBundle validation }
    }

    include(stateDeepCopyTestFactory { DateTimeScheme() })

    include(stateSerializationTestFactory { DateTimeScheme() })
})


/**
 * Sets the [DateTimeScheme.minDateTime] and [DateTimeScheme.maxDateTime] to [dateTime].
 */
private fun DateTimeScheme.withDateTime(dateTime: String): DateTimeScheme {
    this.maxDateTime = Timestamp(dateTime)
    this.minDateTime = Timestamp(dateTime)
    return this
}
