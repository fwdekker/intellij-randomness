package com.fwdekker.randomness.integer

import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.afterNonContainer
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import com.fwdekker.randomness.testhelpers.editorApplyTests
import com.fwdekker.randomness.testhelpers.editorFieldsTests
import com.fwdekker.randomness.testhelpers.isSelectedProp
import com.fwdekker.randomness.testhelpers.prop
import com.fwdekker.randomness.testhelpers.requireEnabledIs
import com.fwdekker.randomness.testhelpers.runEdt
import com.fwdekker.randomness.testhelpers.textProp
import com.fwdekker.randomness.testhelpers.useEdtViolationDetection
import com.fwdekker.randomness.testhelpers.valueProp
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.row
import io.kotest.datatest.withData
import org.assertj.swing.fixture.Containers.showInFrame
import org.assertj.swing.fixture.FrameFixture


/**
 * Unit tests for [IntegerSchemeEditor].
 */
object IntegerSchemeEditorTest : FunSpec({
    tags(Tags.EDITOR)


    lateinit var frame: FrameFixture

    lateinit var scheme: IntegerScheme
    lateinit var editor: IntegerSchemeEditor


    useEdtViolationDetection()

    beforeNonContainer {
        scheme = IntegerScheme()
        editor = runEdt { IntegerSchemeEditor(scheme) }
        frame = showInFrame(editor.rootComponent)
    }

    afterNonContainer {
        frame.cleanUp()
    }



    context("input handling") {
        context("*Value") {
            test("truncates decimals in the minimum value") {
                runEdt { frame.spinner("minValue").target().value = 285.21f }

                frame.spinner("minValue").requireValue(285L)
            }

            test("truncates decimals in the maximum value") {
                runEdt { frame.spinner("maxValue").target().value = 490.34f }

                frame.spinner("maxValue").requireValue(490L)
            }

            test("binds the minimum and maximum values") {
                runEdt { frame.spinner("minValue").target().value = 6804L }

                frame.spinner("minValue").requireValue(6804L)
                frame.spinner("maxValue").requireValue(6804L)
            }
        }

        context("base") {
            test("truncates decimals in the base") {
                runEdt { frame.spinner("base").target().value = 22.62f }

                frame.spinner("base").requireValue(22)
            }
        }

        context("grouping separator") {
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

            context("toggles the grouping separator depending on base value and checkbox state") {
                @Suppress("BooleanLiteralArgument") // Argument names are clear from lambda later on
                withData(
                    nameFn = { "base=${it.a}, checkBoxChecked=${it.b}" },
                    row(8, false, false, false),
                    row(8, true, false, false),
                    row(10, false, true, false),
                    row(10, true, true, true),
                    row(12, false, false, false),
                    row(12, true, false, false),
                ) { (base, checkBoxChecked, expectedCheckBoxEnabled, expectedInputEnabled) ->
                    runEdt {
                        frame.spinner("base").target().value = base
                        frame.checkBox("groupingSeparatorEnabled").target().isSelected = checkBoxChecked
                    }

                    frame.checkBox("groupingSeparatorEnabled").requireEnabledIs(expectedCheckBoxEnabled)
                    frame.comboBox("groupingSeparator").requireEnabledIs(expectedInputEnabled)
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
                        150L,
                    )
                },
                "maxValue" to {
                    row(
                        frame.spinner("maxValue").valueProp(),
                        editor.scheme::maxValue.prop(),
                        578L,
                    )
                },
                "base" to {
                    row(
                        frame.spinner("base").valueProp(),
                        editor.scheme::base.prop(),
                        14,
                    )
                },
                "isUppercase" to {
                    row(
                        frame.checkBox("isUppercase").isSelectedProp(),
                        editor.scheme::isUppercase.prop(),
                        true,
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
                        "!",
                    )
                },
                "affixDecorator" to {
                    row(
                        frame.comboBox("affixDescriptor").textProp(),
                        editor.scheme.affixDecorator::descriptor.prop(),
                        "[@]",
                    )
                },
                "fixedLengthDecorator" to {
                    row(
                        frame.spinner("fixedLengthLength").valueProp(),
                        editor.scheme.fixedLengthDecorator::length.prop(),
                        9,
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
