package com.fwdekker.randomness.ui

import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import javax.swing.text.DocumentFilter
import javax.swing.text.PlainDocument


/**
 * Unit tests for [MaxLengthDocumentFilter].
 */
object MaxLengthDocumentFilterTest : FunSpec({
    tags(Tags.PLAIN)


    lateinit var document: PlainDocument
    lateinit var filter: DocumentFilter
    val getText = { document.getText(0, document.length) }


    beforeNonContainer {
        document = PlainDocument()
        filter = MaxLengthDocumentFilter(3)
        document.documentFilter = filter
    }


    context("constructor") {
        test("throws an exception if 'maxLength' is negative") {
            shouldThrow<IllegalArgumentException> { MaxLengthDocumentFilter(-3) }
                .message shouldBe "Maximum length must be a positive number, but was '-3'."
        }
    }


    context("insert") {
        test("does nothing if the inserted string is null") {
            document.insertString(0, "str", null)

            document.insertString(0, null, null)

            getText() shouldBe "str"
        }

        context("without overwrite") {
            test("inserts a string of length 0 at offset 0") {
                document.insertString(0, "str", null)

                document.insertString(0, "", null)

                getText() shouldBe "str"
            }

            test("inserts a string of length 0 at offset 1") {
                document.insertString(0, "str", null)

                document.insertString(1, "", null)

                getText() shouldBe "str"
            }

            test("inserts a string of length 1 into an empty document") {
                document.insertString(0, "c", null)

                getText() shouldBe "c"
            }

            test("inserts multiple strings after each other") {
                document.insertString(0, "s", null)
                document.insertString(1, "t", null)
                document.insertString(2, "r", null)

                getText() shouldBe "str"
            }

            test("inserts a string at the start") {
                document.insertString(0, "tr", null)
                document.insertString(0, "s", null)

                getText() shouldBe "str"
            }

            test("inserts a string in the middle") {
                document.insertString(0, "sr", null)
                document.insertString(1, "t", null)

                getText() shouldBe "str"
            }

            test("inserts a string of maximum length into an empty document") {
                document.insertString(0, "str", null)

                getText() shouldBe "str"
            }
        }

        context("with overwrite") {
            test("overwrites the last character if a single character is inserted after the end") {
                document.insertString(0, "stx", null)
                document.insertString(3, "r", null)

                getText() shouldBe "str"
            }

            test("overwrites the last character if multiple characters are inserted after the end") {
                document.insertString(0, "stx", null)
                document.insertString(3, "yzr", null)

                getText() shouldBe "str"
            }

            test("overwrites the first character if a single character is inserted at the start") {
                document.insertString(0, "xtr", null)
                document.insertString(0, "s", null)

                getText() shouldBe "str"
            }

            test("overwrites the first two characters if two characters are inserted at the start") {
                document.insertString(0, "xyr", null)
                document.insertString(0, "st", null)

                getText() shouldBe "str"
            }

            test("overwrites the last two characters if two characters are inserted at an offset") {
                document.insertString(0, "sxy", null)
                document.insertString(1, "tr", null)

                getText() shouldBe "str"
            }

            test("overwrites all characters") {
                document.insertString(0, "xyz", null)
                document.insertString(0, "str", null)

                getText() shouldBe "str"
            }

            test("inserts the last 'maxLength' characters of the string into an empty document") {
                document.insertString(0, "stxyzr", null)

                getText() shouldBe "str"
            }

            test("inserts the characters that still fit at the start and overwrites the remainder") {
                document.insertString(0, "xr", null)
                document.insertString(0, "st", null)

                getText() shouldBe "str"
            }

            test("inserts the characters that still fit in the middle and overwrites the remainder") {
                document.insertString(0, "sx", null)
                document.insertString(1, "tr", null)

                getText() shouldBe "str"
            }

            test("inserts the characters that still fit, overwrites the remainder, and inserts the last character") {
                document.insertString(0, "sxy", null)
                document.insertString(1, "tzr", null)

                getText() shouldBe "str"
            }
        }
    }
})

/**
 * Unit tests for [MinMaxLengthDocumentFilter].
 */
object MinMaxLengthDocumentFilterTest : FunSpec({
    lateinit var document: PlainDocument
    lateinit var filter: DocumentFilter
    val getText = { document.getText(0, document.length) }


    beforeNonContainer {
        document = PlainDocument()
        filter = MinMaxLengthDocumentFilter(2, 5)
        document.documentFilter = filter
    }


    context("constructor") {
        test("throws an exception if 'minLength' is greater than 'maxLength'") {
            shouldThrow<IllegalArgumentException> { MinMaxLengthDocumentFilter(4, 2) }
                .message shouldBe "Minimum length must be less than or equal to maximum length."
        }
    }


    context("insert") {
        test("inserts new characters while the length is below range") {
            document.insertString(0, "str", null)

            getText() shouldBe "str"
        }
    }

    context("remove") {
        test("removes characters if the result's length is in range") {
            document.insertString(0, "word", null)

            document.remove(1, 2)

            getText() shouldBe "wd"
        }

        test("removes the last characters in the selection if removing more would put result's length out of range") {
            document.insertString(0, "word", null)

            document.remove(0, 3)

            getText() shouldBe "wd"
        }

        test("removes characters if that puts the result's length back into range") {
            document.documentFilter = null
            document.insertString(0, "very-long", null)
            document.documentFilter = filter

            document.remove(3, 6)

            getText() shouldBe "ver"
        }
    }
})
