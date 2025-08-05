package com.fwdekker.randomness

import com.fwdekker.randomness.SettingsFileManager.backUpConfigTo
import com.fwdekker.randomness.SettingsFileManager.deleteSettings
import com.fwdekker.randomness.SettingsFileManager.settingsFile
import com.fwdekker.randomness.ui.SimpleDialogAction
import com.fwdekker.randomness.ui.SimpleNotificationAction
import com.fwdekker.randomness.ui.askYesNo
import com.fwdekker.randomness.ui.showDialogMessage
import com.fwdekker.randomness.ui.showNotification
import com.intellij.icons.AllIcons.General
import com.intellij.ide.plugins.PluginManagerConfigurable
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.ui.components.JBLabel
import java.io.File
import java.io.IOException


/**
 * Utility methods for managing the settings file underlying [PersistentSettings] itself.
 */
internal object SettingsFileManager {
    /**
     * The notification group to which notifications created by this class belong.
     */
    private const val GROUP = "com.fwdekker.randomness.settings_errors"

    /**
     * Returns the underlying settings file.
     */
    private val settingsFile: File get() = PathManager.getOptionsFile("randomness3")


    /**
     * Copies the [settingsFile] to [target].
     */
    internal fun backUpConfigTo(target: File) {
        settingsFile.copyTo(target, overwrite = true)
    }

    /**
     * Deletes the [settingsFile] and resets [PersistentSettings] to its default state.
     */
    internal fun deleteSettings() {
        settingsFile.delete()
        service<PersistentSettings>().resetState()
    }


    /**
     * Shows a notification through which users can troubleshoot issues with their settings file.
     *
     * @param project the project in which the error notification should be displayed
     * @param title the title to show in the notification
     * @param message the message to display in the notification
     */
    fun showRepairNotification(project: Project? = null, title: String, message: String) {
        val key = "notifications.settings_error.notification"

        showNotification(
            project,
            NotificationType.ERROR,
            GROUP,
            title,
            message,
            SimpleNotificationAction(Bundle("$key.open_plugin_manager")) { _, _ ->
                ShowSettingsUtil.getInstance().showSettingsDialog(project, PluginManagerConfigurable::class.java)
            },
            SimpleNotificationAction(Bundle("$key.backup_and_reset")) { _, notification ->
                showRepairDialog { notification.hideBalloon() }
            },
            isSuggestion = true,
        )
    }

    /**
     * Shows a dialog with repair options for the settings file.
     *
     * @param project the project in which the dialog should be shown
     * @param callback the function to invoke once the dialog is closed
     */
    fun showRepairDialog(project: Project? = null, callback: () -> Unit = {}) {
        val key = "notifications.settings_error.backup_and_reset"

        DialogBuilder()
            .title(Bundle("$key.title"))
            .centerPanel(JBLabel(Bundle("$key.message")))
            .actionDescriptors(
                SimpleDialogAction(Bundle("$key.backup")) { _, _ -> showBackupDialog(project) },
                SimpleDialogAction(Bundle("$key.delete")) { _, dialog ->
                    if (showResetDialog(project)) {
                        dialog.close(0)
                        callback()
                    }
                },
            )
            .cancelButton()
            .showModal(false)
    }

    /**
     * Runs a dialog-based interface around [backUpConfigTo].
     */
    private fun showBackupDialog(project: Project?) {
        val key = "notifications.settings_error.backup"

        val descriptor = FileSaverDescriptor(Bundle("$key.chooser"), "", "xml")
        val target = FileChooserFactory.getInstance()
            .createSaveFileDialog(descriptor, project)
            .save("randomness3.xml")
        if (target == null) return

        try {
            backUpConfigTo(target.file)
            showDialogMessage(project, "", Bundle("$key.success"), General.SuccessDialog)
        } catch (exception: IOException) {
            showDialogMessage(project, "", Bundle("$key.error", exception.message), General.ErrorDialog)
        }
    }

    /**
     * Runs a dialog-based interface around [deleteSettings].
     *
     * @param project the project to show the dialog in
     * @return `true` if and only if the reset was successful
     */
    private fun showResetDialog(project: Project?): Boolean {
        val key = "notifications.settings_error.delete"

        if (!askYesNo(project, Bundle("$key.confirm.title"), Bundle("$key.confirm.message")))
            return false

        return try {
            deleteSettings()
            showDialogMessage(project, "", Bundle("$key.success"), General.SuccessDialog)
            true
        } catch (exception: IOException) {
            showDialogMessage(project, "", Bundle("$key.error", exception.message), General.ErrorDialog)
            false
        }
    }


    /**
     * Syntactic sugar to add the given [descriptors] in a DSL-like fashion.
     */
    private fun DialogBuilder.actionDescriptors(vararg descriptors: DialogBuilder.ActionDescriptor): DialogBuilder =
        apply { descriptors.forEach { addActionDescriptor(it) } }

    /**
     * Syntactic sugar to add a cancel button in a DSL-like fashion.
     */
    private fun DialogBuilder.cancelButton(): DialogBuilder =
        apply { addCancelAction() }
}
