package com.fwdekker.randomness.template

import com.fwdekker.randomness.Scheme
import com.fwdekker.randomness.Settings
import com.fwdekker.randomness.datetime.DateTimeScheme
import com.fwdekker.randomness.decimal.DecimalScheme
import com.fwdekker.randomness.integer.IntegerScheme
import com.fwdekker.randomness.setAll
import com.fwdekker.randomness.string.StringScheme
import com.fwdekker.randomness.testhelpers.DummyScheme
import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.afterNonContainer
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import com.fwdekker.randomness.testhelpers.ideaRunEdt
import com.fwdekker.randomness.testhelpers.shouldContainExactly
import com.fwdekker.randomness.testhelpers.shouldMatchBundle
import com.fwdekker.randomness.testhelpers.useBareIdeaFixture
import com.fwdekker.randomness.testhelpers.useEdtViolationDetection
import com.fwdekker.randomness.uid.UidScheme
import com.fwdekker.randomness.word.WordScheme
import com.intellij.openapi.util.Disposer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.Row2
import io.kotest.data.row
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.assertj.swing.fixture.AbstractComponentFixture
import org.assertj.swing.fixture.Containers
import org.assertj.swing.fixture.FrameFixture


/**
 * Unit tests for [TemplateListEditor].
 */
object TemplateListEditorTest : FunSpec({
    tags(Tags.EDITOR)


    lateinit var frame: FrameFixture

    lateinit var templateList: TemplateList
    lateinit var editor: TemplateListEditor


    useEdtViolationDetection()
    useBareIdeaFixture()

    beforeSpec {
        TemplateListEditor.useTestSplitter = true
    }

    afterSpec {
        TemplateListEditor.useTestSplitter = false
    }

    beforeNonContainer {
        templateList =
            TemplateList(
                mutableListOf(
                    Template("Template1", mutableListOf(IntegerScheme(), StringScheme())),
                    Template("Template2", mutableListOf(DecimalScheme(), WordScheme())),
                )
            )
        templateList.applyContext(Settings(templateList = templateList))

        editor = ideaRunEdt { TemplateListEditor(templateList) }
        frame = Containers.showInFrame(editor.rootComponent)
    }

    afterNonContainer {
        frame.cleanUp()
        ideaRunEdt { Disposer.dispose(editor) }
    }


    context("initialSelection") {
        withData(
            mapOf<String, Row2<() -> String?, Int>>(
                "selects the first template if set to `null`" to
                    row({ null }, 0),
                "selects the first template if set to an invalid UUID" to
                    row({ "invalid" }, 0),
                "selects the template with the given UUID" to
                    row({ templateList.templates[1].uuid }, 3),
                "selects the scheme with the given UUID" to
                    row({ templateList.templates[1].schemes[0].uuid }, 4),
            )
        ) { (uuid, expectedSelection) ->
            frame.cleanUp()
            ideaRunEdt { Disposer.dispose(editor) }

            editor = ideaRunEdt { TemplateListEditor(templateList, initialSelection = uuid()) }
            frame = Containers.showInFrame(editor.rootComponent)

            ideaRunEdt { frame.tree().target().selectionRows!! } shouldContainExactly arrayOf(expectedSelection)
        }
    }

    context("editor creation") {
        context("loads the appropriate editor") {
            withData(
                mapOf(
                    "integer" to row(IntegerScheme()) { it.spinner("minValue") },
                    "decimal" to row(DecimalScheme()) { it.spinner("minValue") },
                    "string" to row(StringScheme()) { it.textBox("pattern") },
                    "uid" to row(UidScheme()) { it.comboBox("idType") },
                    "word" to row(WordScheme()) { it.comboBox("presets") },
                    "date-time" to row(DateTimeScheme()) { it.textBox("minDateTime") },
                    "template reference" to row(TemplateReference()) { it.comboBox("template") },
                )
            ) { (scheme, matcher): Row2<Scheme, (FrameFixture) -> AbstractComponentFixture<*, *, *>> ->
                templateList.templates.setAll(listOf(Template(schemes = mutableListOf(scheme))))
                templateList.applyContext(templateList.context)

                ideaRunEdt {
                    editor.reset()
                    frame.tree().target().setSelectionRow(1)
                }

                matcher(frame).requireVisible()
            }
        }

        test("loads an editor for templates") {
            templateList.templates.setAll(listOf(Template(schemes = mutableListOf())))
            templateList.applyContext(templateList.context)

            ideaRunEdt { editor.reset() }

            frame.textBox("templateName").requireVisible()
        }

        test("throws an error for unknown scheme types") {
            templateList.templates.setAll(listOf(Template(schemes = mutableListOf(DummyScheme()))))
            templateList.applyContext(templateList.context)

            ideaRunEdt { editor.reset() }

            shouldThrow<IllegalStateException> { ideaRunEdt { frame.tree().target().setSelectionRow(1) } }
                .message shouldMatchBundle "template_list.error.unknown_scheme_type"
        }
    }


    context("doValidate") {
        test("returns `null` for the default list") {
            ideaRunEdt { editor.doValidate() } shouldBe null
        }

        test("returns `null` if the template list is valid") {
            templateList.templates.setAll(listOf(Template(schemes = mutableListOf(DummyScheme()))))
            templateList.applyContext(templateList.context)
            ideaRunEdt { editor.reset() }

            ideaRunEdt { editor.doValidate() } shouldBe null
        }

        test("returns a string if the template list is invalid") {
            templateList.templates.setAll(listOf(Template(schemes = mutableListOf(DummyScheme(valid = false)))))
            templateList.applyContext(templateList.context)
            ideaRunEdt { editor.reset() }

            ideaRunEdt { editor.doValidate() } shouldNotBe null
        }
    }

    context("isModified") {
        test("returns `false` if no modifications have been made") {
            ideaRunEdt { editor.isModified() } shouldBe false
        }

        test("returns `true` if modifications have been made") {
            ideaRunEdt {
                frame.tree().target().selectionRows = intArrayOf(1)
                frame.spinner("minValue").target().value = 1
            }

            ideaRunEdt { editor.isModified() } shouldBe true
        }

        test("returns `false` if modifications have been reset") {
            ideaRunEdt {
                frame.tree().target().selectionRows = intArrayOf(1)
                frame.spinner("minValue").target().value = 1
            }

            ideaRunEdt { editor.reset() }

            ideaRunEdt { editor.isModified() } shouldBe false
        }
    }

    context("apply") {
        test("applies changes to the original list") {
            ideaRunEdt {
                frame.tree().target().selectionRows = intArrayOf(1)
                frame.spinner("minValue").target().value = 3
            }

            (templateList.templates[0].schemes[0] as IntegerScheme).minValue shouldNotBe 3
            ideaRunEdt { editor.apply() }

            (templateList.templates[0].schemes[0] as IntegerScheme).minValue shouldBe 3
        }

        test("does not couple the applied state to the editor's internal state") {
            ideaRunEdt {
                frame.tree().target().selectionRows = intArrayOf(2)
                frame.textBox("pattern").target().text = "old"

                ideaRunEdt { editor.apply() }

                frame.tree().target().selectionRows = intArrayOf(2)
                frame.textBox("pattern").target().text = "new"
            }

            (templateList.templates[0].schemes[1] as StringScheme).pattern shouldBe "old"
        }
    }

    context("reset") {
        test("undoes changes to the current selection") {
            ideaRunEdt {
                frame.tree().target().setSelectionRow(1)
                frame.spinner("minValue").target().value = 7L
            }

            ideaRunEdt { editor.reset() }

            frame.spinner("minValue").target().value shouldBe 0L
        }

        test("undoes changes to another selection") {
            ideaRunEdt {
                frame.tree().target().setSelectionRow(1)
                frame.spinner("minValue").target().value = 7L
            }

            ideaRunEdt {
                frame.tree().target().setSelectionRow(3)
                editor.reset()
            }

            ideaRunEdt { frame.tree().target().setSelectionRow(1) }
            ideaRunEdt { frame.spinner("minValue").target().value } shouldBe 0L
        }
    }
})
