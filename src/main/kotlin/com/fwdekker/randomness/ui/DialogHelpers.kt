package com.fwdekker.randomness.ui

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.Icons
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.messages.MessageDialog
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.Icon


/**
 * Shows a balloon notification to the user.
 *
 * @param project the project in which the notification should be displayed
 * @param type the type of notification
 * @param group the group to which this notification belongs
 * @param title the title of the notification
 * @param message the text shown inside the notification
 * @param actions the actions that are suggested to the user inside the notification
 * @param isSuggestion `true` if the notification is a suggestion to the user
 * @param isImportant `true` if the notification is important to the user
 */
@Suppress("detekt:LongParameterList") // Not worth the effort to refactor
fun showNotification(
    project: Project?,
    type: NotificationType,
    group: String,
    title: String,
    message: String,
    vararg actions: NotificationAction,
    isSuggestion: Boolean = false,
    isImportant: Boolean = false,
) =
    NotificationGroupManager.getInstance()
        .getNotificationGroup(group)
        .createNotification(Bundle("randomness"), message, type)
        .setSubtitle(title)
        .setSuggestionType(isSuggestion)
        .setImportant(isImportant)
        .setIcon(if (type in listOf(NotificationType.WARNING, NotificationType.ERROR)) null else Icons.RANDOMNESS)
        .apply { actions.forEach { addAction(it) } }
        .notify(project)

/**
 * Shows a simple yes/no modal dialog and returns `true` if and only if the user clicked yes.
 *
 * @param project the project in which the dialog should be displayed
 * @param title the title to display in the title bar
 * @param message the message to display inside the dialog
 */
fun askYesNo(
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
 * @param icon the icon to display next to the [message]
 */
fun showDialogMessage(
    project: Project?,
    title: String,
    message: String,
    icon: Icon? = null,
) = MessageDialog(project, message, title, arrayOf("OK"), 0, icon, false).show()


/**
 * An action that can be run from a dialog button.
 *
 * @param name the name of the action
 * @param action the code to execute when the action is run
 */
class SimpleDialogAction(
    private val name: String,
    private val action: (ActionEvent, DialogWrapper) -> Unit,
) : DialogBuilder.ActionDescriptor {
    /**
     * Returns the action that should be run from the dialog button.
     *
     * @param dialog the dialog on which to run the action
     */
    override fun getAction(dialog: DialogWrapper): Action =
        object : AbstractAction(name) {
            override fun actionPerformed(event: ActionEvent) =
                action(event, dialog)
        }
}

/**
 * An action that can be run from a notification.
 *
 * @param name the name of the action
 * @param action the code to execute when the action is run
 */
class SimpleNotificationAction(
    name: String,
    private val action: (AnActionEvent, Notification) -> Unit,
) : NotificationAction(name) {
    /**
     * Runs the [action].
     *
     * @param event the event that triggered this action to run
     * @param notification the notification on which the action is run
     */
    override fun actionPerformed(event: AnActionEvent, notification: Notification) =
        action(event, notification)
}
