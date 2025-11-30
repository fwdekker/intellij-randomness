package com.fwdekker.randomness.word

import com.fwdekker.randomness.setAll
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.util.Disposer
import javax.swing.JComponent


class WordListEditor(private val wordList: WordList) : Disposable {
    private val editor: Editor
    val component: JComponent get() = editor.component
    private val document: Document

    // TODO: Support newlines? Support comments?
    private var words: List<String>
        get() = document.text.split("\n")
        set(value) = runWriteAction { document.setText(value.joinToString("\n")) }


    init {
        val factory = EditorFactory.getInstance()
        document = factory.createDocument("Placeholder text")
        editor = factory.createEditor(document)
        Disposer.register(this) { EditorFactory.getInstance().releaseEditor(editor) }

        words = wordList.words
    }

    override fun dispose() = Unit


    fun isModified(): Boolean {
        return wordList.words != words
    }

    fun apply() {
        wordList.words.setAll(words)
    }

    fun reset() {
        words = wordList.words
    }
}
