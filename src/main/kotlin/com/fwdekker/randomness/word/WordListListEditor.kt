package com.fwdekker.randomness.word

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.setAll
import com.fwdekker.randomness.ui.ValidationInfo
import com.intellij.openapi.Disposable
import com.intellij.ui.dsl.builder.panel


class WordListListEditor(val originalWordListList: WordListList = WordListList()) : Disposable {
    val rootComponent = panel {
        group(Bundle("word.ui.words.header")) {
            row {
                label("Woah mama")
                checkBox("Check it out!")
            }
        }
    }.also { reset() }


    private val currentWordListList = WordListList() // Synced with [originalWordListList] in [reset]


    fun doValidate(): ValidationInfo? = currentWordListList.doValidate()

    fun isModified(): Boolean = originalWordListList != currentWordListList

    fun apply() {
        originalWordListList.wordLists.setAll(currentWordListList.deepCopy(retainUuid = true).wordLists)
        originalWordListList.applyContext((+originalWordListList.context).copy(wordListList = originalWordListList))
    }

    fun reset() {
        currentWordListList.wordLists.setAll(originalWordListList.deepCopy(retainUuid = true).wordLists)
        currentWordListList.applyContext((+currentWordListList.context).copy(wordListList = currentWordListList))

//        templateTree.reload() // TODO: Reload UI somehow? Is that necessary?
    }

    /**
     * Disposes this editor's resources.
     */
    override fun dispose() = Unit
}
