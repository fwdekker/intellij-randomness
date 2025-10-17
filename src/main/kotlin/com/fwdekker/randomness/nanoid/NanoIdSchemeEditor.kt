package com.fwdekker.randomness.nanoid

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.SchemeEditor
import com.fwdekker.randomness.affix.AffixDecoratorEditor
import com.fwdekker.randomness.array.ArrayDecoratorEditor
import com.fwdekker.randomness.nanoid.NanoIdScheme.Companion.DEFAULT_SIZE
import com.fwdekker.randomness.nanoid.NanoIdScheme.Companion.PRESET_AFFIX_DECORATOR_DESCRIPTORS
import com.fwdekker.randomness.ui.JIntSpinner
import com.fwdekker.randomness.ui.UIConstants
import com.fwdekker.randomness.ui.bindIntValue
import com.fwdekker.randomness.ui.withFixedWidth
import com.fwdekker.randomness.ui.withName
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel

/**
 * Component for editing a [NanoIdScheme].
 *
 * @param scheme the scheme to edit
 */
class NanoIdSchemeEditor(scheme: NanoIdScheme = NanoIdScheme()) : SchemeEditor<NanoIdScheme>(scheme) {
    override val rootComponent = panel {
        group(Bundle("nanoid.ui.value.header")) {
            row(Bundle("nanoid.ui.value.size_option")) {
                cell(JIntSpinner(DEFAULT_SIZE, NanoIdScheme.MIN_SIZE, Int.MAX_VALUE))
                    .withFixedWidth(UIConstants.SIZE_SMALL)
                    .withName("size")
                    .bindIntValue(scheme::size)
                    .bindValidation(scheme::size)
            }
            row(Bundle("nanoid.ui.value.alphabet_option")) {
                textField()
                    .withName("alphabet")
                    .bindText(scheme::alphabet)
                    .bindValidation(scheme::alphabet)
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
