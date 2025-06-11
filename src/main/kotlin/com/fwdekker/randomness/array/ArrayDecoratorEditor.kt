package com.fwdekker.randomness.array

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.SchemeEditor
import com.fwdekker.randomness.affix.AffixDecoratorEditor
import com.fwdekker.randomness.array.ArrayDecorator.Companion.MIN_MIN_COUNT
import com.fwdekker.randomness.array.ArrayDecorator.Companion.PRESET_AFFIX_DECORATOR_DESCRIPTORS
import com.fwdekker.randomness.array.ArrayDecorator.Companion.PRESET_INDICES_FORMATS
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
import com.intellij.ui.dsl.builder.Cell
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
            lateinit var enabledCheckBox: Cell<JCheckBox>
            lateinit var isEnabled: ComponentPredicate

            row {
                checkBox(Bundle("array.ui.enabled"))
                    .loadMnemonic()
                    .withName("arrayEnabled")
                    .bindSelected(scheme::enabled)
                    .also { enabledCheckBox = it }
                    .also { isEnabled = enabledCheckBox.selected.or(ComponentPredicate.ofConstant(embedded)) }
            }.visible(!embedded)

            decoratedRowRange(indent = !embedded) {
                lateinit var minCountSpinner: JIntSpinner
                lateinit var maxCountSpinner: JIntSpinner

                row(Bundle("array.ui.min_count_option")) {
                    cell(JIntSpinner(value = MIN_MIN_COUNT, minValue = MIN_MIN_COUNT))
                        .withFixedWidth(UIConstants.SIZE_SMALL)
                        .withName("arrayMinCount")
                        .bindIntValue(scheme::minCount)
                        .also { minCountSpinner = it.component }
                }

                row(Bundle("array.ui.max_count_option")) {
                    cell(JIntSpinner(value = MIN_MIN_COUNT, minValue = MIN_MIN_COUNT))
                        .withFixedWidth(UIConstants.SIZE_SMALL)
                        .withName("arrayMaxCount")
                        .bindIntValue(scheme::maxCount)
                        .also { maxCountSpinner = it.component }
                }.bottomGap(BottomGap.SMALL)

                bindSpinners(minCountSpinner, maxCountSpinner)

                row {
                    lateinit var separatorEnabledCheckBox: JCheckBox

                    checkBox(Bundle("array.ui.separator.option"))
                        .withName("arraySeparatorEnabled")
                        .bindSelected(scheme::separatorEnabled)
                        .also { separatorEnabledCheckBox = it.component }

                    comboBox(PRESET_SEPARATORS)
                        .enabledIf(isEnabled.and(separatorEnabledCheckBox.selected))
                        .isEditable(true)
                        .withName("arraySeparator")
                        .bindCurrentText(scheme::separator)
                }
                
                row {
                    lateinit var showIndicesCheckBox: JCheckBox
                    
                    checkBox(Bundle("array.ui.show_indices.option"))
                        .withName("arrayShowIndices")
                        .bindSelected(scheme::showIndices)
                        .also { showIndicesCheckBox = it.component }
                        
                    comboBox(PRESET_INDICES_FORMATS)
                        .enabledIf(isEnabled.and(showIndicesCheckBox.selected))
                        .isEditable(true)
                        .withName("arrayIndicesFormat") 
                        .bindCurrentText(scheme::indicesFormat)
                }
                
                row {
                    checkBox(Bundle("array.ui.use_tuple_indices.option"))
                        .withName("arrayUseTupleIndices")
                        .bindSelected(scheme::useTupleIndices)
                        .enabledIf(isEnabled.and(frame.checkBox("arrayShowIndices").selected))
                }.enabledIf(isEnabled)
                
                row {
                    comment(Bundle("array.ui.show_indices.comment"))
                }.enabledIf(isEnabled)

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
    }


    init {
        reset()
    }
}
