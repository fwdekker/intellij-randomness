package com.fwdekker.randomness.uuid

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.SchemeEditor
import com.fwdekker.randomness.affix.AffixDecoratorEditor
import com.fwdekker.randomness.array.ArrayDecoratorEditor
import com.fwdekker.randomness.ui.JDateTimeField
import com.fwdekker.randomness.ui.UIConstants
import com.fwdekker.randomness.ui.bindDateTimes
import com.fwdekker.randomness.ui.bindTimestamp
import com.fwdekker.randomness.ui.isEditable
import com.fwdekker.randomness.ui.loadMnemonic
import com.fwdekker.randomness.ui.withFixedWidth
import com.fwdekker.randomness.ui.withName
import com.fwdekker.randomness.uuid.UuidScheme.Companion.DEFAULT_MAX_DATE_TIME
import com.fwdekker.randomness.uuid.UuidScheme.Companion.DEFAULT_MIN_DATE_TIME
import com.fwdekker.randomness.uuid.UuidScheme.Companion.PRESET_AFFIX_DECORATOR_DESCRIPTORS
import com.fwdekker.randomness.uuid.UuidScheme.Companion.TIME_BASED_VERSIONS
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toNullableProperty
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.selectedValueMatches
import javax.swing.JList


/**
 * Component for editing a [UuidScheme].
 *
 * @param scheme the scheme to edit
 */
class UuidSchemeEditor(scheme: UuidScheme = UuidScheme()) : SchemeEditor<UuidScheme>(scheme) {
    override val rootComponent = panel {
        group(Bundle("uuid.ui.value.header")) {
            panel {
                lateinit var versionHasDateTime: ComponentPredicate
                lateinit var minDateTimeField: JDateTimeField
                lateinit var maxDateTimeField: JDateTimeField

                row(Bundle("uuid.ui.value.version.option")) {
                    comboBox(UuidScheme.SUPPORTED_VERSIONS, UuidVersionRenderer())
                        .isEditable(false)
                        .withName("version")
                        .bindItem(scheme::version.toNullableProperty())
                        .bindValidation(scheme::version)
                        .also { versionHasDateTime = it.component.selectedValueMatches { it in TIME_BASED_VERSIONS } }
                }

                row(Bundle("datetime.ui.value.min_datetime_option")) {  // TODO: Use uuid-specific text
                    cell(JDateTimeField(DEFAULT_MIN_DATE_TIME))
                        .withFixedWidth(UIConstants.SIZE_VERY_LARGE)
                        .withName("minDateTime")
                        .bindTimestamp(scheme::minDateTime)
                        .bindValidation(scheme::minDateTime)
                        .enabledIf(versionHasDateTime)
                        .also { minDateTimeField = it.component }
                }

                row(Bundle("datetime.ui.value.max_datetime_option")) {  // TODO: Use uuid-specific text
                    cell(JDateTimeField(DEFAULT_MAX_DATE_TIME))
                        .withFixedWidth(UIConstants.SIZE_VERY_LARGE)
                        .withName("maxDateTime")
                        .bindTimestamp(scheme::maxDateTime)
                        .bindValidation(scheme::maxDateTime)
                        .enabledIf(versionHasDateTime)
                        .also { maxDateTimeField = it.component }
                }.bottomGap(BottomGap.SMALL)

                bindDateTimes(minDateTimeField, maxDateTimeField)

                row {
                    checkBox(Bundle("uuid.ui.value.capitalization_option"))
                        .loadMnemonic()
                        .withName("isUppercase")
                        .bindSelected(scheme::isUppercase)
                        .bindValidation(scheme::isUppercase)
                }

                row {
                    checkBox(Bundle("uuid.add_dashes"))
                        .loadMnemonic()
                        .withName("addDashes")
                        .bindSelected(scheme::addDashes)
                        .bindValidation(scheme::addDashes)
                }

                row {
                    AffixDecoratorEditor(scheme.affixDecorator, PRESET_AFFIX_DECORATOR_DESCRIPTORS)
                        .also { decoratorEditors += it }
                        .let { cell(it.rootComponent) }
                }
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


    /**
     * Renders a supported UUID version.
     */
    private class UuidVersionRenderer : ColoredListCellRenderer<Int>() {
        override fun customizeCellRenderer(
            list: JList<out Int>,
            value: Int?,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean,
        ) {
            if (value == null) return

            append("$value")
            append("  ")
            append(Bundle("uuid.ui.value.version.$value"), SimpleTextAttributes.GRAYED_ATTRIBUTES)
        }
    }
}
