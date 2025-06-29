package com.fwdekker.randomness

import com.fwdekker.randomness.testhelpers.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.row
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe


/**
 * Unit tests for extension functions in `ListHelpersKt`.
 */
object ListHelpersTest : FunSpec({
    tags(Tags.PLAIN)


    context("getMod") {
        withData(
            mapOf(
                "index `0` returns 0th element" to row(42, 0, 0),
                "index `1` returns 1st element" to row(42, 1, 1),
                "index `size - 1` returns last element" to row(42, 41, 41),
                "index `size` returns 0th element" to row(42, 42, 0),
                "index `size + 1` returns 1st element" to row(42, 43, 1),
                "index `size * 2` returns 0th element" to row(42, 84, 0),
                "index `size * 100 + 5` returns 5th element" to row(42, 4205, 5),
                "index `-1` returns last element" to row(42, -1, 41),
                "index `-2` returns first-to-last element" to row(42, -2, 40),
                "index `-3` returns second-to-last element" to row(42, -3, 39),
                "index `-size` returns 0th element" to row(42, -42, 0),
                "index `-size * 2` returns 0th element" to row(42, -84, 0),
                "index `-size * 50 + 9` returns 9th element" to row(42, -4191, 9),
            )
        ) { (size, idx, expected) ->
            val list = List(size) { it }
            list.getMod(idx) shouldBe expected
        }
    }

    context("setAll") {
        test("adds the given elements to an empty list") {
            val list = mutableListOf<Int>()

            list.setAll(listOf(0, 1, 2, 3))

            list shouldBe listOf(0, 1, 2, 3)
        }

        test("removes existing elements before inserting the new elements") {
            val list = mutableListOf(0, 1, 2)

            list.setAll(listOf(3, 4, 5))

            list shouldBe listOf(3, 4, 5)
        }

        test("removes existing elements even if they are again to be inserted") {
            val list = mutableListOf(0, 1, 1, 2)

            list.setAll(listOf(1, 2, 3))

            list shouldBe listOf(1, 2, 3)
        }
    }
})
