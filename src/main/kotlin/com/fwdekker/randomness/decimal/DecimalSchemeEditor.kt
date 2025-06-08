package com.fwdekker.randomness.decimal

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.SchemeEditor
import com.fwdekker.randomness.affix.AffixDecoratorEditor
import com.fwdekker.randomness.array.ArrayDecoratorEditor
import com.fwdekker.randomness.decimal.DecimalScheme.Companion.MIN_DECIMAL_COUNT
import com.fwdekker.randomness.decimal.DecimalScheme.Companion.PRESET_AFFIX_DECORATOR_DESCRIPTORS
import com.fwdekker.randomness.decimal.DecimalScheme.Companion.PRESET_DECIMAL_SEPARATORS
import com.fwdekker.randomness.decimal.DecimalScheme.Companion.PRESET_GROUPING_SEPARATORS
import com.fwdekker.randomness.ui.JDoubleSpinner
import com.fwdekker.randomness.ui.JIntSpinner
import com.fwdekker.randomness.ui.MinMaxLengthDocumentFilter
import com.fwdekker.randomness.ui.UIConstants
import com.fwdekker.randomness.ui.bindCurrentText
import com.fwdekker.randomness.ui.bindIntValue
import com.fwdekker.randomness.ui.bindSpinners
import com.fwdekker.randomness.ui.hasValue
import com.fwdekker.randomness.ui.isEditable
import com.fwdekker.randomness.ui.loadMnemonic
import com.fwdekker.randomness.ui.withFilter
import com.fwdekker.randomness.ui.withFixedWidth
import com.fwdekker.randomness.ui.withName
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindValue
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import javax.swing.JCheckBox


/**
 * Component for editing a [DecimalScheme].
 *
 * @param scheme the scheme to edit
 */
class DecimalSchemeEditor(scheme: DecimalScheme = DecimalScheme()) : SchemeEditor<DecimalScheme>(scheme) {
    override val rootComponent = panel {
        group(Bundle("decimal.ui.value.header")) {
            lateinit var minValue: JDoubleSpinner
            lateinit var maxValue: JDoubleSpinner

            row(Bundle("decimal.ui.value.min_option")) {
                cell(JDoubleSpinner())
                    .withFixedWidth(UIConstants.SIZE_VERY_LARGE)
                    .withName("minValue")
                    .bindValue(scheme::minValue)
                    .bindValidation(scheme::minValue)
                    .also { minValue = it.component }
            }

            row(Bundle("decimal.ui.value.max_option")) {
                cell(JDoubleSpinner())
                    .withFixedWidth(UIConstants.SIZE_VERY_LARGE)
                    .withName("maxValue")
                    .bindValue(scheme::maxValue)
                    .bindValidation(scheme::maxValue)
                    .also { maxValue = it.component }
            }

            bindSpinners(minValue, maxValue, DecimalScheme.MAX_VALUE_DIFFERENCE)
        }

        group(Bundle("decimal.ui.format.header")) {
            row(Bundle("decimal.ui.format.number_of_decimals_option")) {
                lateinit var decimalCount: JIntSpinner

                cell(JIntSpinner(value = MIN_DECIMAL_COUNT, minValue = MIN_DECIMAL_COUNT))
                    .withFixedWidth(UIConstants.SIZE_SMALL)
                    .withName("decimalCount")
                    .bindIntValue(scheme::decimalCount)
                    .bindValidation(scheme::decimalCount)
                    .also { decimalCount = it.component }

                checkBox(Bundle("decimal.ui.format.show_trailing_zeroes"))
                    .loadMnemonic()
                    .enabledIf(decimalCount.hasValue { it > 0 })
                    .withName("showTrailingZeroes")
                    .bindSelected(scheme::showTrailingZeroes)
                    .bindValidation(scheme::showTrailingZeroes)
            }

            row(Bundle("decimal.ui.format.decimal_separator_option")) {
                comboBox(PRESET_DECIMAL_SEPARATORS)
                    .isEditable(true)
                    .withFilter(MinMaxLengthDocumentFilter(1, 1))
                    .withName("decimalSeparator")
                    .bindCurrentText(scheme::decimalSeparator)
                    .bindValidation(scheme::decimalSeparator)
            }

            row {
                lateinit var groupingSeparatorEnabled: Cell<JCheckBox>

                checkBox(Bundle("decimal.ui.format.grouping_separator_option"))
                    .loadMnemonic()
                    .withName("groupingSeparatorEnabled")
                    .bindSelected(scheme::groupingSeparatorEnabled)
                    .bindValidation(scheme::groupingSeparatorEnabled)
                    .also { groupingSeparatorEnabled = it }

                comboBox(PRESET_GROUPING_SEPARATORS)
                    .enabledIf(groupingSeparatorEnabled.selected)
                    .isEditable(true)
                    .withFilter(MinMaxLengthDocumentFilter(1, 1))
                    .withName("groupingSeparator")
                    .bindCurrentText(scheme::groupingSeparator)
                    .bindValidation(scheme::groupingSeparator)
            }

            row {
                AffixDecoratorEditor(scheme.affixDecorator, PRESET_AFFIX_DECORATOR_DESCRIPTORS)
                    .also { decoratorEditors += it }
                    .let { cell(it.rootComponent) }
            }
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
