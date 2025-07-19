package com.fwdekker.randomness.ui

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.testhelpers.DummyScheme
import com.fwdekker.randomness.testhelpers.afterNonContainer
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import com.fwdekker.randomness.testhelpers.find
import com.fwdekker.randomness.testhelpers.ideaRunEdt
import com.fwdekker.randomness.testhelpers.matcher
import com.fwdekker.randomness.testhelpers.useBareIdeaFixture
import com.fwdekker.randomness.testhelpers.useEdtViolationDetection
import com.intellij.openapi.util.Disposer
import com.intellij.ui.InplaceButton
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.assertj.swing.fixture.Containers
import org.assertj.swing.fixture.FrameFixture


/**
 * Unit tests for [PreviewPanel].
 */
object PreviewPanelTest : FunSpec({
    lateinit var panel: PreviewPanel
    lateinit var frame: FrameFixture

    var scheme: DummyScheme? = null
    val placeholder = Bundle("preview.placeholder")


    useEdtViolationDetection()
    useBareIdeaFixture()

    beforeNonContainer {
        panel = ideaRunEdt { PreviewPanel { DummyScheme().also { scheme = it } } }
        frame = Containers.showInFrame(panel.rootComponent)

        panel.previewText shouldBe placeholder
    }

    afterNonContainer {
        frame.cleanUp()
        ideaRunEdt { Disposer.dispose(panel) }
    }


    context("updatePreview") {
        test("updates the label's contents") {
            ideaRunEdt { panel.updatePreview() }

            panel.previewText shouldBe "text0"
        }
    }

    context("seed") {
        test("reuses the old seed if the button is not pressed") {
            ideaRunEdt { panel.updatePreview() }
            val oldRandom = scheme?.random

            ideaRunEdt { panel.updatePreview() }
            val newRandom = scheme?.random

            newRandom?.nextInt() shouldBe oldRandom?.nextInt()
        }

        test("uses a new seed when the button is pressed") {
            ideaRunEdt { panel.updatePreview() }
            val oldRandom = scheme?.random

            ideaRunEdt { frame.find(matcher(InplaceButton::class.java)).doClick() }

            ideaRunEdt { panel.updatePreview() }
            val newRandom = scheme?.random

            newRandom?.nextInt() shouldNotBe oldRandom?.nextInt()
        }
    }
})
