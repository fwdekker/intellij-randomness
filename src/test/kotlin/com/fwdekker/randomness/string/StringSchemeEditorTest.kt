package com.fwdekker.randomness.string

import com.fwdekker.randomness.CapitalizationMode
import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.afterNonContainer
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import com.fwdekker.randomness.testhelpers.editorApplyTests
import com.fwdekker.randomness.testhelpers.editorFieldsTests
import com.fwdekker.randomness.testhelpers.isSelectedProp
import com.fwdekker.randomness.testhelpers.itemProp
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
 * Unit tests for [StringSchemeEditor].
 */
object StringSchemeEditorTest : FunSpec({
    tags(Tags.EDITOR)


    lateinit var frame: FrameFixture

    lateinit var scheme: StringScheme
    lateinit var editor: StringSchemeEditor


    useEdtViolationDetection()

    beforeNonContainer {
        scheme = StringScheme()
        editor = runEdt { StringSchemeEditor(scheme) }
        frame = showInFrame(editor.rootComponent)
    }

    afterNonContainer {
        frame.cleanUp()
    }


    include(editorApplyTests { editor })

    include(
        editorFieldsTests(
            { editor },
            mapOf(
                "pattern" to {
                    row(
                        frame.textBox("pattern").textProp(),
                        editor.scheme::pattern.prop(),
                        "[a-z]{3,4}",
                    )
                },
                "isRegex" to {
                    row(
                        frame.checkBox("isRegex").isSelectedProp(),
                        editor.scheme::isRegex.prop(),
                        false,
                    )
                },
                "isInverseRegex" to {
                    row(
                        frame.checkBox("isNonMatching").isSelectedProp(),
                        editor.scheme::isNonMatching.prop(),
                        true,
                    )
                },
                "removeLookAlikeCharacters" to {
                    row(
                        frame.checkBox("removeLookAlikeCharacters").isSelectedProp(),
                        editor.scheme::removeLookAlikeSymbols.prop(),
                        true,
                    )
                },
                "capitalization" to {
                    row(
                        frame.comboBox("capitalization").itemProp(),
                        editor.scheme::capitalization.prop(),
                        CapitalizationMode.RANDOM,
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
