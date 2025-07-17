package com.fwdekker.randomness.testhelpers

import com.intellij.testFramework.fixtures.IdeaTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import io.kotest.core.TestConfiguration
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager


/**
 * Installs the [FailOnThreadViolationRepaintManager] on this scope, and uninstalls it at the end of this scope.
 *
 * @param addTags `true` if and only if tags should be added
 */
fun TestConfiguration.useEdtViolationDetection(addTags: Boolean = true) {
    if (addTags) tags(Tags.SWING)


    beforeSpec {
        FailOnThreadViolationRepaintManager.install()
    }

    afterSpec {
        FailOnThreadViolationRepaintManager.uninstall()
    }
}

/**
 * Sets up a bare [IdeaTestFixture] for each single test in this scope, and tears it down at the end of each such test.
 *
 * @param addTags `true` if and only if tags should be added
 */
fun TestConfiguration.useBareIdeaFixture(addTags: Boolean = true) {
    if (addTags) tags(Tags.IDEA_FIXTURE)


    lateinit var ideaFixture: IdeaTestFixture


    beforeNonContainer {
        ideaFixture = IdeaTestFixtureFactory.getFixtureFactory().createBareFixture()
        ideaFixture.setUp()
    }

    afterNonContainer {
        ideaFixture.tearDown()
    }
}
