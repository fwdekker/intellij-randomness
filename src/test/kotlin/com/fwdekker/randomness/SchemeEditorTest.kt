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
import com.fwdekker.randomness.testhelpers.useBareIdeaFixture
import com.fwdekker.randomness.testhelpers.useEdtViolationDetection
import com.fwdekker.randomness.ui.withName
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.assertj.swing.fixture.Containers
import org.assertj.swing.fixture.FrameFixture
import java.awt.Component
import javax.swing.JCheckBox
import javax.swing.text.JTextComponent


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
        lateinit var foo: JTextComponent
        lateinit var bar: JTextComponent


        beforeNonContainer {
            registerTestEditor { DummyValidatableSchemeEditor(DummyValidatableScheme()) }

            foo = ideaRunEdt { frame.textBox("foo").target() }
            bar = ideaRunEdt { frame.textBox("bar").target() }
        }

        suspend fun revalidate() =
            ideaRunEdt {
                editor.apply()
                editor.doValidate()
            }

        suspend fun JTextComponent.hasError(): Boolean =
            ideaRunEdt { this.getClientProperty("JComponent.outline") } != null


        test("shows no errors if all fields are valid") {
            ideaRunEdt {
                frame.textBox("foo").target().text = "foo"
                frame.textBox("bar").target().text = "bar"
            }

            revalidate()
            foo.hasError() shouldBe false
            bar.hasError() shouldBe false
        }

        test("shows an error for the invalid field") {
            ideaRunEdt { frame.textBox("foo").target().text = "wrong" }

            revalidate()
            foo.hasError() shouldBe true
            bar.hasError() shouldBe false
        }

        test("shows an error for each invalid field") {
            ideaRunEdt {
                frame.textBox("foo").target().text = "wrong"
                frame.textBox("bar").target().text = "wrong"
            }

            revalidate()
            foo.hasError() shouldBe true
            bar.hasError() shouldBe true
        }

        test("stops showing an error once the field is valid again") {
            ideaRunEdt { frame.textBox("foo").target().text = "wrong" }
            revalidate()
            foo.hasError() shouldBe true

            ideaRunEdt { frame.textBox("foo").target().text = "foo" }
            revalidate()
            foo.hasError() shouldBe false
        }

        test("stops showing an error once a field is valid again, even if other fields are still invalid") {
            ideaRunEdt {
                frame.textBox("foo").target().text = "wrong"
                frame.textBox("bar").target().text = "wrong"
            }
            revalidate()
            foo.hasError() shouldBe true
            bar.hasError() shouldBe true

            ideaRunEdt { frame.textBox("foo").target().text = "foo" }
            revalidate()
            foo.hasError() shouldBe false
            bar.hasError() shouldBe true
        }

        test("stops showing errors for all fields if they are all valid again") {
            ideaRunEdt {
                frame.textBox("foo").target().text = "wrong"
                frame.textBox("bar").target().text = "wrong"
            }
            revalidate()
            foo.hasError() shouldBe true
            bar.hasError() shouldBe true

            ideaRunEdt {
                frame.textBox("foo").target().text = "foo"
                frame.textBox("bar").target().text = "bar"
            }
            revalidate()
            foo.hasError() shouldBe false
            bar.hasError() shouldBe false
        }

        test("shows and hides error popups once fields become invalid and valid, respectively") {
            ideaRunEdt { frame.textBox("foo").target().text = "wrong" }
            revalidate()
            foo.hasError() shouldBe true
            bar.hasError() shouldBe false

            ideaRunEdt {
                frame.textBox("foo").target().text = "foo"
                frame.textBox("bar").target().text = "wrong"
            }
            revalidate()
            foo.hasError() shouldBe false
            bar.hasError() shouldBe true
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
