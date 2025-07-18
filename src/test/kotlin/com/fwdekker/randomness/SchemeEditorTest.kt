package com.fwdekker.randomness

import com.fwdekker.randomness.testhelpers.DummyDecoratorScheme
import com.fwdekker.randomness.testhelpers.DummyDecoratorSchemeEditor
import com.fwdekker.randomness.testhelpers.DummyScheme
import com.fwdekker.randomness.testhelpers.DummySchemeEditor
import com.fwdekker.randomness.testhelpers.DummyValidatableScheme
import com.fwdekker.randomness.testhelpers.DummyValidatableSchemeEditor
import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.afterNonContainer
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import com.fwdekker.randomness.testhelpers.ideaRunEdt
import com.fwdekker.randomness.testhelpers.matcher
import com.fwdekker.randomness.testhelpers.useBareIdeaFixture
import com.fwdekker.randomness.testhelpers.useEdtViolationDetection
import com.fwdekker.randomness.ui.withName
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.popup.AbstractPopup
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.assertj.swing.fixture.Containers
import org.assertj.swing.fixture.FrameFixture
import java.awt.Component
import javax.swing.JCheckBox
import javax.swing.JEditorPane


/**
 * Unit tests for [SchemeEditor].
 */
object SchemeEditorTest : FunSpec({
    tags(Tags.EDITOR)


    lateinit var frame: FrameFixture
    lateinit var editor: SchemeEditor<*>


    useEdtViolationDetection()
    useBareIdeaFixture()

    afterNonContainer {
        frame.cleanUp()
    }


    suspend fun registerTestEditor(createEditor: () -> SchemeEditor<*>) {
        editor = ideaRunEdt(createEditor)
        frame = Containers.showInFrame(editor.rootComponent)
    }


    context("components") {
        test("returns named components in order") {
            registerTestEditor {
                DummySchemeEditor {
                    panel {
                        row("Label") {
                            textField().withName("field1")
                            textField()
                            textField().withName("field3")
                        }
                    }
                }
            }

            editor.components.map { (it as Component).name } shouldContainExactly listOf("field1", "field3")
        }

        test("returns extra components") {
            registerTestEditor { DummySchemeEditor().also { it.addExtraComponent("extra") } }

            editor.components shouldContainExactly listOf("extra")
        }
    }

    context("preferredFocusComponent") {
        test("returns the first visible named component") {
            registerTestEditor {
                DummySchemeEditor {
                    panel {
                        row("Label") {
                            textField()
                            spinner(0..10).withName("spinner").visible(false)
                            checkBox("Check").withName("checkbox")
                        }
                    }
                }
            }

            editor.preferredFocusedComponent should beInstanceOf<JCheckBox>()
        }

        test("returns null if there are no visible named components") {
            registerTestEditor {
                DummySchemeEditor {
                    panel {
                        row("Label") {
                            textField()
                            spinner(0..10).withName("spinner").visible(false)
                        }
                    }
                }
            }

            editor.preferredFocusedComponent shouldBe null
        }
    }


    context("reset") {
        test("resets the editor to its original state") {
            registerTestEditor {
                val scheme = DummyScheme(prefix = "old")

                DummySchemeEditor(scheme) { panel { row { textField().bindText(scheme::prefix) } } }
            }
            frame.textBox().requireText("old")

            ideaRunEdt { frame.textBox().target().text = "new" }
            frame.textBox().requireText("new")
            ideaRunEdt { editor.reset() }

            frame.textBox().requireText("old")
        }

        test("resets decorators to their original state") {
            registerTestEditor {
                val decorator = DummyDecoratorScheme(append = "old")
                val scheme = DummyScheme(decorators = listOf(decorator))

                DummySchemeEditor(scheme) {
                    panel {
                        row {
                            DummyDecoratorSchemeEditor(decorator)
                                .also { addDecoratorEditor(it) }
                                .let { cell(it.rootComponent) }
                        }
                    }
                }
            }
            frame.textBox().requireText("old")

            ideaRunEdt { frame.textBox().target().text = "new" }
            frame.textBox().requireText("new")
            ideaRunEdt { editor.reset() }

            frame.textBox().requireText("old")
        }
    }

    context("apply") {
        test("applies changes from the editor") {
            val scheme = DummyScheme(prefix = "old")
            registerTestEditor { DummySchemeEditor(scheme) { panel { row { textField().bindText(scheme::prefix) } } } }

            ideaRunEdt { frame.textBox().target().text = "new" }
            editor.apply()

            scheme.prefix shouldBe "new"
        }

        test("applies changes from decorators") {
            val decorator = DummyDecoratorScheme(append = "old")
            val scheme = DummyScheme(decorators = listOf(decorator))
            registerTestEditor {
                DummySchemeEditor(scheme) {
                    panel {
                        row {
                            DummyDecoratorSchemeEditor(decorator)
                                .also { addDecoratorEditor(it) }
                                .let { cell(it.rootComponent) }
                        }
                    }
                }
            }

            ideaRunEdt { frame.textBox().target().text = "new" }
            editor.apply()

            scheme.decorators[0] shouldBeSameInstanceAs decorator
            (scheme.decorators[0] as DummyDecoratorScheme).append shouldBe "new"
        }
    }

    context("doValidate") {
        suspend fun getErrorPopupTexts(revalidate: Boolean = true): List<String> {
            if (revalidate) {
                ideaRunEdt { editor.apply() }
                frame.label("label").click() // Click outside fields to close existing popups
                ideaRunEdt { editor.doValidate() }
            }

            return ideaRunEdt {
                frame.robot().finder().findAll(matcher(AbstractPopup.MyContentPanel::class.java))
                    .map { popup ->
                        popup.components
                            .filterIsInstance<JEditorPane>()
                            .single()
                            .text
                            .replace(Regex("""<[^>]+>"""), "") // Remove HTML tags
                            .trim(' ', '\n')
                    }
            }
        }


        beforeNonContainer {
            registerTestEditor { DummyValidatableSchemeEditor(DummyValidatableScheme()) }
        }


        test("shows no error popups if all fields are valid") {
            frame.textBox("foo").setText("foo")
            frame.textBox("bar").setText("bar")

            getErrorPopupTexts() should beEmpty()
        }

        test("shows an error popup for an invalid field") {
            frame.textBox("foo").setText("wrong")

            getErrorPopupTexts() shouldContainOnly listOf("Foo field is invalid.")
        }

        test("shows an error popup for each invalid field") {
            frame.textBox("foo").setText("wrong")
            frame.textBox("bar").setText("wrong")

            getErrorPopupTexts() shouldContainOnly listOf("Foo field is invalid.", "Bar field is invalid.")
        }

        test("hides error popups after clicking outside the invalid fields") {
            frame.textBox("foo").setText("wrong")
            getErrorPopupTexts() shouldContainOnly listOf("Foo field is invalid.")

            frame.label("label").click()
            getErrorPopupTexts(revalidate = false) should beEmpty()
        }

        test("hides error popups once fields are valid again") {
            frame.textBox("foo").setText("wrong")
            getErrorPopupTexts() shouldContainOnly listOf("Foo field is invalid.")

            frame.textBox("foo").setText("foo")
            getErrorPopupTexts() should beEmpty()
        }

        test("shows and hides error popups once fields become invalid and valid, respectively") {
            frame.textBox("foo").setText("wrong")
            getErrorPopupTexts() shouldContainOnly listOf("Foo field is invalid.")

            frame.textBox("foo").setText("foo")
            frame.textBox("bar").setText("wrong")
            getErrorPopupTexts() shouldContainOnly listOf("Bar field is invalid.")
        }
    }


    context("addChangeListener") {
        test("invokes the listener when a visible named component is edited") {
            registerTestEditor { DummySchemeEditor { panel { row { textField().withName("name") } } } }

            var updateCount = 0
            editor.addChangeListener { updateCount++ }
            ideaRunEdt { frame.textBox().target().text = "new" }

            updateCount shouldBe 1
        }

        test("invokes the listener when an extra component is edited") {
            registerTestEditor {
                DummySchemeEditor { panel { row { textField().also { addExtraComponent(it.component) } } } }
            }

            var updateCount = 0
            editor.addChangeListener { updateCount++ }
            ideaRunEdt { frame.textBox().target().text = "new" }

            updateCount shouldBeGreaterThanOrEqual 1
        }

        test("invokes the listener when a decorator editor's component is edited") {
            registerTestEditor {
                val decorator = DummyDecoratorScheme(append = "old")
                val scheme = DummyScheme(decorators = listOf(decorator))

                DummySchemeEditor(scheme) {
                    panel {
                        row {
                            DummyDecoratorSchemeEditor(decorator)
                                .also { addDecoratorEditor(it) }
                                .let { cell(it.rootComponent) }
                        }
                    }
                }
            }

            var updateCount = 0
            editor.addChangeListener { updateCount++ }
            ideaRunEdt { frame.textBox().target().text = "new" }

            updateCount shouldBeGreaterThanOrEqual 2
        }
    }
})
