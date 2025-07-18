package com.fwdekker.randomness.ui

import javax.swing.JComponent
import javax.swing.JSpinner
import javax.swing.SwingUtilities


/**
 * Moves focus to `this` component, soon.
 *
 * Has special handling for components for which I personally think that the focus should be handled differently.
 */
fun JComponent.focusLater() =
    SwingUtilities.invokeLater {
        val target =
            if (this is JSpinner) (this.editor as JSpinner.NumberEditor).textField
            else this

        target.requestFocus()
    }
