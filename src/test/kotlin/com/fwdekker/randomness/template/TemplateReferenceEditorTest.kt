package com.fwdekker.randomness.template

import com.fwdekker.randomness.CapitalizationMode
import com.fwdekker.randomness.Settings
import com.fwdekker.randomness.testhelpers.DummyScheme
import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.afterNonContainer
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import com.fwdekker.randomness.testhelpers.editorFieldsTests
import com.fwdekker.randomness.testhelpers.itemProp
import com.fwdekker.randomness.testhelpers.prop
import com.fwdekker.randomness.testhelpers.runEdt
import com.fwdekker.randomness.testhelpers.textProp
import com.fwdekker.randomness.testhelpers.useEdtViolationDetection
import com.fwdekker.randomness.testhelpers.valueProp
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.row
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.assertj.swing.fixture.Containers
import org.assertj.swing.fixture.FrameFixture


/**
 * Unit tests for [TemplateReferenceEditor].
 */
object TemplateReferenceEditorTest : FunSpec({
    tags(Tags.EDITOR)


    lateinit var frame: FrameFixture

    lateinit var context: Settings
    lateinit var reference: TemplateReference
    lateinit var editor: TemplateReferenceEditor


    useEdtViolationDetection()

    beforeNonContainer {
        context = Settings(
            templateList = TemplateList(
                mutableListOf(
                    Template("Template0", mutableListOf(DummyScheme())),
                    Template("Template1", mutableListOf(TemplateReference())),
                    Template("Template2", mutableListOf(DummyScheme())),
                )
            ),
        )

        reference = context.templates[1].schemes[0] as TemplateReference
        reference.applyContext(context)
        reference.templateUuid = context.templates[0].uuid

        editor = runEdt { TemplateReferenceEditor(reference) }
        frame = Containers.showInFrame(editor.rootComponent)
    }

    afterNonContainer {
        frame.cleanUp()
    }


    context("reset") {
        test("selects nothing if the reference refers to null") {
            reference.template = null
            runEdt { editor.reset() }

            runEdt { frame.comboBox("template").itemProp().get() } shouldBe null
        }

        test("does not load the reference's parent as a selectable option") {
            val box = frame.comboBox("template").target()
            val items = (0 until box.itemCount).map { box.getItemAt(it) as Template }

            items shouldNotContain reference.parent
        }
    }

    context("apply") {
        test("makes no changes by default") {
            val before = editor.scheme.deepCopy(retainUuid = true)

            editor.apply()

            before shouldBe editor.scheme
        }
    }

    include(
        editorFieldsTests(
            { editor },
            mapOf(
                "template" to {
                    row(
                        frame.comboBox("template").itemProp(),
                        editor.scheme::template.prop(),
                        context.templates[2],
                    )
                },
                "capitalization" to {
                    row(
                        frame.comboBox("capitalization").itemProp(),
                        editor.scheme::capitalization.prop(),
                        CapitalizationMode.LOWER,
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
