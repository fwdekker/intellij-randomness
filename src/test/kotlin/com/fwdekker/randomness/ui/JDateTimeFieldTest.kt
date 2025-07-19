package com.fwdekker.randomness.ui

import com.fwdekker.randomness.Timestamp
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import com.fwdekker.randomness.testhelpers.runEdt
import com.fwdekker.randomness.testhelpers.shouldMatchBundle
import com.fwdekker.randomness.testhelpers.useEdtViolationDetection
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe


/**
 * Unit tests for [JDateTimeField].
 */
object JDateTimeFieldTest : FunSpec({
    lateinit var field: JDateTimeField


    useEdtViolationDetection()

    beforeNonContainer {
        field = runEdt { JDateTimeField() }
    }


    context("value") {
        context("get") {
            test("returns the default if no value has been set") {
                val default = Timestamp("2034")
                val myField = runEdt { JDateTimeField(default) }

                myField.value shouldBe default
            }
        }

        context("set") {
            test("throws an error if a non-date-time is set") {
                shouldThrow<IllegalArgumentException> { runEdt { field.setValue("invalid") } }
                    .message shouldMatchBundle "datetime_field.error.invalid_type"
            }
        }

        test("returns the value that is set to it") {
            val dateTime = Timestamp("0006-08-17 23:50:51.974")

            runEdt { field.value = dateTime }

            field.value shouldBe dateTime
        }
    }


    context("formatter") {
        test("stores valid timestamps") {
            runEdt {
                field.text = "9017-07-12 05:22:05.767"
                field.commitEdit()
            }

            field.value.value shouldBe "9017-07-12 05:22:05.767"
        }

        test("stores liberally interpreted timestamps") {
            runEdt {
                field.text = "4494-09"
                field.commitEdit()
            }

            field.value.value shouldBe "4494-09-01 00:00:00.000"
        }

        test("stores values that cannot be interpreted as timestamps") {
            runEdt {
                field.text = "How are you?"
                field.commitEdit()
            }

            field.value.value shouldBe "How are you?"
        }

        test("interprets `null` as an empty string") {
            runEdt {
                field.text = null
                field.commitEdit()
            }

            field.value.value shouldBe ""
        }
    }
})

/**
 * Unit tests for [bindDateTimes].
 */
object BindDateTimesTest : FunSpec({
    useEdtViolationDetection()


    context("updating") {
        test("updates the minimum date-time if the maximum goes below its value") {
            val min = runEdt { JDateTimeField(Timestamp("1970-01-01 00:00:00.000")) }
            val max = runEdt { JDateTimeField(Timestamp("1970-01-01 00:00:00.000")) }
            bindDateTimes(min, max)

            runEdt { max.value = Timestamp("0760-06-24 09:38:00.747") }

            min.value shouldBe Timestamp("0760-06-24 09:38:00.747")
        }

        test("updates the maximum date-time if the minimum goes above its value") {
            val min = runEdt { JDateTimeField(Timestamp("1970-01-01 00:00:00.000")) }
            val max = runEdt { JDateTimeField(Timestamp("1970-01-01 00:00:00.000")) }
            bindDateTimes(min, max)

            runEdt { min.value = Timestamp("8660-05-28 06:11:32.199") }

            max.value shouldBe Timestamp("8660-05-28 06:11:32.199")
        }
    }
})
