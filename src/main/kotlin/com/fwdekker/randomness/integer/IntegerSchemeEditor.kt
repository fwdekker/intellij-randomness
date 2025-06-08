package com.fwdekker.randomness.integer

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.SchemeEditor
import com.fwdekker.randomness.affix.AffixDecoratorEditor
import com.fwdekker.randomness.array.ArrayDecoratorEditor
import com.fwdekker.randomness.fixedlength.FixedLengthDecoratorEditor
import com.fwdekker.randomness.integer.IntegerScheme.Companion.DECIMAL_BASE
import com.fwdekker.randomness.integer.IntegerScheme.Companion.MAX_BASE
import com.fwdekker.randomness.integer.IntegerScheme.Companion.MIN_BASE
import com.fwdekker.randomness.integer.IntegerScheme.Companion.PRESET_AFFIX_DECORATOR_DESCRIPTORS
import com.fwdekker.randomness.integer.IntegerScheme.Companion.PRESET_GROUPING_SEPARATORS
import com.fwdekker.randomness.ui.JIntSpinner
import com.fwdekker.randomness.ui.JLongSpinner
import com.fwdekker.randomness.ui.MinMaxLengthDocumentFilter
import com.fwdekker.randomness.ui.UIConstants
import com.fwdekker.randomness.ui.bindCurrentText
import com.fwdekker.randomness.ui.bindIntValue
import com.fwdekker.randomness.ui.bindLongValue
import com.fwdekker.randomness.ui.bindSpinners
import com.fwdekker.randomness.ui.hasValue
import com.fwdekker.randomness.ui.isEditable
import com.fwdekker.randomness.ui.loadMnemonic
import com.fwdekker.randomness.ui.withFilter
import com.fwdekker.randomness.ui.withFixedWidth
import com.fwdekker.randomness.ui.withName
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.and
import com.intellij.ui.layout.selected
import javax.swing.JCheckBox


/**
 * Component for editing an [IntegerScheme].
 *
 * @param scheme the scheme to edit
 */
class IntegerSchemeEditor(scheme: IntegerScheme = IntegerScheme()) : SchemeEditor<IntegerScheme>(scheme) {
    override val rootComponent = panel {
        group(Bundle("integer.ui.value.header")) {
            lateinit var minValue: JLongSpinner
            lateinit var maxValue: JLongSpinner

            row(Bundle("integer.ui.value.min_option")) {
                cell(JLongSpinner())
                    .withFixedWidth(UIConstants.SIZE_LARGE)
                    .withName("minValue")
                    .bindLongValue(scheme::minValue)
                    .bindValidation(scheme::minValue)
                    .also { minValue = it.component }
            }

            row(Bundle("integer.ui.value.max_option")) {
                cell(JLongSpinner())
                    .withFixedWidth(UIConstants.SIZE_LARGE)
                    .withName("maxValue")
                    .bindLongValue(scheme::maxValue)
                    .bindValidation(scheme::maxValue)
                    .also { maxValue = it.component }
            }

            bindSpinners(minValue, maxValue, maxRange = null)
        }

        group(Bundle("integer.ui.format.header")) {
            lateinit var base: JIntSpinner
            lateinit var groupingSeparatorEnabled: JCheckBox

            row(Bundle("integer.ui.format.base_option")) {
                cell(JIntSpinner(DECIMAL_BASE, MIN_BASE, MAX_BASE))
                    .withFixedWidth(UIConstants.SIZE_SMALL)
                    .withName("base")
                    .bindIntValue(scheme::base)
                    .bindValidation(scheme::base)
                    .also { base = it.component }
            }

            row {
                checkBox(Bundle("integer.ui.format.uppercase_option"))
                    .enabledIf(base.hasValue { it > DECIMAL_BASE })
                    .loadMnemonic()
                    .withName("isUppercase")
                    .bindSelected(scheme::isUppercase)
                    .bindValidation(scheme::isUppercase)
            }

            row {
                checkBox(Bundle("integer.ui.format.grouping_separator_option"))
                    .loadMnemonic()
                    .withName("groupingSeparatorEnabled")
                    .bindSelected(scheme::groupingSeparatorEnabled)
                    .bindValidation(scheme::groupingSeparatorEnabled)
                    .also { groupingSeparatorEnabled = it.component }

                comboBox(PRESET_GROUPING_SEPARATORS)
                    .enabledIf(base.hasValue { it == DECIMAL_BASE }.and(groupingSeparatorEnabled.selected))
                    .withName("groupingSeparator")
                    .isEditable(true)
                    .withFilter(MinMaxLengthDocumentFilter(1, 1))
                    .bindCurrentText(scheme::groupingSeparator)
                    .bindValidation(scheme::groupingSeparator)
            }.enabledIf(base.hasValue { it == DECIMAL_BASE })

            row {
                AffixDecoratorEditor(scheme.affixDecorator, PRESET_AFFIX_DECORATOR_DESCRIPTORS)
                    .also { decoratorEditors += it }
                    .let { cell(it.rootComponent) }
            }
        }

        row {
            FixedLengthDecoratorEditor(scheme.fixedLengthDecorator)
                .also { decoratorEditors += it }
                .let { cell(it.rootComponent).align(AlignX.FILL) }
        }

        row {
            ArrayDecoratorEditor(scheme.arrayDecorator)
                .also { decoratorEditors += it }
                .let { cell(it.rootComponent).align(AlignX.FILL) }
        }
    }.finalize(this)


    init {
        reset()
    }
}
