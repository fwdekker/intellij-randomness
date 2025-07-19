package com.fwdekker.randomness.uuid

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
 * Unit tests for [UuidSchemeEditor].
 */
object UuidSchemeEditorTest : FunSpec({
    tags(Tags.EDITOR)


    lateinit var frame: FrameFixture

    lateinit var scheme: UuidScheme
    lateinit var editor: UuidSchemeEditor


    useEdtViolationDetection()

    beforeNonContainer {
        scheme = UuidScheme()
        editor = runEdt { UuidSchemeEditor(scheme) }
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
