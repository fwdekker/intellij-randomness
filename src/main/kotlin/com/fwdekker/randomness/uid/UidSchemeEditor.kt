package com.fwdekker.randomness.uid

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.SchemeEditor
import com.fwdekker.randomness.affix.AffixDecoratorEditor
import com.fwdekker.randomness.array.ArrayDecoratorEditor
import com.fwdekker.randomness.ui.JDateTimeField
import com.fwdekker.randomness.ui.JIntSpinner
import com.fwdekker.randomness.ui.UIConstants
import com.fwdekker.randomness.ui.bindDateTimes
import com.fwdekker.randomness.ui.bindIntValue
import com.fwdekker.randomness.ui.bindTimestamp
import com.fwdekker.randomness.ui.isEditable
import com.fwdekker.randomness.ui.loadMnemonic
import com.fwdekker.randomness.ui.withFixedWidth
import com.fwdekker.randomness.ui.withName
import com.fwdekker.randomness.uid.NanoIdConfig.Companion.DEFAULT_SIZE
import com.fwdekker.randomness.uid.UuidConfig.Companion.DEFAULT_MAX_DATE_TIME
import com.fwdekker.randomness.uid.UuidConfig.Companion.DEFAULT_MIN_DATE_TIME
import com.fwdekker.randomness.uid.UuidConfig.Companion.TIME_BASED_VERSIONS
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toNullableProperty
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.selectedValueMatches
import javax.swing.JComboBox
import javax.swing.JList


/**
 * Component for editing a [UidScheme].
 *
 * @param scheme the scheme to edit
 */
class UidSchemeEditor(scheme: UidScheme = UidScheme()) : SchemeEditor<UidScheme>(scheme) {
    private lateinit var idTypeComboBox: JComboBox<IdType>

    override val rootComponent = panel {
        lateinit var isUuidSelected: ComponentPredicate
        lateinit var isNanoIdSelected: ComponentPredicate

        group(Bundle("uid.ui.value.header")) {
            row(Bundle("uid.ui.type.option")) {
                comboBox(IdType.entries, IdTypeRenderer())
                    .isEditable(false)
                    .withName("idType")
                    .bindItem(
                        getter = { scheme.idType },
                        setter = { value -> scheme.idType = value ?: IdType.DEFAULT }
                    )
                    .also { cell ->
                        idTypeComboBox = cell.component
                        isUuidSelected = cell.component.selectedValueMatches { it == IdType.Uuid }
                        isNanoIdSelected = cell.component.selectedValueMatches { it == IdType.NanoId }
                    }
            }

            // UUID settings panel
            rowsRange {
                lateinit var versionHasDateTime: ComponentPredicate
                lateinit var minDateTimeField: JDateTimeField
                lateinit var maxDateTimeField: JDateTimeField

                row(Bundle("uuid.ui.value.version.option")) {
                    comboBox(UuidConfig.SUPPORTED_VERSIONS, UuidVersionRenderer())
                        .isEditable(false)
                        .withName("version")
                        .bindItem(scheme.uuidConfig::version.toNullableProperty())
                        .bindValidation(scheme.uuidConfig::version)
                        .also { cell ->
                            versionHasDateTime = cell.component.selectedValueMatches { it in TIME_BASED_VERSIONS }
                        }
                }

                indent {
                    row(Bundle("uuid.ui.value.min_datetime_option")) {
                        cell(JDateTimeField(DEFAULT_MIN_DATE_TIME))
                            .withFixedWidth(UIConstants.SIZE_VERY_LARGE)
                            .withName("minDateTime")
                            .bindTimestamp(scheme.uuidConfig::minDateTime)
                            .bindValidation(scheme.uuidConfig::minDateTime)
                            .also { minDateTimeField = it.component }
                        contextHelp(Bundle("uuid.ui.datetime_help"))
                    }.enabledIf(versionHasDateTime)

                    row(Bundle("uuid.ui.value.max_datetime_option")) {
                        cell(JDateTimeField(DEFAULT_MAX_DATE_TIME))
                            .withFixedWidth(UIConstants.SIZE_VERY_LARGE)
                            .withName("maxDateTime")
                            .bindTimestamp(scheme.uuidConfig::maxDateTime)
                            .bindValidation(scheme.uuidConfig::maxDateTime)
                            .also { maxDateTimeField = it.component }
                        contextHelp(Bundle("uuid.ui.datetime_help"))
                    }.enabledIf(versionHasDateTime).bottomGap(BottomGap.SMALL)

                    bindDateTimes(minDateTimeField, maxDateTimeField)
                }

                row {
                    checkBox(Bundle("uuid.ui.value.capitalization_option"))
                        .loadMnemonic()
                        .withName("isUppercase")
                        .bindSelected(scheme.uuidConfig::isUppercase)
                        .bindValidation(scheme.uuidConfig::isUppercase)
                }

                row {
                    checkBox(Bundle("uuid.add_dashes"))
                        .loadMnemonic()
                        .withName("addDashes")
                        .bindSelected(scheme.uuidConfig::addDashes)
                        .bindValidation(scheme.uuidConfig::addDashes)
                }
            }.visibleIf(isUuidSelected)

            // NanoID settings panel
            rowsRange {
                row(Bundle("nanoid.ui.value.size_option")) {
                    cell(JIntSpinner(DEFAULT_SIZE, NanoIdConfig.MIN_SIZE, Int.MAX_VALUE))
                        .withFixedWidth(UIConstants.SIZE_SMALL)
                        .withName("size")
                        .bindIntValue(scheme.nanoIdConfig::size)
                        .bindValidation(scheme.nanoIdConfig::size)
                }
                row(Bundle("nanoid.ui.value.alphabet_option")) {
                    textField()
                        .withFixedWidth(UIConstants.SIZE_VERY_LARGE)
                        .withName("alphabet")
                        .bindText(scheme.nanoIdConfig::alphabet)
                        .bindValidation(scheme.nanoIdConfig::alphabet)
                }
            }.visibleIf(isNanoIdSelected)

            row {
                AffixDecoratorEditor(scheme.affixDecorator, UidScheme.PRESET_AFFIX_DECORATOR_DESCRIPTORS)
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


    /**
     * Renders an ID type in the dropdown.
     */
    private class IdTypeRenderer : ColoredListCellRenderer<IdType>() {
        override fun customizeCellRenderer(
            list: JList<out IdType>,
            value: IdType?,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean,
        ) {
            if (value == null) return
            append(value.displayName)
        }
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
