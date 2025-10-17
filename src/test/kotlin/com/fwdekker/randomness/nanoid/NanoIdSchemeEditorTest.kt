package com.fwdekker.randomness.nanoid

import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.afterNonContainer
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import com.fwdekker.randomness.testhelpers.editorApplyTests
import com.fwdekker.randomness.testhelpers.editorFieldsTests
import com.fwdekker.randomness.testhelpers.prop
import com.fwdekker.randomness.testhelpers.runEdt
import com.fwdekker.randomness.testhelpers.textProp
import com.fwdekker.randomness.testhelpers.useBareIdeaFixture
import com.fwdekker.randomness.testhelpers.useEdtViolationDetection
import com.fwdekker.randomness.testhelpers.valueProp
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.row
import org.assertj.swing.fixture.Containers.showInFrame
import org.assertj.swing.fixture.FrameFixture

/**
 * Unit tests for [NanoIdSchemeEditor].
 */
object NanoIdSchemeEditorTest : FunSpec({
    tags(Tags.EDITOR)

    lateinit var frame: FrameFixture

    lateinit var scheme: NanoIdScheme
    lateinit var editor: NanoIdSchemeEditor

    useEdtViolationDetection()
    useBareIdeaFixture()

    beforeNonContainer {
        scheme = NanoIdScheme()
        editor = runEdt { NanoIdSchemeEditor(scheme) }
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
                "size" to {
                    row(
                        frame.spinner("size").valueProp(),
                        editor.scheme::size.prop(),
                        37,
                    )
                },
                "alphabet" to {
                    row(
                        frame.textBox("alphabet").textProp(),
                        editor.scheme::alphabet.prop(),
                        "abc123",
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
