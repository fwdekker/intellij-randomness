package com.fwdekker.randomness.template

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.SchemeEditor
import com.fwdekker.randomness.ui.UIConstants
import com.fwdekker.randomness.ui.withFixedWidth
import com.fwdekker.randomness.ui.withName
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel


/**
 * Component for editing non-children-related aspects of a [Template].
 *
 * @param scheme the scheme to edit
 */
class TemplateEditor(scheme: Template) : SchemeEditor<Template>(scheme) {
    override val rootComponent = panel {
        onApply { scheme.arrayDecorator.enabled = false }

        row(Bundle("template.ui.name_option")) {
            textField()
                .withFixedWidth(UIConstants.SIZE_LARGE)
                .withName("templateName")
                .bindText(scheme::name)
                .bindValidation(scheme::name)
        }
    }.finalize(this)


    init {
        reset()
    }
}
