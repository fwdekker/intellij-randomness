package com.fwdekker.randomness

import com.fwdekker.randomness.testhelpers.DummyDecoratorScheme
import com.fwdekker.randomness.testhelpers.DummyScheme
import com.fwdekker.randomness.testhelpers.Tags
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs


/**
 * Unit tests for [Scheme].
 */
object SchemeTest : FunSpec({
    tags(Tags.PLAIN, Tags.SCHEME)


    context("icon") {
        test("returns null if the type icon is null") {
            val scheme = DummyScheme()

            scheme.typeIcon = null

            scheme.icon shouldBe null
        }

        test("uses the type icon as a basis for the icon") {
            val scheme = DummyScheme()

            scheme.icon?.base shouldBe scheme.typeIcon
        }

        test("does not add icon overlays if the scheme has no decorators") {
            val scheme = DummyScheme()

            scheme.icon!!.overlays should beEmpty()
        }

        test("does not add icon overlays if the scheme's decorators have no icons") {
            val decorator = DummyDecoratorScheme()
            val scheme = DummyScheme(decorators = listOf(decorator))

            scheme.icon!!.overlays should beEmpty()
        }

        test("adds the scheme's decorators' icons") {
            val decorator = DummyDecoratorScheme()
            decorator.overlayIcon = OverlayIcon(Icons.REPEAT, Icons.REFERENCE)
            val scheme = DummyScheme(decorators = listOf(decorator))

            scheme.icon?.overlays shouldContainExactly listOf(decorator.overlayIcon)
        }
    }

    context("applyContext") {
        test("applies the context to itself") {
            val context = Settings()
            val scheme = DummyScheme()

            scheme.applyContext(context)

            +scheme.context shouldBeSameInstanceAs context
        }

        test("applies the context to each decorator") {
            val context = Settings()
            val decorator = DummyDecoratorScheme()
            val scheme = DummyScheme(decorators = listOf(decorator))

            scheme.applyContext(context)

            +decorator.context shouldBeSameInstanceAs context
        }
    }

    context("generateStrings") {
        test("throws an exception if the scheme is invalid") {
            val scheme = DummyScheme(valid = false)

            shouldThrow<DataGenerationException> { scheme.generateStrings() }.message shouldBe "DummyScheme is invalid"
        }

        test("returns undecorated strings if there is no decorator") {
            val scheme = DummyScheme()

            scheme.generateStrings(2) shouldContainExactly listOf("text0", "text1")
        }

        test("returns decorated strings if there is a decorator") {
            val scheme = DummyScheme(decorators = listOf(DummyDecoratorScheme(enabled = true)))

            scheme.generateStrings(2) shouldContainExactly listOf("text0:decorated", "text1:decorated")
        }

        test("applies decorators in order") {
            val scheme = DummyScheme(
                decorators = listOf(
                    DummyDecoratorScheme(enabled = true, append = ":a"),
                    DummyDecoratorScheme(enabled = true, append = ":b"),
                    DummyDecoratorScheme(enabled = true, append = ":c"),
                ),
            )

            scheme.generateStrings(2) shouldContainExactly listOf("text0:a:b:c", "text1:a:b:c")
        }

        test("applies decorators recursively") {
            val scheme = DummyScheme(
                decorators = listOf(
                    DummyDecoratorScheme(
                        enabled = true,
                        append = ":a",
                        decorators = listOf(
                            DummyDecoratorScheme(
                                enabled = true,
                                append = ":aa",
                                decorators = listOf(DummyDecoratorScheme(enabled = true, append = ":aaa")),
                            ),
                            DummyDecoratorScheme(
                                enabled = true,
                                append = ":ab",
                                decorators = listOf(DummyDecoratorScheme(enabled = true, append = ":aba")),
                            ),
                        ),
                    ),
                    DummyDecoratorScheme(
                        enabled = true,
                        append = ":b",
                        decorators = listOf(DummyDecoratorScheme(enabled = true, append = ":ba")),
                    ),
                )
            )

            scheme.generateStrings()[0] shouldBe "text0:a:aa:aaa:ab:aba:b:ba"
        }
    }
})

/**
 * Unit tests for [DecoratorScheme].
 */
object DecoratorSchemeTest : FunSpec({
    tags(Tags.PLAIN, Tags.SCHEME)


    context("generateStrings") {
        test("throws an exception if the decorator is invalid") {
            val decorator = DummyDecoratorScheme(enabled = true, valid = false)

            shouldThrow<DataGenerationException> { decorator.generateStrings() }
                .message shouldBe "DummyDecoratorScheme is invalid"
        }

        test("throws an exception if the decorator is invalid, even if disabled") {
            val decorator = DummyDecoratorScheme(valid = false)

            shouldThrow<DataGenerationException> { decorator.generateStrings() }
                .message shouldBe "DummyDecoratorScheme is invalid"
        }

        test("returns the generator's output if disabled") {
            val decorator = DummyDecoratorScheme()
            decorator.generator = { count -> List(count) { "text$it" } }

            decorator.generateStrings(2) shouldContainExactly listOf("text0", "text1")
        }

        test("decorators the generator's output if enabled") {
            val decorator = DummyDecoratorScheme(enabled = true)
            decorator.generator = { count -> List(count) { "text$it" } }

            decorator.generateStrings(2) shouldContainExactly listOf("text0:decorated", "text1:decorated")
        }
    }
})
