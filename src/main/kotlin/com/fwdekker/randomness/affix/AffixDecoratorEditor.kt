package com.fwdekker.randomness.affix

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.SchemeEditor
import com.fwdekker.randomness.camelPlus
import com.fwdekker.randomness.ui.bindCurrentText
import com.fwdekker.randomness.ui.disableMnemonic
import com.fwdekker.randomness.ui.isEditable
import com.fwdekker.randomness.ui.loadMnemonic
import com.fwdekker.randomness.ui.withName
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.and
import javax.swing.JCheckBox


/**
 * Component for editing an [AffixDecorator].
 *
 * @param scheme the scheme to edit
 * @param presets the default affixation options available to the user in the editor
 * @param enabledIf the predicate that determines whether the components in this editor are enabled
 * @param enableMnemonic whether to enable mnemonics
 * @param namePrefix the string to prepend to all component names
 */
class AffixDecoratorEditor(
    scheme: AffixDecorator,
    presets: Collection<String>,
    enabledIf: ComponentPredicate? = null,
    enableMnemonic: Boolean = true,
    namePrefix: String = "",
) : SchemeEditor<AffixDecorator>(scheme) {
    override val rootComponent = panel {
        row {
            lateinit var enabledCheckBox: Cell<JCheckBox>

            checkBox(Bundle("affix.ui.option"))
                .let { if (enableMnemonic) it.loadMnemonic() else it.disableMnemonic() }
                .withName(namePrefix.camelPlus("affixEnabled"))
                .bindSelected(scheme::enabled)
                .bindValidation(scheme::enabled)
                .also { enabledCheckBox = it }

            comboBox(presets)
                .enabledIf(enabledIf?.and(enabledCheckBox.selected) ?: enabledCheckBox.selected)
                .isEditable(true)
                .withName(namePrefix.camelPlus("affixDescriptor"))
                .bindCurrentText(scheme::descriptor)
                .bindValidation(scheme::descriptor)
            contextHelp(Bundle("affix.ui.comment"))
        }.also { if (enabledIf != null) it.enabledIf(enabledIf) }
    }.finalize(this)


    init {
        reset()
    }
}
