package com.fwdekker.randomness.fixedlength

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.SchemeEditor
import com.fwdekker.randomness.fixedlength.FixedLengthDecorator.Companion.MIN_LENGTH
import com.fwdekker.randomness.ui.JIntSpinner
import com.fwdekker.randomness.ui.MaxLengthDocumentFilter
import com.fwdekker.randomness.ui.UIConstants
import com.fwdekker.randomness.ui.bindIntValue
import com.fwdekker.randomness.ui.loadMnemonic
import com.fwdekker.randomness.ui.withDocument
import com.fwdekker.randomness.ui.withFixedWidth
import com.fwdekker.randomness.ui.withName
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import javax.swing.JCheckBox
import javax.swing.text.PlainDocument


/**
 * Component for editing a [FixedLengthDecorator].
 *
 * @param scheme the scheme to edit
 */
class FixedLengthDecoratorEditor(scheme: FixedLengthDecorator) : SchemeEditor<FixedLengthDecorator>(scheme) {
    override val rootComponent = panel {
        group(Bundle("fixed_length.title")) {
            lateinit var enabledCheckBox: Cell<JCheckBox>

            row {
                checkBox(Bundle("fixed_length.ui.enabled"))
                    .loadMnemonic()
                    .withName("fixedLengthEnabled")
                    .bindSelected(scheme::enabled)
                    .bindValidation(scheme::enabled)
                    .also { enabledCheckBox = it }
            }

            indent {
                row(Bundle("fixed_length.ui.length_option")) {
                    cell(JIntSpinner(value = MIN_LENGTH, minValue = MIN_LENGTH))
                        .withFixedWidth(UIConstants.SIZE_SMALL)
                        .withName("fixedLengthLength")
                        .bindIntValue(scheme::length)
                        .bindValidation(scheme::length)
                }

                row(Bundle("fixed_length.ui.filler_option")) {
                    textField()
                        .withFixedWidth(UIConstants.SIZE_SMALL)
                        .withDocument(PlainDocument().also { it.documentFilter = MaxLengthDocumentFilter(1) })
                        .withName("fixedLengthFiller")
                        .bindText(scheme::filler)
                        .bindValidation(scheme::filler)
                }
            }.enabledIf(enabledCheckBox.selected)
        }
    }.finalize(this)


    init {
        reset()
    }
}
