package com.fwdekker.randomness.word

import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.options.ex.Settings
import com.intellij.openapi.util.Disposer
import org.jetbrains.annotations.NonNls
import java.awt.Component
import javax.swing.JComponent
import kotlin.reflect.jvm.jvmName


internal class WordListListConfigurable : SearchableConfigurable, Disposable {
    @Suppress("detekt:LateinitUsage") // Initialized in [createComponent]
    lateinit var editor: WordListListEditor private set


    override fun getDisplayName() = "Randomness: Word Lists"

    override fun createComponent(): JComponent {
        if (::editor.isInitialized) return editor.rootComponent

        editor = WordListListEditor().also { Disposer.register(this, it) }
        return editor.rootComponent
    }

    override fun isModified(): Boolean = editor.isModified() || editor.doValidate() != null

    override fun reset() = editor.reset()

    override fun apply() = editor.apply()


    override fun getId(): @NonNls String = this::class.jvmName

    override fun enableSearch(option: String?): Runnable? = option?.let {
        Runnable {
            createComponent()
            editor.selectWordList(it)
        }
    }


    /**
     * Recursively disposes this configurable's resources.
     */
    override fun disposeUIResources() = Disposer.dispose(this)

    /**
     * Non-recursively disposes this configurable's resources.
     */
    override fun dispose() = Unit
}


/**
 * Selects [wordList] in the currently open [WordListListConfigurable], or does nothing if this is not possible for some
 * reason.
 */
fun selectWordListInSettings(context: Component, wordList: WordList?) {
    wordList ?: return

    val id = "com.fwdekker.randomness.word.WordListListConfigurable"
    val settings = Settings.KEY.getData(DataManager.getInstance().getDataContext(context)) ?: return
    val configurable = settings.find(id) as WordListListConfigurable
    settings.select(configurable, wordList.uuid)
}
