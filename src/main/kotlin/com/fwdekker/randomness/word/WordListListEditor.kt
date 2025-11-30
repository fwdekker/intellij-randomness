package com.fwdekker.randomness.word

import com.fwdekker.randomness.Settings
import com.fwdekker.randomness.setAll
import com.fwdekker.randomness.ui.ValidationInfo
import com.intellij.openapi.Disposable
import java.awt.BorderLayout
import javax.swing.JPanel


class WordListListEditor(val originalWordListList: WordListList = Settings.DEFAULT.wordListList) : Disposable {
    private val currentWordListList = WordListList() // Synced with [originalWordListList] in [reset]

    val rootComponent = JPanel(BorderLayout())
    private val tree = WordListListTree(currentWordListList)
    private var editor: WordListEditor? = null


    init {
        rootComponent.add(tree.component, BorderLayout.WEST)

        val editorContainer = JPanel(BorderLayout())
        rootComponent.add(editorContainer, BorderLayout.CENTER)

        tree.selectionModel.addListSelectionListener { event ->
            if (event.valueIsAdjusting) return@addListSelectionListener

            val selectedIdx = tree.selectionModel.selectedIndices.singleOrNull() ?: return@addListSelectionListener
            val selected = tree.dataModel.getElementAt(selectedIdx)

            editor?.apply()
            editor = WordListEditor(selected).also {
                editorContainer.removeAll()
                editorContainer.add(it.component, BorderLayout.CENTER)
            }
        }

        reset()
    }


    fun doValidate(): ValidationInfo? = currentWordListList.doValidate()

    fun isModified(): Boolean = originalWordListList != currentWordListList || editor?.isModified() ?: false

    fun apply() {
        editor?.apply()

        originalWordListList.wordLists.setAll(currentWordListList.deepCopy(retainUuid = true).wordLists)
        originalWordListList.applyContext((+originalWordListList.context).copy(wordListList = originalWordListList))
    }

    fun reset() {
        currentWordListList.wordLists.setAll(originalWordListList.deepCopy(retainUuid = true).wordLists)
        currentWordListList.applyContext((+currentWordListList.context).copy(wordListList = currentWordListList))

        tree.dataModel.allContentsChanged()
        editor?.reset()
    }

    /**
     * Disposes this editor's resources.
     */
    override fun dispose() = Unit
}
