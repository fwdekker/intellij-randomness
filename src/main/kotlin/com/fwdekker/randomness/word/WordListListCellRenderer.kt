package com.fwdekker.randomness.word

import com.intellij.ui.ColoredListCellRenderer
import javax.swing.JList


class WordListListCellRenderer : ColoredListCellRenderer<WordList>() {
    override fun customizeCellRenderer(
        list: JList<out WordList>,
        value: WordList?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean,
    ) {
        if (value == null) return

        append(value.name)
    }
}
