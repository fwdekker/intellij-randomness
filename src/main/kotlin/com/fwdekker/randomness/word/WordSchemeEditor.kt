package com.fwdekker.randomness.word

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.SchemeEditor
import com.fwdekker.randomness.affix.AffixDecoratorEditor
import com.fwdekker.randomness.array.ArrayDecoratorEditor
import com.fwdekker.randomness.ui.UIConstants
import com.fwdekker.randomness.ui.addChangeListenerTo
import com.fwdekker.randomness.ui.onResetThis
import com.fwdekker.randomness.ui.withFixedHeight
import com.fwdekker.randomness.ui.withName
import com.fwdekker.randomness.word.WordScheme.Companion.PRESET_AFFIX_DECORATOR_DESCRIPTORS
import com.fwdekker.randomness.word.WordScheme.Companion.PRESET_CAPITALIZATION
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.impl.UndoManagerImpl
import com.intellij.openapi.command.undo.DocumentReferenceManager
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.Disposer
import com.intellij.ui.SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toMutableProperty
import com.intellij.ui.dsl.builder.toNullableProperty
import com.intellij.ui.dsl.listCellRenderer.listCellRenderer
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import java.awt.event.ItemEvent


/**
 * Component for editing a [WordScheme].
 *
 * @param scheme the scheme to edit
 */
class WordSchemeEditor(scheme: WordScheme = WordScheme()) : SchemeEditor<WordScheme>(scheme) {
    override val rootComponent = panel {
        group(Bundle("word.ui.words.header")) {
            lateinit var preset: ComboBox<DefaultWordList>
            lateinit var document: Document

            row(Bundle("word.ui.words.insert_option")) {
                comboBox(listOf(PRESET_ITEM) + DefaultWordList.WORD_LISTS, WORD_LIST_RENDERER)
                    .withName("presets")
                    .also {
                        it.component.addItemListener { event ->
                            if (event.stateChange == ItemEvent.SELECTED) {
                                val item = event.item as DefaultWordList
                                if (item != PRESET_ITEM) document.setWordList(item.words)
                            }
                        }
                    }
                    .onResetThis {
                        it.component.selectedIndex =
                            DefaultWordList.WORD_LISTS.map(DefaultWordList::words).indexOf(scheme.words) + 1
                    }
                    .also { preset = it.component }
            }

            row {
                val factory = EditorFactory.getInstance()

                document = factory.createDocument("")
                addChangeListenerTo(document) {
                    if (preset.selectedIndex != 0 && document.getWordList() != preset.item.words)
                        preset.selectedIndex = 0
                }
                extraComponents += document

                val wordListEditor = factory.createEditor(document)
                Disposer.register(this@WordSchemeEditor, factory.wrapDisposable(wordListEditor))
                cell(wordListEditor.component)
                    .onReset { document.resetUndoHistory() }
                    .align(AlignX.FILL)
                    .withFixedHeight(UIConstants.SIZE_VERY_LARGE)
                    .bind(
                        { document.getWordList() },
                        { _, list: List<String> -> document.setWordList(list) },
                        scheme::words.toMutableProperty()
                    )
                    .bindValidation(scheme::words)
            }
        }

        group(Bundle("word.ui.format.header")) {
            row(Bundle("word.ui.format.capitalization_option")) {
                comboBox(PRESET_CAPITALIZATION, textListCellRenderer { it?.toLocalizedString() })
                    .withName("capitalization")
                    .bindItem(scheme::capitalization.toNullableProperty())
                    .bindValidation(scheme::capitalization)
            }

            row {
                AffixDecoratorEditor(scheme.affixDecorator, PRESET_AFFIX_DECORATOR_DESCRIPTORS)
                    .also { decoratorEditors += it }
                    .let { cell(it.rootComponent) }
            }
        }

        row {
            ArrayDecoratorEditor(scheme.arrayDecorator)
                .also { decoratorEditors += it }
                .let { cell(it.rootComponent).align(AlignX.FILL) }
        }
    }.finalize(this)


    init {
        reset()
    }


    /**
     * Holds constants.
     */
    companion object {
        /**
         * The "header" item inserted at the top of the preset combobox.
         */
        val PRESET_ITEM get() = DefaultWordList(Bundle("word_list.ui.select_preset"), "")

        /**
         * Renders a [DefaultWordList] as a string inside a [javax.swing.JComboBox].
         */
        @Suppress("UnstableApiUsage") // Cannot be circumvented
        private val WORD_LIST_RENDERER =
            listCellRenderer<DefaultWordList?> {
                val index = index
                val name = value?.name

                text(name ?: Bundle("word_list.ui.nameless")) {
                    if (index == 0 || name == null)
                        attributes = REGULAR_ITALIC_ATTRIBUTES
                }
            }
    }
}


/**
 * Resets the undo history of this [Document].
 */
private fun Document.resetUndoHistory() {
    (UndoManager.getGlobalInstance() as UndoManagerImpl)
        .invalidateActionsFor(DocumentReferenceManager.getInstance().create(this))
}

/**
 * Returns the list of all non-blank lines in this [Document].
 */
fun Document.getWordList() = this.text.split('\n').filterNot { it.isBlank() }

/**
 * Puts each entry of [wordList] onto a separate line in this [Document].
 */
fun Document.setWordList(wordList: List<String>) =
    runWriteAction { setText(wordList.joinToString(separator = "\n", postfix = "\n")) }

/**
 * Returns a [Disposable] that can dispose the [editor] through the [EditorFactory] that created it.
 */
@Suppress("ObjectLiteralToLambda") // [Disposable] docs state that lambdas should be avoided
private fun EditorFactory.wrapDisposable(editor: Editor) =
    object : Disposable {
        override fun dispose() = this@wrapDisposable.releaseEditor(editor)
    }
