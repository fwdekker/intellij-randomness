package com.fwdekker.randomness

import com.intellij.ide.plugins.PluginManagerConfigurable
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.messages.MessageDialog
import com.intellij.ui.components.JBLabel
import java.awt.event.ActionEvent
import java.lang.module.ModuleDescriptor.Version
import javax.swing.AbstractAction
import javax.swing.Action


/**
 * Displays notifications to the user.
 */
internal object Notifier {
    /**
     * Shows a notification introducing the user to version 3 of Randomness if the user previously used Randomness 2.
     */
    fun showWelcomeToV3(project: Project) {
        val oldSettings = PathManager.getOptionsFile("randomness")

        val propComp = PropertiesComponent.getInstance()
        val propKey = "com.fwdekker.randomness.notifications.welcome_to_v3"
        if (propComp.isTrueValue(propKey) || !oldSettings.exists())
            return

        showNotification(
            project,
            NotificationType.INFORMATION,
            "com.fwdekker.randomness.updates",
            Bundle("notifications.welcome_to_v3.title"),
            Bundle("notifications.welcome_to_v3.message"),
            SimpleNotificationAction(Bundle("notifications.welcome_to_v3.delete_old_settings")) { _, notification ->
                if (askYesNo(project, Bundle("notifications.welcome_to_v3.delete_old_settings_confirm"))) {
                    oldSettings.delete()
                    propComp.setValue(propKey, true)
                    notification.hideBalloon()
                }
            },
            SimpleNotificationAction(Bundle("notifications.welcome_to_v3.do_not_ask_again")) { _, notification ->
                propComp.setValue(propKey, true)
                notification.hideBalloon()
            },
            isSuggestionType = true,
        )
    }


    /**
     * Shows a notification informing the user that the settings file has an unsupported version from the future and
     * cannot be used.
     *
     * @param futureVersion the version to which the user must update Randomness to ensure the settings file can be read
     * @param project the project in which the error notification should be displayed
     */
    fun showFutureSettingsErrorNotification(futureVersion: Version, project: Project? = null) =
        showSettingsErrorNotification(
            project,
            Bundle("notifications.settings_error.future_error.title"),
            Bundle("notifications.settings_error.future_error.message", futureVersion.toString())
        )

    /**
     * Shows a notification informing the user that the settings could not be parsed and allows the user to delete their
     * settings file.
     *
     * @param project the project in which the error notification should be displayed
     */
    fun showParseSettingsErrorNotification(project: Project? = null) =
        showSettingsErrorNotification(
            project,
            Bundle("notifications.settings_error.parse_error.title"),
            Bundle("notifications.settings_error.parse_error.message")
        )

    /**
     * Shows a notification through which users can troubleshoot issues with their settings file.
     *
     * @param project the project in which the error notification should be displayed
     * @param title the title to show in the notification
     * @param message the message to display in the notification
     */
    private fun showSettingsErrorNotification(project: Project? = null, title: String, message: String) =
        showNotification(
            project,
            NotificationType.ERROR,
            "com.fwdekker.randomness.settings_errors",
            title,
            message,
            SimpleNotificationAction(Bundle("notifications.settings_error.notification.open_plugin_manager")) { _, _ ->
                ShowSettingsUtil.getInstance().showSettingsDialog(project, PluginManagerConfigurable::class.java)
            },
            SimpleNotificationAction(Bundle("notifications.settings_error.notification.backup_and_reset")) { _, notification ->
                DialogBuilder().apply {
                    setTitle(Bundle("notifications.settings_error.backup_and_reset.title"))
                    setCenterPanel(JBLabel("<html>${Bundle("notifications.settings_error.backup_and_reset.message")}</html>"))
                    addActionDescriptor(SimpleDialogAction(Bundle("notifications.settings_error.backup.title")) { _, _ ->
                        FileChooserFactory.getInstance()
                            .createSaveFileDialog(
                                FileSaverDescriptor(
                                    Bundle("notifications.settings_error.backup.chooser.title"),
                                    Bundle("notifications.settings_error.backup.chooser.message"),
                                    "xml"
                                ), project
                            )
                            .save("randomness3.xml")
                            ?.also { chosenFile ->
                                PathManager.getOptionsFile("randomness3")
                                    .copyTo(chosenFile.file, overwrite = true)
                                showMessage(
                                    project,
                                    Bundle("notifications.settings_error.backup.success.title"),
                                    Bundle("notifications.settings_error.backup.success.message")
                                )
                            }
                    })
                    addActionDescriptor(SimpleDialogAction(Bundle("notifications.settings_error.delete.title")) { _, dialog ->
                        if (askYesNo(
                                project,
                                Bundle("notifications.settings_error.delete.confirm.title"),
                                Bundle("notifications.settings_error.delete.confirm.message")
                            )
                        ) {
                            PathManager.getOptionsFile("randomness3").delete()
                            service<PersistentSettings>().resetState()

                            showMessage(
                                project,
                                Bundle("notifications.settings_error.delete.success.title"),
                                Bundle("notifications.settings_error.delete.success.message")
                            )
                            dialog.close(0)
                            notification.hideBalloon()
                        }
                    })
                    addCancelAction()
                }.showModal(false)
            }
        )


    /**
     * Shows a notification to the user.
     *
     * @param project the project in which the notification should be displayed
     * @param type the type of notification
     * @param group the group to which this notification belongs
     * @param title the title of the notification
     * @param message the text shown inside the notification
     * @param actions the actions that are suggested to the user inside the notification
     * @param isSuggestionType `true` if the notification is a suggestion to the user
     */
    private fun showNotification(
        project: Project?,
        type: NotificationType,
        group: String,
        title: String,
        message: String,
        vararg actions: NotificationAction,
        isSuggestionType: Boolean = false,
    ) =
        NotificationGroupManager.getInstance()
            .getNotificationGroup(group)
            .createNotification(Bundle("randomness"), message, type)
            .setSubtitle(title)
            .setSuggestionType(isSuggestionType)
            .apply { if (type !in listOf(NotificationType.WARNING, NotificationType.ERROR)) setIcon(Icons.RANDOMNESS) }
            .apply { actions.forEach { action -> addAction(action) } }
            .notify(project)

    /**
     * Shows a simple yes/no modal dialog and returns `true` if and only if the user clicked yes.
     *
     * @param project the project in which the dialog should be displayed
     * @param title the title to display in the title bar
     * @param message the message to display inside the dialog
     */
    private fun askYesNo(
        project: Project?,
        title: String,
        message: String = "",
    ): Boolean = MessageDialogBuilder.yesNo(title, message, Icons.RANDOMNESS).ask(project)

    /**
     * Shows a simple text message to the user in a modal dialog.
     *
     * @param project the project in which the dialog should be displayed
     * @param title the title to display in the title bar
     * @param message the message to display inside the dialog
     */
    private fun showMessage(
        project: Project?,
        title: String,
        message: String,
    ) = MessageDialog(project, message, title, arrayOf("OK"), 0, null, false).show()


    /**
     * An action that can be run from a notification.
     *
     * @param name the name of the action
     * @param action the code to execute when the action is run
     */
    private class SimpleNotificationAction(
        name: String,
        private val action: (AnActionEvent, Notification) -> Unit,
    ) : NotificationAction(name) {
        override fun actionPerformed(event: AnActionEvent, notification: Notification) = action(event, notification)
    }

    /**
     * An action that can be run from a dialog button.
     *
     * @param name the name of the action
     * @param action the code to execute when the action is run
     */
    private class SimpleDialogAction(
        private val name: String,
        private val action: (ActionEvent, DialogWrapper) -> Unit,
    ) : DialogBuilder.ActionDescriptor {
        override fun getAction(dialog: DialogWrapper): Action =
            object : AbstractAction(name) {
                override fun actionPerformed(event: ActionEvent) = action(event, dialog)
            }
    }
}


/**
 * Displays notifications when a project is opened.
 */
internal class StartupNotifier : ProjectActivity {
    /**
     * Displays notifications when a project is opened.
     */
    override suspend fun execute(project: Project) = Notifier.showWelcomeToV3(project)
}
