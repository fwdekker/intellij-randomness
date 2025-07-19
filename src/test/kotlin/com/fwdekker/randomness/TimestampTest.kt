package com.fwdekker.randomness

import com.fwdekker.randomness.Timestamp.Companion.FORMATTER
import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.shouldValidateAsBundle
import com.fwdekker.randomness.testhelpers.stateDeepCopyTestFactory
import com.fwdekker.randomness.testhelpers.stateSerializationTestFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.row
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlin.random.Random


/**
 * Unit tests for [Timestamp].
 */
object TimestampTest : FunSpec({
    tags(Tags.PLAIN)


    context("epochMilli") {
        withData(
            mapOf(
                "returns the epoch for a complete timestamp" to
                    row("1168-05-20 06:37:39.725", -25_296_600_140_275L),
                "returns the epoch for an autocompleted complete timestamp" to
                    row("5103-07-25", 98_885_577_600_000L),
                "returns null for an empty timestamp" to
                    row("", null),
                "returns null for an invalid non-empty timestamp" to
                    row("invalid", null),
                "returns null for an invalid numeric timestamp" to
                    row("65789190", null),
            )
        ) { (timestamp, expected) -> Timestamp(timestamp).epochMilli shouldBe expected }
    }


    context("constructor") {
        context("timestamp completion") {
            withData(
                mapOf(
                    // Date only
                    "completes the value given a 4-digit year" to
                        row(Timestamp("1674"), "1674-01-01 00:00:00.000"),
                    "completes the value given a 4-digit year and 1-digit month" to
                        row(Timestamp("9172-9"), "9172-09-01 00:00:00.000"),
                    "completes the value given a 4-digit year and 2-digit month" to
                        row(Timestamp("5701-05"), "5701-05-01 00:00:00.000"),
                    "completes the value given a 4-digit year, 2-digit month, and 1-digit day" to
                        row(Timestamp("2812-07-7"), "2812-07-07 00:00:00.000"),
                    "completes the value given a 4-digit year, 2-digit month, and 2-digit day" to
                        row(Timestamp("4174-08-27"), "4174-08-27 00:00:00.000"),
                    // Date and time
                    "completes the value given a complete date, 1-digit hour, and 1-digit minute" to
                        row(Timestamp("6346-08-29 3:1"), "6346-08-29 03:01:00.000"),
                    "completes the value given a complete date, 1-digit hour, and 2-digit minute" to
                        row(Timestamp("5094-12-05 5:14"), "5094-12-05 05:14:00.000"),
                    "completes the value given a complete date, 2-digit hour, and 1-digit minute" to
                        row(Timestamp("9177-08-15 07:4"), "9177-08-15 07:04:00.000"),
                    "completes the value given a complete date, 2-digit hour, and 2-digit minute" to
                        row(Timestamp("9622-09-24 06:06"), "9622-09-24 06:06:00.000"),
                    "completes the value given a complete date, 2-digit hour, 2-digit minute, and 1-digit second" to
                        row(Timestamp("2604-12-07 15:30:7"), "2604-12-07 15:30:07.000"),
                    "completes the value given a complete date, 2-digit hour, 2-digit minute, and 2-digit second" to
                        row(Timestamp("7448-10-25 13:46:21"), "7448-10-25 13:46:21.000"),
                    "completes the value given a complete date-time and 1-digit millisecond" to
                        row(Timestamp("4718-09-12 08:41:06.5"), "4718-09-12 08:41:06.500"),
                    "completes the value given a complete date-time and 2-digit millisecond" to
                        row(Timestamp("9351-02-25 17:53:46.85"), "9351-02-25 17:53:46.850"),
                    "retains the value given a complete date-time" to
                        row(Timestamp("7880-08-27 03:02:18.208"), "7880-08-27 03:02:18.208"),
                    // Date order
                    "interprets dd-dd-dddd as DD-MM-YYYY" to
                        row(Timestamp("15-09-5375 21:02:13.284"), "5375-09-15 21:02:13.284"),
                    "interprets dd/dd/dddd as DD/MM/YYYY" to
                        row(Timestamp("19/05/0004 07:42:16.571"), "0004-05-19 07:42:16.571"),
                    "interprets any grouping of 2-2-4 as DD-MM-YYYY" to
                        row(Timestamp("31(10)0807 20:50:22.209"), "0807-10-31 20:50:22.209"),
                    "interprets any grouping of 4-2-2 as YYYY-MM-DD" to
                        row(Timestamp("0428[01]08 04:55:59.004"), "0428-01-08 04:55:59.004"),
                    "interprets a sequence of 8 digits as YYYY-MM-DD" to
                        row(Timestamp("12970814"), "1297-08-14 00:00:00.000"),
                    // Date parsing
                    "parses English-language month abbreviations" to
                        row(Timestamp("0168 Aug 24 06:05:04.898"), "0168-08-24 06:05:04.898"),
                    "parses English-language month names" to
                        row(Timestamp("0702 January 10 23:57:45.052"), "0702-01-10 23:57:45.052"),
                    "parses a British English date" to
                        row(Timestamp("16 April 0222 17:26:01.811"), "0222-04-16 17:26:01.811"),
                    "parses an American English date" to
                        row(Timestamp("May 3, 0454 19:43:08.435"), "0454-05-03 19:43:08.435"),
                    // Invalid
                    "retains an empty timestamp" to
                        row(Timestamp(""), ""),
                    "retains an invalid non-empty timestamp" to
                        row(Timestamp("invalid"), "invalid"),
                    "retains an invalid numeric timestamp" to
                        row(Timestamp("57819512"), "57819512"),
                )
            ) { (given, expected) -> given.value shouldBe expected }
        }
    }


    context("isBefore") {
        withData(
            mapOf(
                "returns false for identical dates" to
                    row(Timestamp("9510-12-27 23:51:18.556"), Timestamp("9510-12-27 23:51:18.556"), false),
                "returns false if first date is after second date" to
                    row(Timestamp("8552-08-29 08:33:35.784"), Timestamp("7588-07-06 08:48:00.539"), false),
                "returns true if first date is before second date" to
                    row(Timestamp("5494-07-30 01:55:03.144"), Timestamp("7783-01-03 03:33:44.932"), true),
                "returns false if first date is invalid" to
                    row(Timestamp("invalid"), Timestamp("7927-06-10 12:17:15.448"), false),
                "returns false if second date is invalid" to
                    row(Timestamp("8164-03-06 05:00:04.146"), Timestamp("invalid"), false),
                "returns false if both dates are invalid" to
                    row(Timestamp("invalid"), Timestamp("invalid"), false),
            )
        ) { (a, b, expected) -> a.isBefore(b) shouldBe expected }
    }

    context("doValidate") {
        withData(
            mapOf(
                "succeeds for default state" to
                    row(Timestamp(), null),
                "succeeds for a given complete timestamp" to
                    row(Timestamp("8634-01-29 20:28:31.529"), null),
                "succeeds for an autocompleted timestamp" to
                    row(Timestamp("8199-01"), null),
                "fails for an empty timestamp" to
                    row(Timestamp(""), "timestamp.error.parse"),
                "fails for an invalid non-empty timestamp" to
                    row(Timestamp("invalid"), "timestamp.error.parse"),
                "fails for an invalid numeric timestamp" to
                    row(Timestamp("71895819"), "timestamp.error.parse"),
            )
        ) { (timestamp, validation) -> timestamp shouldValidateAsBundle validation }
    }


    context("fromEpochMilli") {
        withData(
            mapOf(
                "returns the timestamp for epoch 0" to
                    row(0L, "1970-01-01 00:00:00.000"),
                "returns the timestamp for a four-digit future year" to
                    row(197_276_354_331_758L, "8221-06-10 03:18:51.758"),
                "returns the timestamp for a four-digit past year" to
                    row(-108_783_788_697L, "1966-07-21 22:16:51.303"),
                "returns the timestamp for a five-digit future year" to
                    row(421_949_603_579_485L, "+15341-01-18 23:12:59.485"),
            )
        ) { (epochMilli, expected) -> Timestamp.fromEpochMilli(epochMilli).value shouldBe expected }
    }


    include(stateDeepCopyTestFactory { Timestamp() })

    include(stateSerializationTestFactory { Timestamp() })
})

/**
 * Unit tests for extension functions in `TimestampKt`.
 */
object TimestampKtTest : FunSpec({
    tags(Tags.PLAIN)


    context("nextTimestampInclusive") {
        test("generates the only possible value when min equals max") {
            val timestamp = Timestamp("9181-07-18 10:59:33.663")

            Random.nextTimestampInclusive(timestamp, timestamp).format(FORMATTER) shouldBe timestamp.value
        }

        test("generates each value in the inclusive range") {
            // Probability of less than 10 / 65_536 of failure

            val min = Timestamp("5243-07-29 22:21:27.276")
            val max = Timestamp("5243-07-29 22:21:27.285")

            listOf(
                Timestamp("5243-07-29 22:21:27.276"),
                Timestamp("5243-07-29 22:21:27.277"),
                Timestamp("5243-07-29 22:21:27.278"),
                Timestamp("5243-07-29 22:21:27.279"),
                Timestamp("5243-07-29 22:21:27.280"),
                Timestamp("5243-07-29 22:21:27.281"),
                Timestamp("5243-07-29 22:21:27.282"),
                Timestamp("5243-07-29 22:21:27.283"),
                Timestamp("5243-07-29 22:21:27.284"),
                Timestamp("5243-07-29 22:21:27.285"),
            ).forEach { target ->
                List(128) { Random.nextTimestampInclusive(min, max).format(FORMATTER) } shouldContain target.value
            }
        }

        test("generates values in between min and max (and probably not min or max itself)") {
            // Probability of 512 / 123_926_552_811_472 of failure

            val min = Timestamp("0672-11-17 19:45:39.080")
            val max = Timestamp("6541-12-18 22:12:30.552")

            repeat(256) {
                val timestamp = Timestamp(Random.nextTimestampInclusive(min, max).format(FORMATTER))

                min.isBefore(timestamp) shouldBe true
                timestamp.isBefore(max) shouldBe true
            }
        }
    }
})
