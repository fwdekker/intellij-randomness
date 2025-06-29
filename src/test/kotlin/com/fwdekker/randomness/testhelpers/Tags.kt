package com.fwdekker.randomness.testhelpers

import com.fwdekker.randomness.testhelpers.Tags.SCHEME
import io.kotest.core.NamedTag


/**
 * Tags for Kotest-based tags for filtering tests.
 *
 * See the project's README for more information.
 */
object Tags {
    /**
     * Tests for [com.fwdekker.randomness.SchemeEditor]s.
     */
    val EDITOR = NamedTag("Editor")

    /**
     * Tests that rely on setting up an IDE fixture.
     */
    val IDEA_FIXTURE = NamedTag("IdeaFixture")

    /**
     * Tests that do require neither UI access nor complicated fixtures.
     *
     * Super-category of [SCHEME].
     */
    val PLAIN = NamedTag("Plain")

    /**
     * Tests for [com.fwdekker.randomness.Scheme]s.
     */
    val SCHEME = NamedTag("Scheme")

    /**
     * Tests that rely on accessing Swing components.
     */
    val SWING = NamedTag("Swing")
}
