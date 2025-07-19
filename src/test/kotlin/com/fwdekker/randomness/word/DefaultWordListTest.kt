package com.fwdekker.randomness.word

import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import com.fwdekker.randomness.testhelpers.shouldMatchBundle
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.should
import java.io.IOException
import kotlin.system.measureNanoTime


/**
 * Unit tests for [DefaultWordList].
 */
object DefaultWordListTest : FunSpec({
    tags(Tags.PLAIN)


    beforeNonContainer {
        DefaultWordList.clearCache()
    }


    context("words") {
        test("throws an exception if the file does not exist") {
            val list = DefaultWordList("name", "word-lists/does-not-exist.txt")

            shouldThrow<IOException> { list.words }.message shouldMatchBundle "word_list.error.file_not_found"
        }

        test("returns an empty list of words if the file is empty") {
            val list = DefaultWordList("name", "word-lists/empty-list.txt")

            list.words should beEmpty()
        }

        test("returns the list of words from the file") {
            val list = DefaultWordList("name", "word-lists/non-empty-list.txt")

            list.words shouldContainExactly listOf("lorem", "ipsum", "dolor")
        }

        test("does not return blank lines") {
            val list = DefaultWordList("name", "word-lists/with-blank-lines.txt")

            list.words shouldContainExactly listOf("lorem", "ipsum", "dolor", "sit")
        }

        context("caching") {
            test("throws an exception again if the words are read again") {
                val list = DefaultWordList("name", "word-lists/does-not-exist.txt")

                shouldThrow<IOException> { list.words }
                    .message shouldMatchBundle "word_list.error.file_not_found"
            }

            test("returns words quicker if read again from the same instance") {
                val list = DefaultWordList("name", "word-lists/timing-test.txt")

                val firstTime = measureNanoTime { list.words }
                val secondTime = measureNanoTime { list.words }

                secondTime shouldBeLessThan firstTime / 2
            }

            test("returns words quicker if read again from another instance") {
                val firstList = DefaultWordList("name", "word-lists/timing-test.txt")
                val firstTime = measureNanoTime { firstList.words }

                val secondList = DefaultWordList("name", "word-lists/timing-test.txt")
                val secondTime = measureNanoTime { secondList.words }

                secondTime shouldBeLessThan firstTime / 2
            }
        }
    }
})
