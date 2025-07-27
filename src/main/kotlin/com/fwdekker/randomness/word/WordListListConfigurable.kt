package com.fwdekker.randomness.word

import com.intellij.openapi.Disposable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.util.Disposer
import javax.swing.JComponent


internal class WordListListConfigurable : Configurable, Disposable {
    @Suppress("detekt:LateinitUsage") // Initialized in [createComponent]
    lateinit var editor: WordListListEditor private set


    override fun getDisplayName() = "Randomness: Word Lists"

    override fun createComponent(): JComponent? {
        editor = WordListListEditor().also { Disposer.register(this, it) }
        return editor.rootComponent
    }

    override fun isModified(): Boolean = editor.isModified() || editor.doValidate() != null

    override fun reset() = editor.reset()

    override fun apply() = editor.apply()


    /**
     * Recursively disposes this configurable's resources.
     */
    override fun disposeUIResources() = Disposer.dispose(this)

    /**
     * Non-recursively disposes this configurable's resources.
     */
    override fun dispose() = Unit
}
