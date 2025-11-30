package com.fwdekker.randomness.template

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.Scheme
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.ex.Settings
import com.intellij.openapi.util.Disposer
import java.awt.Component
import javax.swing.JComponent


/**
 * Tells IntelliJ how to use a [TemplateListEditor] to edit a [TemplateList] in the settings dialog.
 *
 * Set [schemeToSelect] before [createComponent] is invoked to determine which template should be selected when the
 * configurable opens.
 *
 * This class is separate from [TemplateListEditor] because that class creates UI components in the constructor. But
 * configurables may be created at any time in the background, so using [TemplateListEditor] as a configurable would
 * cause unnecessary lag.
 *
 * @see TemplateSettingsAction
 */
// TODO: Change into `SearchableConfigurable`, and deprecate the ugly `schemeToSelect` mechanism
internal class TemplateListConfigurable : Configurable, Disposable {
    /**
     * The user interface for changing the settings, displayed in IntelliJ's settings window.
     */
    @Suppress("detekt:LateinitUsage") // Initialized in [createComponent]
    lateinit var editor: TemplateListEditor private set

    /**
     * The UUID of the scheme to select after calling [createComponent].
     */
    var schemeToSelect: String? = null


    /**
     * Returns the name of the configurable as displayed in the settings window.
     */
    override fun getDisplayName() = Bundle("randomness")

    /**
     * Creates a new editor and returns the root pane of the created editor.
     */
    override fun createComponent(): JComponent {
        editor = TemplateListEditor(initialSelection = schemeToSelect).also { Disposer.register(this, it) }
        return editor.rootComponent
    }


    /**
     * Returns `true` if the settings were modified since they were loaded or they are invalid.
     */
    override fun isModified(): Boolean = editor.isModified() || editor.doValidate() != null

    /**
     * Saves the changes in the settings component to the default settings object, and updates template shortcuts.
     *
     * @throws ConfigurationException if the changes cannot be saved
     */
    @Throws(ConfigurationException::class)
    override fun apply() {
        editor.doValidate()?.also {
            if (it.state is Scheme)
                selectScheme(it.state)

            throw ConfigurationException(it.message, Bundle("template_list.error.failed_to_save_settings"))
        }

        val oldList = editor.originalTemplateList.deepCopy(retainUuid = true)
        editor.apply()
        val newList = editor.originalTemplateList.deepCopy(retainUuid = true)

        TemplateActionLoader().updateActions(oldList.templates, newList.templates)
    }

    /**
     * Discards unsaved changes in the settings component.
     */
    override fun reset() = editor.reset()


    /**
     * Explicitly selects the given scheme (based on its UUID).
     */
    fun selectScheme(scheme: Scheme) = editor.selectScheme(scheme)


    /**
     * Recursively disposes this configurable's resources.
     */
    override fun disposeUIResources() = Disposer.dispose(this)

    /**
     * Non-recursively disposes this configurable's resources.
     */
    override fun dispose() = Unit
}


/**
 * Selects [scheme] in the currently open [TemplateListConfigurable], or does nothing if this is not possible for some
 * reason.
 */
fun selectSchemeInSettings(context: Component, scheme: Scheme?) {
    scheme ?: return

    val id = "com.fwdekker.randomness.template.TemplateListConfigurable"
    val settings = Settings.KEY.getData(DataManager.getInstance().getDataContext(context)) ?: return
    val configurable = settings.find(id) as TemplateListConfigurable

    configurable.selectScheme(scheme)
}
