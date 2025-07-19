package com.fwdekker.randomness.template

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.SchemeEditor
import com.fwdekker.randomness.affix.AffixDecoratorEditor
import com.fwdekker.randomness.array.ArrayDecoratorEditor
import com.fwdekker.randomness.template.TemplateReference.Companion.PRESET_AFFIX_DECORATOR_DESCRIPTORS
import com.fwdekker.randomness.template.TemplateReference.Companion.PRESET_CAPITALIZATION
import com.fwdekker.randomness.ui.onResetThis
import com.fwdekker.randomness.ui.withName
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toNullableProperty
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import com.intellij.ui.layout.ComboBoxPredicate
import javax.swing.JList


/**
 * Component for editing a [TemplateReference].
 *
 * @param scheme the scheme to edit
 */
class TemplateReferenceEditor(scheme: TemplateReference) : SchemeEditor<TemplateReference>(scheme) {
    override val rootComponent = panel {
        group(Bundle("reference.ui.value.header")) {
            row(Bundle("reference.ui.value.template_option")) {
                lateinit var templates: ComboBox<Template>

                comboBox(emptyList(), TemplateCellRenderer())
                    .onResetThis { cell ->
                        cell.component.removeAllItems()

                        (+scheme.context).templates
                            .filter { scheme.canReference(it) }
                            .forEach { cell.component.addItem(it) }
                    }
                    .withName("template")
                    .bindItem(scheme::template.toNullableProperty())
                    .bindValidation(scheme::template)
                    .also { templates = it.component }

                link(Bundle("reference.ui.value.visit")) { selectSchemeInSettings(templates, scheme.template) }
                    .visibleIf(ComboBoxPredicate<Template>(templates) { it != null })
            }

            row(Bundle("reference.ui.value.capitalization_option")) {
                comboBox(PRESET_CAPITALIZATION, textListCellRenderer { it?.toLocalizedString() })
                    .withName("capitalization")
                    .bindItem(scheme::capitalization.toNullableProperty())
                    .bindValidation(scheme::capitalization)
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


    /**
     * Renders a template.
     */
    private class TemplateCellRenderer : ColoredListCellRenderer<Template>() {
        /**
         * Renders the [value] as its icon and name, ignoring other parameters.
         */
        override fun customizeCellRenderer(
            list: JList<out Template>,
            value: Template?,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean,
        ) {
            icon = (value?.icon ?: Template.DEFAULT_ICON).get()
            append(value?.name ?: Bundle("template.name.unknown"))
        }
    }
}
