package com.fwdekker.randomness.word

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.SchemeEditor
import com.fwdekker.randomness.affix.AffixDecoratorEditor
import com.fwdekker.randomness.array.ArrayDecoratorEditor
import com.fwdekker.randomness.ui.onResetThis
import com.fwdekker.randomness.ui.withName
import com.fwdekker.randomness.word.WordScheme.Companion.PRESET_AFFIX_DECORATOR_DESCRIPTORS
import com.fwdekker.randomness.word.WordScheme.Companion.PRESET_CAPITALIZATION
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toNullableProperty
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer


/**
 * Component for editing a [WordScheme].
 *
 * @param scheme the scheme to edit
 */
class WordSchemeEditor(scheme: WordScheme = WordScheme()) : SchemeEditor<WordScheme>(scheme) {
    override val rootComponent = panel {
        group(Bundle("word.ui.words.header")) {
            lateinit var wordLists: ComboBox<WordList>

            row("Word list") {
                comboBox(emptyList<WordList>(), WordListListCellRenderer())
                    .onResetThis { cell ->
                        cell.component.removeAllItems()

                        (+scheme.context).wordListList.wordLists
                            .forEach { cell.component.addItem(it) }
                    }
                    .bindItem(scheme::wordList)
                    .bindValidation(scheme::wordList)
                    .also { wordLists = it.component }
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
}
