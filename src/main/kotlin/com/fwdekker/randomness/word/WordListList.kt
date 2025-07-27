package com.fwdekker.randomness.word

import com.fwdekker.randomness.State
import com.intellij.util.xmlb.annotations.OptionTag


data class WordList(
    var name: String = "My Word List",
    @OptionTag
    val words: MutableList<String> = mutableListOf(),
) : State() {
    override fun deepCopy(retainUuid: Boolean): WordList =
        copy(words = words.toMutableList()).deepCopyTransient(retainUuid)
}

data class WordListList(
    @OptionTag
    val wordLists: MutableList<WordList> = mutableListOf(),
) : State() {
    override fun deepCopy(retainUuid: Boolean): WordListList =
        copy(wordLists = wordLists.map { it.deepCopy(retainUuid) }.toMutableList())
}
