package com.fwdekker.randomness.array

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.SchemeEditor
import com.fwdekker.randomness.affix.AffixDecoratorEditor
import com.fwdekker.randomness.array.ArrayDecorator.Companion.MIN_MIN_COUNT
import com.fwdekker.randomness.array.ArrayDecorator.Companion.PRESET_AFFIX_DECORATOR_DESCRIPTORS
import com.fwdekker.randomness.array.ArrayDecorator.Companion.PRESET_ELEMENT_FORMATS
import com.fwdekker.randomness.array.ArrayDecorator.Companion.PRESET_SEPARATORS
import com.fwdekker.randomness.ui.JIntSpinner
import com.fwdekker.randomness.ui.UIConstants
import com.fwdekker.randomness.ui.bindCurrentText
import com.fwdekker.randomness.ui.bindIntValue
import com.fwdekker.randomness.ui.bindSpinners
import com.fwdekker.randomness.ui.decoratedRowRange
import com.fwdekker.randomness.ui.isEditable
import com.fwdekker.randomness.ui.loadMnemonic
import com.fwdekker.randomness.ui.ofConstant
import com.fwdekker.randomness.ui.withFixedWidth
import com.fwdekker.randomness.ui.withName
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.and
import com.intellij.ui.layout.or
import com.intellij.ui.layout.selected
import javax.swing.JCheckBox


/**
 * Component for editing an [ArrayDecorator].
 *
 * @param scheme the scheme to edit
 * @param embedded `true` if the editor is embedded, which means that no titled separator is shown at the top,
 * components are additionally indented, and the user cannot disable the array decorator; does not affect the value of
 * [ArrayDecorator.enabled]
 */
class ArrayDecoratorEditor(
    scheme: ArrayDecorator,
    private val embedded: Boolean = false,
) : SchemeEditor<ArrayDecorator>(scheme) {
    override val rootComponent = panel {
        decoratedRowRange(title = if (!embedded) Bundle("array.title") else null, indent = !embedded) {
            lateinit var isEnabled: ComponentPredicate

            row {
                checkBox(Bundle("array.ui.enabled"))
                    .loadMnemonic()
                    .withName("arrayEnabled")
                    .bindSelected(scheme::enabled)
                    .bindValidation(scheme::enabled)
                    .also { isEnabled = it.selected.or(ComponentPredicate.ofConstant(embedded)) }
            }.visible(!embedded)

            decoratedRowRange(indent = !embedded) {
                lateinit var minCountSpinner: JIntSpinner
                lateinit var maxCountSpinner: JIntSpinner

                row(Bundle("array.ui.min_count_option")) {
                    cell(JIntSpinner(value = MIN_MIN_COUNT, minValue = MIN_MIN_COUNT))
                        .withFixedWidth(UIConstants.SIZE_SMALL)
                        .withName("arrayMinCount")
                        .bindIntValue(scheme::minCount)
                        .bindValidation(scheme::minCount)
                        .also { minCountSpinner = it.component }
                }

                row(Bundle("array.ui.max_count_option")) {
                    cell(JIntSpinner(value = MIN_MIN_COUNT, minValue = MIN_MIN_COUNT))
                        .withFixedWidth(UIConstants.SIZE_SMALL)
                        .withName("arrayMaxCount")
                        .bindIntValue(scheme::maxCount)
                        .bindValidation(scheme::maxCount)
                        .also { maxCountSpinner = it.component }
                }.bottomGap(BottomGap.SMALL)

                bindSpinners(minCountSpinner, maxCountSpinner)

                row {
                    lateinit var separatorEnabledCheckBox: JCheckBox

                    checkBox(Bundle("array.ui.separator.option"))
                        .withName("arraySeparatorEnabled")
                        .bindSelected(scheme::separatorEnabled)
                        .bindValidation(scheme::separatorEnabled)
                        .also { separatorEnabledCheckBox = it.component }

                    comboBox(PRESET_SEPARATORS)
                        .enabledIf(isEnabled.and(separatorEnabledCheckBox.selected))
                        .isEditable(true)
                        .withName("arraySeparator")
                        .bindCurrentText(scheme::separator)
                        .bindValidation(scheme::separator)
                }

                row(Bundle("array.ui.element_format.option")) {
                    comboBox(PRESET_ELEMENT_FORMATS)
                        .isEditable(true)
                        .withName("arrayElementFormat")
                        .bindCurrentText(scheme::elementFormat)
                        .bindValidation(scheme::elementFormat)
                    contextHelp(Bundle("array.ui.element_format.comment"))
                }

                row {
                    AffixDecoratorEditor(
                        scheme.affixDecorator,
                        PRESET_AFFIX_DECORATOR_DESCRIPTORS,
                        enabledIf = isEnabled,
                        enableMnemonic = false,
                        namePrefix = "array",
                    )
                        .also { decoratorEditors += it }
                        .let { cell(it.rootComponent) }
                }
            }.enabledIf(isEnabled)
        }
    }.finalize(this)


    init {
        reset()
    }
}
