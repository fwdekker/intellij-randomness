package com.fwdekker.randomness

import com.fwdekker.randomness.testhelpers.DummyInsertAction
import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.afterNonContainer
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import com.fwdekker.randomness.testhelpers.ideaEdtTest
import com.fwdekker.randomness.testhelpers.ideaRunEdt
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.Document
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe


/**
 * Unit tests for [InsertAction].
 */
object InsertActionTest : FunSpec({
    tags(Tags.IDEA_FIXTURE)


    lateinit var myFixture: CodeInsightTestFixture
    lateinit var document: Document
    lateinit var caretModel: CaretModel

    lateinit var insertAction: DummyInsertAction


    beforeNonContainer {
        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        val builder = factory.createLightFixtureBuilder(null, "NewInsertActionTest").getFixture()

        myFixture = factory.createCodeInsightFixture(builder, factory.createTempDirTestFixture())
        myFixture.testDataPath = javaClass.classLoader.getResource("integration-project/")!!.path
        myFixture.setUp()

        ideaRunEdt {
            val file = myFixture.copyFileToProject("emptyFile.txt")
            myFixture.openFileInEditor(file)

            document = myFixture.editor.document
            caretModel = myFixture.editor.caretModel

            var insertValue = 0
            insertAction = DummyInsertAction { "${insertValue++}" }
        }
    }

    @Suppress("detekt:SwallowedException") // Intentional
    afterNonContainer {
        try {
            myFixture.tearDown()
        } catch (_: Error) {
            // Swallow errors about undisposed timers
        }
    }


    /**
     * Moves the primary caret to [offset] and returns the primary caret.
     */
    fun setCaret(offset: Int) =
        caretModel.primaryCaret.also { it.moveToOffset(offset) }

    /**
     * Adds a caret at [offset] and returns the caret.
     */
    fun addCaret(offset: Int) =
        caretModel.addCaret(myFixture.editor.offsetToVisualPosition(offset))!!

    /**
     * Selects the interval from [startOffset] (inclusive) to [endOffset] (exclusive) with the primary caret.
     */
    fun setSelection(startOffset: Int, endOffset: Int) =
        caretModel.primaryCaret.setSelection(startOffset, endOffset)

    /**
     * Adds a caret that selects from [startOffset] (inclusive) to [endOffset] (exclusive).
     */
    fun addSelection(startOffset: Int, endOffset: Int) =
        addCaret(startOffset).setSelection(startOffset, endOffset)



    context("actionPerformed") {
        ideaEdtTest("inserts text into an empty document") {
            myFixture.testAction(insertAction)

            document.text shouldBe "0"
        }

        ideaEdtTest("inserts text in front of existing text") {
            runWriteCommandAction(myFixture.project) { document.setText("contents") }

            setCaret(0)
            myFixture.testAction(insertAction)

            document.text shouldBe "0contents"
        }

        ideaEdtTest("inserts text behind existing text") {
            runWriteCommandAction(myFixture.project) { document.setText("contents") }

            setCaret(8)
            myFixture.testAction(insertAction)

            document.text shouldBe "contents0"
        }

        ideaEdtTest("inserts text in the middle of existing text") {
            runWriteCommandAction(myFixture.project) { document.setText("contents") }

            setCaret(3)
            myFixture.testAction(insertAction)

            document.text shouldBe "con0tents"
        }

        ideaEdtTest("replaces the entire document if selected") {
            runWriteCommandAction(myFixture.project) { document.setText("contents") }

            setSelection(0, 8)
            myFixture.testAction(insertAction)

            document.text shouldBe "0"
        }

        ideaEdtTest("replaces a partial selection of text") {
            runWriteCommandAction(myFixture.project) { document.setText("contents") }

            setSelection(2, 4)
            myFixture.testAction(insertAction)

            document.text shouldBe "co0ents"
        }

        ideaEdtTest("inserts text at multiple carets") {
            runWriteCommandAction(myFixture.project) { document.setText("line1\nline2\nline3") }

            setCaret(2)
            addCaret(7)
            addCaret(16)
            myFixture.testAction(insertAction)

            document.text shouldBe "li0ne1\nl1ine2\nline23"
        }

        ideaEdtTest("replaces text at multiple carets") {
            runWriteCommandAction(myFixture.project) { document.setText("line1\nline2\nline3") }

            setSelection(2, 7)
            addSelection(10, 15)
            myFixture.testAction(insertAction)

            document.text shouldBe "li0ine1e3"
        }

        ideaEdtTest("simultaneously inserts at carets and replaces at selections") {
            runWriteCommandAction(myFixture.project) { document.setText("line1\nline2\nline3") }

            setCaret(2)
            addSelection(4, 6)
            addCaret(8)
            addSelection(12, 15)
            myFixture.testAction(insertAction)

            document.text shouldBe "li0ne1li2ne2\n3e3"
        }

        ideaEdtTest("inserts the same value at multiple carets") {
            runWriteCommandAction(myFixture.project) { document.setText("line1\nline2\nline3") }

            setCaret(5)
            addCaret(10)
            addCaret(12)

            var insertValue = 0
            myFixture.testAction(DummyInsertAction(repeat = true) { "${insertValue++}" })

            document.text shouldBe "line10\nline02\n0line3"
        }


        ideaEdtTest("inserts nothing if the scheme is invalid") {
            myFixture.testAction(DummyInsertAction { throw DataGenerationException("Invalid input!") })

            document.text shouldBe ""
        }

        ideaEdtTest("inserts nothing if the scheme is invalid with an empty message") {
            myFixture.testAction(DummyInsertAction { throw DataGenerationException() })

            document.text shouldBe ""
        }

        ideaEdtTest("inserts nothing if the project is null") {
            val event = AnActionEvent.createFromDataContext("", null) {
                if (it == CommonDataKeys.PROJECT.name) myFixture.project
                else null
            }

            insertAction.actionPerformed(event)

            document.text shouldBe ""
        }

        ideaEdtTest("inserts nothing if the editor is null") {
            val event = AnActionEvent.createFromDataContext("", null) {
                if (it == CommonDataKeys.EDITOR.name) myFixture.editor
                else null
            }

            insertAction.actionPerformed(event)

            document.text shouldBe ""
        }
    }

    context("presentation") {
        ideaEdtTest("disables the presentation if the editor is null") {
            val presentation = Presentation()

            insertAction.update(AnActionEvent.createFromDataContext("", presentation) { null })

            presentation.isEnabled shouldBe false
        }

        ideaEdtTest("enables the presentation if the editor is not null") {
            val presentation = Presentation()

            insertAction.update(AnActionEvent.createFromDataContext("", presentation) { myFixture.editor })

            presentation.isEnabled shouldBe true
        }

        ideaEdtTest("disables the presentation if the editor is read-only") {
            val presentation = Presentation()

            myFixture.editor.document.setReadOnly(true)
            insertAction.update(AnActionEvent.createFromDataContext("", presentation) { myFixture.editor })

            presentation.isEnabled shouldBe false
        }
    }
})
