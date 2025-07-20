package com.fwdekker.randomness.datetime

import com.fwdekker.randomness.Timestamp
import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.afterNonContainer
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import com.fwdekker.randomness.testhelpers.editorApplyTests
import com.fwdekker.randomness.testhelpers.editorFieldsTests
import com.fwdekker.randomness.testhelpers.find
import com.fwdekker.randomness.testhelpers.matcher
import com.fwdekker.randomness.testhelpers.prop
import com.fwdekker.randomness.testhelpers.runEdt
import com.fwdekker.randomness.testhelpers.textProp
import com.fwdekker.randomness.testhelpers.timestampProp
import com.fwdekker.randomness.testhelpers.useBareIdeaFixture
import com.fwdekker.randomness.testhelpers.useEdtViolationDetection
import com.fwdekker.randomness.testhelpers.valueProp
import com.fwdekker.randomness.ui.JDateTimeField
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import org.assertj.swing.fixture.Containers.showInFrame
import org.assertj.swing.fixture.FrameFixture


/**
 * Unit tests for [DateTimeSchemeEditor].
 */
object DateTimeSchemeEditorTest : FunSpec({
    tags(Tags.EDITOR)


    lateinit var frame: FrameFixture
    lateinit var scheme: DateTimeScheme
    lateinit var editor: DateTimeSchemeEditor


    useEdtViolationDetection()
    useBareIdeaFixture()

    beforeNonContainer {
        scheme = DateTimeScheme()
        editor = runEdt { DateTimeSchemeEditor(scheme) }
        frame = showInFrame(editor.rootComponent)
    }

    afterNonContainer {
        frame.cleanUp()
    }


    context("input handling") {
        test("expands entered date-times") {
            val min = runEdt { frame.find(matcher(JDateTimeField::class.java, matcher = { it.name == "minDateTime" })) }

            runEdt {
                min.text = "0867"
                min.commitEdit()
            }

            runEdt { min.value.value } shouldBe "0867-01-01 00:00:00.000"
        }

        test("binds the minimum and maximum date-times") {
            runEdt { frame.textBox("minDateTime").timestampProp().set(Timestamp("1131")) }

            runEdt { frame.textBox("maxDateTime").timestampProp().set(Timestamp("0463")) }

            runEdt { frame.textBox("minDateTime").timestampProp().get() } shouldBe Timestamp("0463")
            runEdt { frame.textBox("maxDateTime").timestampProp().get() } shouldBe Timestamp("0463")
        }
    }


    include(editorApplyTests { editor })

    include(
        editorFieldsTests(
            { editor },
            mapOf(
                "minDateTime" to {
                    row(
                        frame.textBox("minDateTime").timestampProp(),
                        editor.scheme::minDateTime.prop(),
                        Timestamp("0379-09-20 17:27:35.767"),
                    )
                },
                "maxDateTime" to {
                    row(
                        frame.textBox("maxDateTime").timestampProp(),
                        editor.scheme::maxDateTime.prop(),
                        Timestamp("8457-03-25 04:00:37.075"),
                    )
                },
                "pattern" to {
                    row(
                        frame.textBox("pattern").textProp(),
                        editor.scheme::pattern.prop(),
                        "dd/MM/yyyy",
                    )
                },
                "arrayDecorator" to {
                    row(
                        frame.spinner("arrayMaxCount").valueProp(),
                        editor.scheme.arrayDecorator::maxCount.prop(),
                        7,
                    )
                },
            )
        )
    )
})
