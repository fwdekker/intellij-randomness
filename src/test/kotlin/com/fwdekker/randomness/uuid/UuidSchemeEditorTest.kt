package com.fwdekker.randomness.uuid

import com.fwdekker.randomness.Timestamp
import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.afterNonContainer
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import com.fwdekker.randomness.testhelpers.editorApplyTests
import com.fwdekker.randomness.testhelpers.editorFieldsTests
import com.fwdekker.randomness.testhelpers.find
import com.fwdekker.randomness.testhelpers.isSelectedProp
import com.fwdekker.randomness.testhelpers.itemProp
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
 * Unit tests for [UuidSchemeEditor].
 */
object UuidSchemeEditorTest : FunSpec({
    tags(Tags.EDITOR)


    lateinit var frame: FrameFixture

    lateinit var scheme: UuidScheme
    lateinit var editor: UuidSchemeEditor


    useEdtViolationDetection()
    useBareIdeaFixture()

    beforeNonContainer {
        scheme = UuidScheme()
        editor = runEdt { UuidSchemeEditor(scheme) }
        frame = showInFrame(editor.rootComponent)
    }

    afterNonContainer {
        frame.cleanUp()
    }


    context("input handling") {
        test("expands entered date-times") {
            val min = runEdt { frame.find(matcher(JDateTimeField::class.java, matcher = { it.name == "minDateTime" })) }

            runEdt {
                min.text = "1956"
                min.commitEdit()
            }

            runEdt { min.value.value } shouldBe "1956-01-01 00:00:00.000"
        }

        test("binds the minimum and maximum times") {
            runEdt { frame.textBox("minDateTime").timestampProp().set(Timestamp("5970")) }

            runEdt { frame.textBox("maxDateTime").timestampProp().set(Timestamp("4972")) }

            runEdt { frame.textBox("minDateTime").timestampProp().get() } shouldBe Timestamp("4972")
            runEdt { frame.textBox("maxDateTime").timestampProp().get() } shouldBe Timestamp("4972")
        }
    }


    include(editorApplyTests { editor })

    include(
        editorFieldsTests(
            { editor },
            mapOf(
                "type" to {
                    row(
                        frame.comboBox("version").itemProp(),
                        editor.scheme::version.prop(),
                        8,
                    )
                },
                "isUppercase" to {
                    row(
                        frame.checkBox("isUppercase").isSelectedProp(),
                        editor.scheme::isUppercase.prop(),
                        true,
                    )
                },
                "addDashes" to {
                    row(
                        frame.checkBox("addDashes").isSelectedProp(),
                        editor.scheme::addDashes.prop(),
                        false,
                    )
                },
                "minDateTime" to {
                    row(
                        frame.textBox("minDateTime").timestampProp(),
                        editor.scheme::minDateTime.prop(),
                        Timestamp("0489-03-30 13:36:32"),
                    )
                },
                "maxDateTime" to {
                    row(
                        frame.textBox("maxDateTime").timestampProp(),
                        editor.scheme::maxDateTime.prop(),
                        Timestamp("1656-11-05 20:58:41"),
                    )
                },
                "affixDecorator" to {
                    row(
                        frame.comboBox("affixDescriptor").textProp(),
                        editor.scheme.affixDecorator::descriptor.prop(),
                        "[@]",
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
