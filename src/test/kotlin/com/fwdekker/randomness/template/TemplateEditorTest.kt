package com.fwdekker.randomness.template

import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.afterNonContainer
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import com.fwdekker.randomness.testhelpers.editorFieldsTests
import com.fwdekker.randomness.testhelpers.prop
import com.fwdekker.randomness.testhelpers.runEdt
import com.fwdekker.randomness.testhelpers.textProp
import com.fwdekker.randomness.testhelpers.useEdtViolationDetection
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import org.assertj.swing.fixture.Containers
import org.assertj.swing.fixture.FrameFixture


/**
 * Unit tests for [TemplateEditor].
 */
object TemplateEditorTest : FunSpec({
    tags(Tags.EDITOR)


    lateinit var frame: FrameFixture

    lateinit var template: Template
    lateinit var editor: TemplateEditor


    useEdtViolationDetection()

    beforeNonContainer {
        template = Template()
        editor = runEdt { TemplateEditor(template) }
        frame = Containers.showInFrame(editor.rootComponent)
    }

    afterNonContainer {
        frame.cleanUp()
    }


    test("'apply' makes no changes by default") {
        val before = editor.scheme.deepCopy(retainUuid = true)

        editor.apply()

        before shouldBe editor.scheme
    }

    include(
        editorFieldsTests(
            { editor },
            mapOf("name" to { row(frame.textBox("templateName").textProp(), editor.scheme::name.prop(), "New Name") })
        )
    )
})
