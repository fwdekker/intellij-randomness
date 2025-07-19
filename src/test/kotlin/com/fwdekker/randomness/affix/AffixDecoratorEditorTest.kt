package com.fwdekker.randomness.affix

import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.afterNonContainer
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import com.fwdekker.randomness.testhelpers.editorApplyTests
import com.fwdekker.randomness.testhelpers.editorFieldsTests
import com.fwdekker.randomness.testhelpers.prop
import com.fwdekker.randomness.testhelpers.requireEnabledIs
import com.fwdekker.randomness.testhelpers.runEdt
import com.fwdekker.randomness.testhelpers.textProp
import com.fwdekker.randomness.testhelpers.useEdtViolationDetection
import com.intellij.ui.layout.selected
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.row
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.assertj.swing.fixture.Containers
import org.assertj.swing.fixture.FrameFixture
import javax.swing.JCheckBox


/**
 * Unit tests for [AffixDecoratorEditor].
 */
object AffixDecoratorEditorTest : FunSpec({
    tags(Tags.EDITOR)


    lateinit var frame: FrameFixture

    lateinit var scheme: AffixDecorator
    lateinit var editor: AffixDecoratorEditor


    useEdtViolationDetection()

    beforeNonContainer {
        scheme = AffixDecorator(enabled = true)
        editor = runEdt { AffixDecoratorEditor(scheme, presets = listOf("a", "b", "c")) }
        frame = Containers.showInFrame(editor.rootComponent)
    }

    afterNonContainer {
        frame.cleanUp()
    }


    context("constructor") {
        context("enableMnemonic") {
            test("enables mnemonics if true") {
                frame.checkBox("affixEnabled").target().mnemonic shouldNotBe 0
            }

            test("disables mnemonics if false") {
                frame.cleanUp()
                editor = runEdt { AffixDecoratorEditor(scheme, presets = listOf("."), enableMnemonic = false) }
                frame = Containers.showInFrame(editor.rootComponent)

                frame.checkBox("affixEnabled").target().mnemonic shouldBe 0
            }
        }

        context("namePrefix") {
            test("does not prefix component names if the prefix is an empty string") {
                shouldNotThrowAny { frame.checkBox("affixEnabled") }
                shouldNotThrowAny { frame.comboBox("affixDescriptor") }
            }

            test("prefixes component names by the given prefix and retains camel case") {
                frame.cleanUp()
                editor = runEdt { AffixDecoratorEditor(scheme, presets = listOf("."), namePrefix = "prefix") }
                frame = Containers.showInFrame(editor.rootComponent)

                shouldNotThrowAny { frame.checkBox("prefixAffixEnabled") }
                shouldNotThrowAny { frame.comboBox("prefixAffixDescriptor") }
            }
        }
    }


    context("input handling") {
        context("ifEnabled and affixEnabled") {
            lateinit var toggle: JCheckBox


            beforeNonContainer {
                toggle = runEdt { JCheckBox().also { it.isSelected = false } }
            }


            @Suppress("BooleanLiteralArgument") // Argument names are clear from lambda later on
            withData(
                nameFn = { "enabledIf=${it.a}, checkboxChecked=${it.b}" },
                row(null, false, true, false),
                row(null, true, true, true),
                row(false, false, false, false),
                row(false, true, false, false),
                row(true, false, true, false),
                row(true, true, true, true),
            ) { (predicateState, checkboxState, expectedCheckboxEnabled, expectedDescriptorEnabled) ->
                val predicate = predicateState?.let { toggle.selected }

                frame.cleanUp()
                editor = runEdt { AffixDecoratorEditor(scheme, presets = listOf("."), enabledIf = predicate) }
                frame = Containers.showInFrame(editor.rootComponent)

                runEdt {
                    if (predicateState != null) toggle.isSelected = predicateState
                    frame.checkBox().target().isSelected = checkboxState
                }

                frame.checkBox().requireEnabledIs(expectedCheckboxEnabled)
                frame.comboBox().requireEnabledIs(expectedDescriptorEnabled)
            }
        }
    }


    include(editorApplyTests { editor })

    include(
        editorFieldsTests(
            { editor },
            mapOf(
                "descriptor" to {
                    row(
                        frame.comboBox("affixDescriptor").textProp(),
                        editor.scheme::descriptor.prop(),
                        "non-preset string",
                    )
                },
            )
        )
    )
})
