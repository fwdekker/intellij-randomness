package com.fwdekker.randomness.ui

import com.fwdekker.randomness.Timestamp
import com.fwdekker.randomness.testhelpers.DummySchemeEditor
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import com.fwdekker.randomness.testhelpers.ideaRunEdt
import com.fwdekker.randomness.testhelpers.useBareIdeaFixture
import com.fwdekker.randomness.testhelpers.useEdtViolationDetection
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.dsl.builder.panel
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.Row2
import io.kotest.data.row
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import javax.swing.JCheckBox
import javax.swing.JFormattedTextField
import javax.swing.JRadioButton
import javax.swing.JSpinner
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.JTree
import javax.swing.text.JTextComponent
import javax.swing.text.PlainDocument
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode


/**
 * Unit tests for extension functions in `ListenerHelpersKt`.
 */
object ListenerHelpersTest : FunSpec({
    var listenerInvoked = false
    val listener = { listenerInvoked = true }


    useEdtViolationDetection()
    useBareIdeaFixture()

    beforeNonContainer {
        listenerInvoked = false
    }


    context("addChangeListenerTo") {
        withData(
            @Suppress("UNCHECKED_CAST") // Unavoidable
            mapOf(
                "AbstractButton: JCheckBox: Change selection" to
                    row(
                        { JCheckBox() },
                        { (it as JCheckBox).isSelected = true },
                    ),
                "AbstractButton: JRadioButton: Change selection" to
                    row(
                        { JRadioButton() },
                        { (it as JRadioButton).isSelected = true },
                    ),
                "Document: Insert text" to
                    row(
                        { PlainDocument() },
                        { (it as PlainDocument).insertString(0, "text", null) },
                    ),
                "Document: Remove text" to
                    row(
                        { PlainDocument().also { it.insertString(0, "text", null) } },
                        { (it as PlainDocument).remove(1, 2) },
                    ),
                "Document: Replace text" to
                    row(
                        { PlainDocument().also { it.insertString(0, "text", null) } },
                        { (it as PlainDocument).replace(2, 1, "y", null) },
                    ),
                // [JBDocument] is excluded because setting up the correct fixtures is very difficult
                "JComboBox: Select different item" to
                    row(
                        { ComboBox(arrayOf("item1", "item2")) },
                        { (it as ComboBox<String>).item = "item1" },
                    ),
                "JComboBox: While typing" to
                    row(
                        { ComboBox(arrayOf("item1", "item2")).also { it.isEditable = true } },
                        { ((it as ComboBox<String>).editor.editorComponent as JTextComponent).text = "ite" },
                    ),
                "JSpinner: Change value" to
                    row(
                        { JSpinner() },
                        { (it as JSpinner).value = 5 },
                    ),
                "JTextComponent: JTextArea" to
                    row(
                        { JTextArea() },
                        { (it as JTextArea).text = "text" },
                    ),
                "JTextComponent: JDateTimeField" to
                    row(
                        { JDateTimeField() },
                        { (it as JDateTimeField).value = Timestamp("2862-02-14 02:27:11.154") },
                    ),
                "JTextComponent: JTextField" to
                    row(
                        { JTextField() },
                        { (it as JTextField).text = "text" },
                    ),
                "JTextComponent: Insert text" to
                    row(
                        { JTextField() },
                        { (it as JTextField).document.insertString(0, "text", null) },
                    ),
                "JTextComponent: Remove text" to
                    row(
                        { JTextField("text") },
                        { (it as JTextField).document.remove(2, 2) },
                    ),
                "JTree: Change selection" to
                    row(
                        { JTree() },
                        { (it as JTree).setSelectionRow(2) },
                    ),
                "JTree: Add node" to
                    row(
                        { JTree() },
                        { (it as JTree).model().insertNodeInto(DefaultMutableTreeNode(), it.model().root(), 0) },
                    ),
                "JTree: Remove node" to
                    row(
                        { JTree() },
                        { (it as JTree).model().removeNodeFromParent(it.model().root().firstChild as MutableTreeNode) },
                    ),
                "SchemeEditor: Recursion" to
                    row(
                        { DummySchemeEditor { panel { row { textField().withName("text") } } } },
                        { ((it as DummySchemeEditor).components.first() as JTextField).text = "text" },
                    ),
            )
        ) { (createComponent, changeComponent): Row2<() -> Any, (Any) -> Unit> ->
            val component = ideaRunEdt { createComponent() }
            addChangeListenerTo(component, listener = listener)

            listenerInvoked shouldBe false
            ideaRunEdt { changeComponent(component) }

            listenerInvoked shouldBe true
        }
    }


    test("invokes the listener when the text of a JFormattedTextField is committed") {
        // Committing the input may result in the `AbstractFormatter` changing the `value` of the field, even though
        // the `text` field is not changed. Therefore, this event should be listened to separately.

        val component = ideaRunEdt { JFormattedTextField("old text") }
        ideaRunEdt { component.text = "uncommitted text" }
        addChangeListenerTo(component, listener = listener)

        listenerInvoked shouldBe false
        ideaRunEdt { component.commitEdit() }

        listenerInvoked shouldBe true
    }
})


/**
 * Returns the [model] of this [JTree], cast to a [DefaultTreeModel].
 */
private fun JTree.model() = this.model as DefaultTreeModel

/**
 * Returns the [root] of this [DefaultTreeModel], cast to a [DefaultMutableTreeNode].
 */
private fun DefaultTreeModel.root() = this.root as DefaultMutableTreeNode
