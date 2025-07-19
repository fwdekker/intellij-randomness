package com.fwdekker.randomness.template

import com.fwdekker.randomness.Settings
import com.fwdekker.randomness.testhelpers.afterNonContainer
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import com.fwdekker.randomness.testhelpers.ideaRunEdt
import com.fwdekker.randomness.testhelpers.shouldContainExactly
import com.fwdekker.randomness.testhelpers.shouldMatchBundle
import com.fwdekker.randomness.testhelpers.useBareIdeaFixture
import com.fwdekker.randomness.testhelpers.useEdtViolationDetection
import com.intellij.openapi.options.ConfigurationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.assertj.swing.fixture.Containers
import org.assertj.swing.fixture.FrameFixture


/**
 * Unit tests for [TemplateListConfigurable].
 */
object TemplateListConfigurableTest : FunSpec({
    lateinit var frame: FrameFixture

    lateinit var configurable: TemplateListConfigurable


    useEdtViolationDetection()
    useBareIdeaFixture()

    beforeSpec {
        TemplateListEditor.useTestSplitter = true
    }

    afterSpec {
        TemplateListEditor.useTestSplitter = false
    }

    beforeNonContainer {
        configurable = TemplateListConfigurable()
        frame = Containers.showInFrame(ideaRunEdt { configurable.createComponent() })
    }

    afterNonContainer {
        frame.cleanUp()
        ideaRunEdt { configurable.disposeUIResources() }
    }


    context("templateToSelect") {
        test("selects the template with the given UUID") {
            frame.cleanUp()
            ideaRunEdt { configurable.disposeUIResources() }

            configurable = TemplateListConfigurable()
            configurable.schemeToSelect = Settings.DEFAULT.templates[2].uuid
            frame = Containers.showInFrame(ideaRunEdt { configurable.createComponent() })

            ideaRunEdt { frame.tree().target().selectionRows!! } shouldContainExactly arrayOf(4)
        }
    }


    context("isModified") {
        test("returns `false` if no modifications were made") {
            configurable.isModified shouldBe false
        }

        test("returns `true` if modifications were made") {
            ideaRunEdt { frame.textBox("templateName").target().text = "New Name" }

            configurable.isModified shouldBe true
        }

        test("returns `true` if no modifications were made but the template list is invalid") {
            Settings.DEFAULT.templates[0].name = ""
            ideaRunEdt { configurable.reset() }

            configurable.editor.isModified() shouldBe false
            configurable.isModified shouldBe true
        }
    }

    context("apply") {
        test("throws an exception if the template list is invalid") {
            ideaRunEdt { frame.textBox("templateName").target().text = "" }

            shouldThrow<ConfigurationException> { configurable.apply() }
                .title shouldMatchBundle "template_list.error.failed_to_save_settings"
        }

        test("applies the changes") {
            ideaRunEdt { frame.textBox("templateName").target().text = "New Name" }

            configurable.apply()

            Settings.DEFAULT.templates[0].name shouldBe "New Name"
        }
    }

    context("reset") {
        test("resets the editor") {
            ideaRunEdt { frame.textBox("templateName").target().text = "Changed Name" }

            ideaRunEdt { configurable.reset() }

            ideaRunEdt { frame.textBox("templateName").target().text } shouldNotBe "Changed Name"
        }
    }
})
