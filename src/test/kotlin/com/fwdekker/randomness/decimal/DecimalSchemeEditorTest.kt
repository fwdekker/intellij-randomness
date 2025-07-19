package com.fwdekker.randomness.decimal

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
import org.assertj.swing.fixture.Containers.showInFrame
import org.assertj.swing.fixture.FrameFixture


/**
 * Unit tests for [DecimalSchemeEditor].
 */
object DecimalSchemeEditorTest : FunSpec({
    tags(Tags.EDITOR)


    lateinit var frame: FrameFixture

    lateinit var scheme: DecimalScheme
    lateinit var editor: DecimalSchemeEditor


    useEdtViolationDetection()

    beforeNonContainer {
        scheme = DecimalScheme()
        editor = runEdt { DecimalSchemeEditor(scheme) }
        frame = showInFrame(editor.rootComponent)
    }

    afterNonContainer {
        frame.cleanUp()
    }



    context("input handling") {
        context("*Value") {
            test("binds the minimum and maximum values") {
                runEdt { frame.spinner("minValue").target().value = 3670.0 }

                frame.spinner("minValue").requireValue(3670.0)
                frame.spinner("maxValue").requireValue(3670.0)
            }
        }

        context("decimalCount") {
            test("truncates decimals in the decimal count") {
                runEdt { frame.spinner("decimalCount").target().value = 693.57f }

                frame.spinner("decimalCount").requireValue(693)
            }
        }

        context("groupingSeparator") {
            context("enforces the length filter") {
                beforeNonContainer {
                    runEdt { frame.checkBox("groupingSeparatorEnabled").target().isSelected = true }
                }


                test("enforces a minimum length of 1") {
                    runEdt { frame.comboBox("groupingSeparator").target().editor.item = "" }

                    frame.comboBox("groupingSeparator").requireSelection(",")
                }

                test("enforces a maximum length of 1") {
                    runEdt { frame.comboBox("groupingSeparator").target().editor.item = "long" }

                    frame.comboBox("groupingSeparator").requireSelection(",")
                }
            }

            context("enabled state") {
                test("enables the input if the checkbox is selected") {
                    runEdt { frame.checkBox("groupingSeparatorEnabled").target().isSelected = true }

                    frame.comboBox("groupingSeparator").requireEnabled()
                }

                test("disables the input if the checkbox is not selected") {
                    runEdt { frame.checkBox("groupingSeparatorEnabled").target().isSelected = false }

                    frame.comboBox("groupingSeparator").requireDisabled()
                }
            }
        }
    }


    include(editorApplyTests { editor })

    include(
        editorFieldsTests(
            { editor },
            mapOf(
                "minValue" to {
                    row(
                        frame.spinner("minValue").valueProp(),
                        editor.scheme::minValue.prop(),
                        189.0,
                    )
                },
                "maxValue" to {
                    row(
                        frame.spinner("maxValue").valueProp(),
                        editor.scheme::maxValue.prop(),
                        200.0,
                    )
                },
                "decimalCount" to {
                    row(
                        frame.spinner("decimalCount").valueProp(),
                        editor.scheme::decimalCount.prop(),
                        4L,
                    )
                },
                "showTrailingZeroes" to {
                    row(
                        frame.checkBox("showTrailingZeroes").isSelectedProp(),
                        editor.scheme::showTrailingZeroes.prop(),
                        true,
                    )
                },
                "decimalSeparator" to {
                    row(
                        frame.comboBox("decimalSeparator").textProp(),
                        editor.scheme::decimalSeparator.prop(),
                        "|",
                    )
                },
                "groupingSeparatorEnabled" to {
                    row(
                        frame.checkBox("groupingSeparatorEnabled").isSelectedProp(),
                        editor.scheme::groupingSeparatorEnabled.prop(),
                        true,
                    )
                },
                "groupingSeparator" to {
                    row(
                        frame.comboBox("groupingSeparator").textProp(),
                        editor.scheme::groupingSeparator.prop(),
                        "/",
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
