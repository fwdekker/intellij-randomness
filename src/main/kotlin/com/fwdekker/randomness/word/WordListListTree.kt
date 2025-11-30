package com.fwdekker.randomness.word

import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.ListSelectionModel


class WordListListTree(wordListList: WordListList) {
    val dataModel = CollectionListModel(wordListList.wordLists, true)
    val selectionModel: ListSelectionModel
    val component = JPanel(BorderLayout())


    init {
        val list = JBList(dataModel)
        selectionModel = list.selectionModel
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.cellRenderer = WordListListCellRenderer()

        val decorator = ToolbarDecorator.createDecorator(list)
            .setAddAction {
                dataModel.add(WordList("New Word List"))
                selectionModel.setSelectionInterval(dataModel.size - 1, dataModel.size - 1)
            }
            .setRemoveAction {
                if (list.selectedIndex >= 0)
                    dataModel.remove(list.selectedIndex)
            }
            .createPanel()

        component.add(decorator, BorderLayout.NORTH)
        component.add(list, BorderLayout.CENTER)
    }
}
