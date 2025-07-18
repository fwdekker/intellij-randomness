package com.fwdekker.randomness

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.ui.MessageDialogBuilder


/**
 * Displays notifications when a project is opened.
 */
internal class StartupNotifier : ProjectActivity {
    /**
     * Displays notifications when a project is opened.
     */
    override suspend fun execute(project: Project) = showWelcomeToV3(project)

    /**
     * Shows a notification introducing the user to version 3 of Randomness if the user previously used Randomness 2.
     */
    private fun showWelcomeToV3(project: Project) {
        val key = "notifications.welcome_to_v3"
        val oldConfig = PathManager.getOptionsFile("randomness")

        val propComp = PropertiesComponent.getInstance()
        val propKey = "com.fwdekker.randomness.$key"
        if (propComp.isTrueValue(propKey) || !oldConfig.exists())
            return

        NotificationGroupManager.getInstance()
            .getNotificationGroup("com.fwdekker.randomness.updates")
            .createNotification(Bundle("$key.title"), Bundle("$key.content"), NotificationType.INFORMATION)
            .setSubtitle(Bundle("$key.subtitle"))
            .setIcon(Icons.RANDOMNESS)
            .setSuggestionType(true)
            .addAction(object : NotificationAction(Bundle("$key.delete_old_config")) {
                override fun actionPerformed(event: AnActionEvent, notification: Notification) {
                    MessageDialogBuilder
                        .yesNo(Bundle("$key.delete_old_config_confirm"), "", Icons.RANDOMNESS)
                        .ask(project)
                        .also {
                            if (it) {
                                oldConfig.delete()
                                propComp.setValue(propKey, true)
                                notification.hideBalloon()
                            }
                        }
                }
            })
            .addAction(object : NotificationAction(Bundle("$key.do_not_ask_again")) {
                override fun actionPerformed(event: AnActionEvent, notification: Notification) {
                    propComp.setValue(propKey, true)
                    notification.hideBalloon()
                }
            })
            .notify(project)
    }
}
