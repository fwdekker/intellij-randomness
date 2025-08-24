package com.fwdekker.randomness.template

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.Icons
import com.fwdekker.randomness.InsertAction
import com.fwdekker.randomness.OverlayIcon
import com.fwdekker.randomness.Timely.generateTimely
import com.fwdekker.randomness.array.ArrayDecorator
import com.fwdekker.randomness.array.ArrayDecoratorEditor
import com.fwdekker.randomness.ui.PreviewPanel
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.ActionWrapperUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.util.Disposer
import java.awt.BorderLayout
import javax.swing.JPanel


/**
 * All actions related to inserting template-based strings.
 *
 * @param template the template to create actions for
 * @see TemplateInsertAction
 * @see TemplateSettingsAction
 */
class TemplateGroupAction(private val template: Template) :
    ActionGroup(template.name, Bundle("template.description.default", template.name), null) {
    /**
     * Returns the action that is appropriate for the given keyboard modifiers.
     *
     * @param array `true` if and only if the output should be in array form
     * @param repeat `true` if and only if the output should be repeated
     * @param settings `true` if and only if settings should be shown
     */
    private fun getActionByModifier(array: Boolean = false, repeat: Boolean = false, settings: Boolean = false) =
        if (settings) TemplateSettingsAction(template)
        else TemplateInsertAction(template, array = array, repeat = repeat)


    /**
     * Specifies the thread in which [update] is invoked.
     */
    override fun getActionUpdateThread() = ActionUpdateThread.EDT


    /**
     * Updates the [event]'s presentation of this action.
     */
    override fun update(event: AnActionEvent) {
        super.update(event)

        event.presentation.isPerformGroup = true
        event.presentation.isPopupGroup = true
        event.presentation.icon = template.icon?.get()
    }

    /**
     * Chooses one of the three actions to execute based on the key modifiers in [event].
     *
     * @param event carries contextual information
     */
    override fun actionPerformed(event: AnActionEvent) =
        getActionByModifier(
            array = event.inputEvent?.isShiftDown ?: false,
            repeat = event.inputEvent?.isAltDown ?: false,
            settings = event.inputEvent?.isControlDown ?: false
        ).let { ActionWrapperUtil.actionPerformed(event, this, it) }

    /**
     * Returns variant actions for the main insertion action.
     *
     * @param event carries contextual information
     * @return variant actions for the main insertion action
     */
    override fun getChildren(event: AnActionEvent?) =
        arrayOf(
            getActionByModifier(array = true),
            getActionByModifier(repeat = true),
            getActionByModifier(array = true, repeat = true),
            getActionByModifier(settings = true)
        )
}


/**
 * Inserts random strings in the editor using the given template.
 *
 * @param template the template to use for inserting data
 * @param array `true` if and only if an array of values should be inserted
 * @param repeat `true` if and only if the same value should be inserted at each caret
 * @see TemplateGroupAction
 */
class TemplateInsertAction(
    private val template: Template,
    private val array: Boolean = false,
    repeat: Boolean = false,
) : InsertAction(
    repeat = repeat,
    text = template.name
        .let { if (repeat) Bundle("template.name.repeat_prefix", it) else it }
        .let { if (array) Bundle("template.name.array_suffix", it) else it },
    description = when {
        array && repeat -> Bundle("template.description.repeat.array", template.name)
        repeat -> Bundle("template.description.repeat", template.name)
        array -> Bundle("template.description.array", template.name)
        else -> Bundle("template.description.default", template.name)
    },
    icon = {
        template.icon
            ?.let { if (array) it.plusOverlay(OverlayIcon.ARRAY) else it }
            ?.let { if (repeat) it.plusOverlay(OverlayIcon.REPEAT) else it }
            ?.get()
    }
) {
    override val configurable
        get() =
            if (array) ArrayDecoratorConfigurable(template.arrayDecorator)
            else null


    override fun generateStrings(count: Int) =
        generateTimely {
            val template = template.deepCopy().also { it.arrayDecorator.enabled = array }

            if (repeat) template.generateStrings().single().let { string -> List(count) { string } }
            else template.generateStrings(count)
        }


    /**
     * Edits the [ArrayDecorator] of a template and shows a preview.
     *
     * @param arrayDecorator the decorator being edited in this configurable
     */
    inner class ArrayDecoratorConfigurable(arrayDecorator: ArrayDecorator) : Configurable, Disposable {
        private val editor =
            ArrayDecoratorEditor(arrayDecorator, embedded = true)
                .also { Disposer.register(this, it) }
        private val previewPanel =
            PreviewPanel { template.deepCopy(retainUuid = true).also { it.arrayDecorator.enabled = true } }
                .also { Disposer.register(this, it) }


        init {
            editor.addChangeListener {
                editor.apply()
                previewPanel.updatePreview()
            }
            previewPanel.updatePreview()
        }


        /**
         * Returns the component that the [editor] prefers to be focused when the editor is focused.
         */
        override fun getPreferredFocusedComponent() = editor.preferredFocusedComponent

        /**
         * Creates a component containing the [editor] and the [previewPanel].
         */
        override fun createComponent() =
            JPanel(BorderLayout())
                .also {
                    it.add(editor.rootComponent, BorderLayout.NORTH)
                    it.add(previewPanel.rootComponent, BorderLayout.SOUTH)
                }

        /**
         * Returns `true`.
         */
        override fun isModified() = true

        /**
         * Saves the [editor]'s state.
         */
        override fun apply() = editor.apply()

        /**
         * Returns [text].
         */
        override fun getDisplayName() = text

        /**
         * Recursively disposes this configurable's resources.
         */
        override fun disposeUIResources() = Disposer.dispose(this)

        /**
         * Non-recursively disposes this configurable's resources.
         */
        override fun dispose() = Unit
    }
}

/**
 * Open the settings dialog to edit [template].
 *
 * @param template the template to select after opening the settings dialog
 * @see TemplateGroupAction
 * @see TemplateListConfigurable
 */
@Suppress("DialogTitleCapitalization") // False positive
class TemplateSettingsAction(private val template: Template? = null) : AnAction(
    if (template == null) Bundle("template.name.settings")
    else Bundle("template.name.settings_suffix", template.name),
    template?.let { Bundle("template.description.settings", it.name) },
    null
) {
    /**
     * Opens the IntelliJ settings menu at the right location to adjust the template configurable.
     *
     * @param event carries contextual information
     */
    override fun actionPerformed(event: AnActionEvent) =
        ShowSettingsUtil.getInstance()
            .showSettingsDialog(event.project, TemplateListConfigurable::class.java) { configurable ->
                configurable?.apply { schemeToSelect = template?.let { it.schemes.firstOrNull() ?: it }?.uuid }
            }


    /**
     * Specifies the thread in which [update] is invoked.
     */
    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    /**
     * Sets the icon of this action.
     *
     * @param event carries contextual information
     */
    override fun update(event: AnActionEvent) {
        event.presentation.icon = template?.icon?.plusOverlay(OverlayIcon.SETTINGS)?.get() ?: Icons.SETTINGS
    }
}
