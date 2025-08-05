package com.fwdekker.randomness

import com.fwdekker.randomness.SettingsFileManager.showRepairNotification
import com.fwdekker.randomness.ui.SimpleNotificationAction
import com.fwdekker.randomness.ui.askYesNo
import com.fwdekker.randomness.ui.showNotification
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.lang.module.ModuleDescriptor.Version


/**
 * Displays notifications to the user.
 */
internal object Notifier {
    /**
     * Shows a notification introducing the user to version 3 of Randomness if the user previously used Randomness 2.
     */
    fun showWelcomeToV3(project: Project) {
        val key = "notifications.welcome_to_v3"
        val oldSettings = PathManager.getOptionsFile("randomness")

        val propComp = PropertiesComponent.getInstance()
        val propKey = "com.fwdekker.randomness.notifications.welcome_to_v3"
        if (propComp.isTrueValue(propKey) || !oldSettings.exists())
            return

        showNotification(
            project,
            NotificationType.INFORMATION,
            "com.fwdekker.randomness.updates",
            Bundle("$key.title"),
            Bundle("$key.message"),
            SimpleNotificationAction(Bundle("$key.delete_old_settings")) { _, notification ->
                if (askYesNo(project, Bundle("$key.delete_old_settings_confirm"))) {
                    oldSettings.delete()
                    propComp.setValue(propKey, true)
                    notification.hideBalloon()
                }
            },
            SimpleNotificationAction(Bundle("$key.do_not_ask_again")) { _, notification ->
                propComp.setValue(propKey, true)
                notification.hideBalloon()
            },
            isSuggestion = true,
        )
    }


    /**
     * Shows a notification informing the user that the settings file has an unsupported version from the future and
     * cannot be used.
     *
     * @param futureVersion the version to which the user must update Randomness to ensure the settings file can be read
     * @param project the project in which the error notification should be displayed
     */
    fun showFutureSettingsError(futureVersion: Version, project: Project? = null) {
        val key = "notifications.settings_error.future_error"
        showRepairNotification(project, Bundle("$key.title"), Bundle("$key.message", futureVersion.toString()))
    }

    /**
     * Shows a notification informing the user that the settings could not be parsed and allows the user to delete their
     * settings file.
     *
     * @param project the project in which the error notification should be displayed
     */
    fun showParseSettingsError(project: Project? = null) {
        val key = "notifications.settings_error.parse_error"
        showRepairNotification(project, Bundle("$key.title"), Bundle("$key.message"))
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
