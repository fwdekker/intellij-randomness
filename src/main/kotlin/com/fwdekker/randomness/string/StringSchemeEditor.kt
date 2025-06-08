package com.fwdekker.randomness.string

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.SchemeEditor
import com.fwdekker.randomness.array.ArrayDecoratorEditor
import com.fwdekker.randomness.string.StringScheme.Companion.PRESET_CAPITALIZATION
import com.fwdekker.randomness.ui.UIConstants
import com.fwdekker.randomness.ui.loadMnemonic
import com.fwdekker.randomness.ui.withFixedWidth
import com.fwdekker.randomness.ui.withName
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toNullableProperty
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import com.intellij.ui.layout.selected
import javax.swing.JCheckBox


/**
 * Component for editing a [StringScheme].
 *
 * @param scheme the scheme to edit
 */
class StringSchemeEditor(scheme: StringScheme = StringScheme()) : SchemeEditor<StringScheme>(scheme) {
    override val rootComponent = panel {
        group(Bundle("string.ui.value.header")) {
            row(Bundle("string.ui.value.pattern_option")) {
                textField()
                    .withFixedWidth(UIConstants.SIZE_VERY_LARGE)
                    .withName("pattern")
                    .bindText(scheme::pattern)
                    .bindValidation(scheme::pattern)

                browserLink(Bundle("string.ui.value.pattern_help"), Bundle("string.ui.value.pattern_help_url"))
            }

            row("") {
                lateinit var isRegexBox: JCheckBox

                checkBox(Bundle("string.ui.value.is_regex_option"))
                    .loadMnemonic()
                    .withName("isRegex")
                    .bindSelected(scheme::isRegex)
                    .bindValidation(scheme::isRegex)
                    .also { isRegexBox = it.component }

                checkBox(Bundle("string.ui.value.is_non_matching_option"))
                    .loadMnemonic()
                    .withName("isNonMatching")
                    .bindSelected(scheme::isNonMatching)
                    .bindValidation(scheme::isNonMatching)
                    .enabledIf(isRegexBox.selected)
            }.bottomGap(BottomGap.SMALL)

            row(Bundle("string.ui.value.capitalization_option")) {
                comboBox(PRESET_CAPITALIZATION, textListCellRenderer { it?.toLocalizedString() })
                    .withName("capitalization")
                    .bindItem(scheme::capitalization.toNullableProperty())
                    .bindValidation(scheme::capitalization)
            }

            row {
                checkBox(Bundle("string.ui.value.remove_look_alike"))
                    .loadMnemonic()
                    .withName("removeLookAlikeCharacters")
                    .bindSelected(scheme::removeLookAlikeSymbols)
                    .bindValidation(scheme::removeLookAlikeSymbols)

                contextHelp(Bundle("string.ui.value.remove_look_alike_help", StringScheme.LOOK_ALIKE_CHARACTERS))
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
