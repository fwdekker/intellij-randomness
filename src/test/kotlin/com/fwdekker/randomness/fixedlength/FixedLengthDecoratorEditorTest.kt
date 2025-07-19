package com.fwdekker.randomness.fixedlength

import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.afterNonContainer
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import com.fwdekker.randomness.testhelpers.editorApplyTests
import com.fwdekker.randomness.testhelpers.editorFieldsTests
import com.fwdekker.randomness.testhelpers.isSelectedProp
import com.fwdekker.randomness.testhelpers.prop
import com.fwdekker.randomness.testhelpers.runEdt
import com.fwdekker.randomness.testhelpers.textProp
import com.fwdekker.randomness.testhelpers.useEdtViolationDetection
import com.fwdekker.randomness.testhelpers.valueProp
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.row
import org.assertj.swing.fixture.Containers
import org.assertj.swing.fixture.FrameFixture


/**
 * Unit tests for [FixedLengthDecoratorEditor].
 */
object FixedLengthDecoratorEditorTest : FunSpec({
    tags(Tags.EDITOR)


    lateinit var frame: FrameFixture

    lateinit var scheme: FixedLengthDecorator
    lateinit var editor: FixedLengthDecoratorEditor


    useEdtViolationDetection()

    beforeNonContainer {
        scheme = FixedLengthDecorator(enabled = true)
        editor = runEdt { FixedLengthDecoratorEditor(scheme) }
        frame = Containers.showInFrame(editor.rootComponent)
    }

    afterNonContainer {
        frame.cleanUp()
    }


    context("input handling") {
        context("fixedLengthEnabled") {
            test("disables inputs if deselected") {
                runEdt { frame.checkBox("fixedLengthEnabled").target().isSelected = false }

                frame.spinner("fixedLengthLength").requireDisabled()
                frame.textBox("fixedLengthFiller").requireDisabled()
            }

            test("enables inputs if (re)selected") {
                runEdt { frame.checkBox("fixedLengthEnabled").target().isSelected = false }
                runEdt { frame.checkBox("fixedLengthEnabled").target().isSelected = true }

                frame.spinner("fixedLengthLength").requireEnabled()
                frame.textBox("fixedLengthFiller").requireEnabled()
            }
        }

        context("filler") {
            test("enforces the filler's length filter") {
                runEdt { frame.textBox("fixedLengthFiller").target().text = "zAt" }

                frame.textBox("fixedLengthFiller").requireText("t")
            }
        }
    }


    include(editorApplyTests { editor })

    include(
        editorFieldsTests(
            { editor },
            mapOf(
                "enabled" to {
                    row(frame.checkBox("fixedLengthEnabled").isSelectedProp(), editor.scheme::enabled.prop(), false)
                },
                "length" to {
                    row(frame.spinner("fixedLengthLength").valueProp(), editor.scheme::length.prop(), 5L)
                },
                "filler" to {
                    row(frame.textBox("fixedLengthFiller").textProp(), editor.scheme::filler.prop(), ".")
                },
            )
        )
    )
})
