@file:Suppress("DEPRECATION") // Required for [ErrorReportSubmitter].

package com.fwdekker.randomness

import com.intellij.diagnostic.AbstractMessage
import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.diagnostic.SubmittedReportInfo.SubmissionStatus.DUPLICATE
import com.intellij.openapi.diagnostic.SubmittedReportInfo.SubmissionStatus.FAILED
import com.intellij.openapi.diagnostic.SubmittedReportInfo.SubmissionStatus.NEW_ISSUE
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.Consumer
import org.eclipse.egit.github.core.Issue
import org.eclipse.egit.github.core.RepositoryId
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.client.PageIterator
import org.eclipse.egit.github.core.service.IssueService
import java.awt.Component
import java.io.File
import java.net.URI
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


/**
 * Reports exceptions on GitHub.
 *
 * Heavily inspired by [Patrick Scheibe's error reporter](https://github.com/halirutan/Wolfram-Language-IntelliJ-Plugin-Archive/tree/e3dd72f9cd344d678ac892aaa7bf59abd84871e8/src/de/halirutan/mathematica/errorreporting).
 */
@Suppress("detekt:MaxLineLength") // Necessary because of the long link in the docs above
internal class ErrorReporter : ErrorReportSubmitter() {
    /**
     * Interacts with GitHub.
     */
    private val github = GitHubReporter()


    /**
     * Returns the text that is displayed in the button to report the error.
     */
    override fun getReportActionText() = Bundle("reporter.report")

    /**
     * Returns the privacy notice text.
     */
    override fun getPrivacyNoticeText() = Bundle("reporter.privacy_notice")

    /**
     * Submits the exception by opening the browser to create an issue on GitHub.
     *
     * @param events the events to report
     * @param additionalInfo additional information provided by the user
     * @param parentComponent ignored
     * @param consumer the callback to invoke afterwards
     * @return `true`
     */
    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>,
    ): Boolean {
        ProgressManager.getInstance()
            .run(object : Backgroundable(null, Bundle("reporter.task.title"), false) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = true

                    val report = github.report(IssueData(events.asList(), additionalInfo, pluginDescriptor))
                    consumer.consume(report)
                    notifyUser(report)
                }
            })

        return true
    }

    /**
     * Displays a notification to the user explaining the [report].
     */
    private fun notifyUser(report: SubmittedReportInfo) =
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Error Report")
            .run {
                if (report.status == FAILED)
                    createNotification(
                        Bundle("reporter.notify.title.failure"),
                        report.linkText,
                        NotificationType.ERROR,
                    )
                else
                    createNotification(
                        Bundle("reporter.notify.title.success"),
                        if (report.status == DUPLICATE) Bundle("reporter.notify.description.duplicate")
                        else Bundle("reporter.notify.description.new"),
                        NotificationType.INFORMATION,
                    )
            }
            .setIcon(Icons.RANDOMNESS)
            .setImportant(false)
            .apply {
                if (report.status == FAILED) {
                    addAction(object : NotificationAction(Bundle("reporter.notify.error.report_manually")) {
                        override fun actionPerformed(event: AnActionEvent, notification: Notification) =
                            BrowserUtil.browse(Bundle("reporter.notify.error.url"))
                    })
                } else {
                    addAction(object : NotificationAction(Bundle("reporter.notify.view_in_browser")) {
                        override fun actionPerformed(event: AnActionEvent, notification: Notification) =
                            BrowserUtil.browse(report.url)
                    })
                }
            }
            .notify(null)
}


/**
 * Knows how to report [IssueData] to GitHub.
 */
private class GitHubReporter {
    /**
     * The repository to open issues in.
     */
    private val repo = RepositoryId(GIT_REPO_USER, GIT_REPO)

    /**
     * Service for interacting with the issues API on GitHub.
     */
    private val issueService by lazy {
        IssueService(GitHubClient().also { it.setOAuth2Token(GitHubScrambler.getToken()) })
    }


    /**
     * Attempts to report [issueData] to GitHub.
     */
    fun report(issueData: IssueData): SubmittedReportInfo =
        try {
            synchronized(this) {
                val duplicate = issueService.pageIssues(repo, mapOf("state" to "all"))
                    .flatSequence()
                    .firstOrNull { issueData.isDuplicateOf(it) }

                val context: Issue
                if (duplicate == null) {
                    context = issueService.createIssue(repo, issueData.asGitHubIssue())
                } else {
                    issueService.createComment(repo, duplicate.number, issueData.body)
                    context = duplicate
                }

                SubmittedReportInfo(
                    context.htmlUrl,
                    Bundle(
                        if (duplicate == null) "reporter.report.new" else "reporter.report.duplicate",
                        context.htmlUrl,
                        context.number,
                    ),
                    if (duplicate == null) NEW_ISSUE else DUPLICATE
                )
            }
        } catch (_: Exception) {
            SubmittedReportInfo(null, Bundle("reporter.report.error"), FAILED)
        }


    /**
     * Holds constants.
     */
    companion object {
        /**
         * The name of the user that owns the repo to report errors in.
         */
        private const val GIT_REPO_USER = "FWDekkerBot"

        /**
         * The repository to report errors in.
         */
        private const val GIT_REPO = "intellij-randomness-issues"


        /**
         * Returns a flattened sequence of all elements in the iterator.
         */
        private fun <V> PageIterator<V>.flatSequence(): Sequence<V> =
            (this as Iterable<Collection<V>>).asSequence().flatten()
    }
}

/**
 * A GitHub authentication token that is slightly scrambled (and NOT SECURE).
 *
 * Though the scrambling uses encryption, it is not actually stored securely, and can be obtained relatively easily
 * by others. Even if [GitHubScrambler.KEY] and [GitHubScrambler.IV] were not stored in plaintext, you would eventually
 * have to leak the plaintext token anyway. There is no way around this.
 *
 * Assume the token is public knowledge: It may be stolen and abused, and it is your responsibility to ensure that
 * the potential for harm is minimised: Use a fine-grained access token that is limited to read/write access for a
 * single repository of a separate user. Do not use your main repo to store issues in, unless you're fine with the worst
 * case of all your issues being deleted. Subscribe to every event in the repository so that you will notice when the
 * token is being abused for spam.
 *
 * The token is retrieved from a repository, because tokens are only valid for up to one year, but users should be able
 * to report issues even if there has not been an update for more than a year. This means the [GitHubScrambler.KEY] and
 * [GitHubScrambler.IV] must not be refreshed between updates, as otherwise previous versions will be unable to
 * unscramble the token.
 */
private object GitHubScrambler {
    /**
     * The IV to use for (un)scrambling.
     */
    private const val IV = "DefinitelyGoodIV"

    /**
     * The "private" key to use for (un)scrambling.
     */
    private const val KEY = "GreatScrambleKey"

    /**
     * The IV specification to use for (un)scrambling.
     */
    private val IV_SPEC = IvParameterSpec(IV.toByteArray(charset("UTF-8")))

    /**
     * The key specification to use for (un)scrambling.
     */
    private val KEY_SPEC = SecretKeySpec(KEY.toByteArray(charset("UTF-8")), "AES")

    /**
     * The URL at which a newer token may be available.
     */
    private val URL =
        URI("https://raw.githubusercontent.com/FWDekker/intellij-randomness/main/src/main/resources/reporter/token.bin")
            .toURL()


    /**
     * Instantiates a [Cipher] for (un)scrambling a token.
     */
    private fun createCipher(): Cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")

    /**
     * Reads the unscrambled token.
     */
    fun getToken(): String =
        unscramble(URL.readText())
            .also { require(it.startsWith("github")) { "Invalid token after unscrambling." } }

    /**
     * Unscrambles the given [scrambledToken].
     */
    fun unscramble(scrambledToken: String): String =
        createCipher()
            .also { it.init(Cipher.DECRYPT_MODE, KEY_SPEC, IV_SPEC) }
            .doFinal(Base64.getDecoder().decode(scrambledToken.trim().toByteArray()))
            .let { String(it).trim() }

    /**
     * Scrambles the given [token].
     */
    fun scramble(token: String): String =
        createCipher()
            .also { it.init(Cipher.ENCRYPT_MODE, KEY_SPEC, IV_SPEC) }
            .doFinal(token.trim().toByteArray())
            .let { String(Base64.getEncoder().encode(it)) }


    /**
     * Runs an interactive session to scramble a token into a file. Run with `gradlew run --console=plain`.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        val target = File("src/main/resources/reporter/token.bin")

        print("Enter token to scramble: ")
        val token = readln()

        target.writeText(scramble(token))
        require(unscramble(target.readText()) == token) { "Stored token does not match input token." }

        println("Scrambled token has been stored in '${target.absolutePath}'.")
    }
}

/**
 * Contains a variety of metadata on an issue to report, and knows how to format the issue in a textual form.
 *
 * @property events The events to report.
 * @property additionalInfo Additional information provided by the user.
 * @property pluginDescriptor The descriptor of Randomness.
 */
private class IssueData(
    val events: List<IdeaLoggingEvent>,
    val additionalInfo: String?,
    val pluginDescriptor: PluginDescriptor,
) {
    /**
     * Event data for the [events].
     */
    private val eventData: List<AbstractMessage> =
        events.map { it.data }.filterIsInstance<AbstractMessage>()

    /**
     * The hash that identifies this issue, used for duplication detection.
     */
    private val hash: String =
        eventData.map { it.throwable.stackTrace.contentHashCode() }.hashCode().toUInt().toString(radix = 16)

    /**
     * The list of included attachments.
     */
    private val attachments: List<Attachment> =
        eventData.flatMap { it.allAttachments }.filter { it.isIncluded }


    /**
     * Returns the title of the issue.
     */
    val title: String =
        Bundle(
            "reporter.issue.title",
            hash,
            eventData.firstNotNullOfOrNull { it.throwable.message }
                ?: events.firstNotNullOfOrNull { it.message }
                ?: Bundle("reporter.issue.title.unknown"),
        )

    /**
     * The body of the issue.
     */
    val body: String =
        emptySequence<Pair<String, String>>()
            .plus(
                Bundle("reporter.issue.body.comments.title") to
                    additionalInfo.ifNullOrBlank { italic(Bundle("reporter.issue.body.comments.body_empty")) }.trim()
            )
            .plus(
                events
                    .map { it.throwableText }
                    .filterNot { it.isBlank() }
                    .mapIndexed { idx: Int, body: String ->
                        Bundle("reporter.issue.body.stacktrace.title", idx + 1) to
                            spoiler(code(body, "java"))
                    }
            )
            .plus(
                attachments.map {
                    Bundle("reporter.issue.body.attachment.title", it.name) to
                        spoiler(code(it.displayText))
                }
            )
            .plus(
                Bundle("reporter.issue.body.version.title") to
                    """
                    - Randomness version: ${pluginDescriptor.version ?: "_Unknown_"}
                    - Randomness settings version: ${PersistentSettings.CURRENT_VERSION}
                    - IDE version: ${ApplicationInfo.getInstance().apiVersion}
                    - Operating system: ${SystemInfo.OS_NAME}
                    """.trimIndent()
            )
            .joinToString(separator = "\n\n") { section(it.first, it.second) }
            .let { italic(Bundle("reporter.issue.body.header")) + "\n\n---\n\n" + it }


    /**
     * Returns the corresponding [Issue].
     */
    fun asGitHubIssue(): Issue =
        Issue()
            .also {
                it.title = title
                it.body = body
            }

    /**
     * Returns `true` if and only if this [IssueData] is (likely) a duplicate of the existing [Issue].
     */
    fun isDuplicateOf(other: Issue): Boolean =
        this.title.takeWhile { it != ']' } == other.title.takeWhile { it != ']' }


    /**
     * Holds constants.
     */
    companion object {
        /**
         * Creates a Markdown code block containing [body] and using the given [language].
         */
        private fun code(body: String, language: String = ""): String =
            "```$language\n$body\n```"

        /**
         * Creates an italicized piece of Markdown.
         */
        private fun italic(body: String) = "_${body}_"

        /**
         * Creates a Markdown section with the given [title] and [body].
         */
        private fun section(title: String, body: String): String =
            "**${title.trim()}**\n${body.trim()}"

        /**
         * Creates a Markdown spoiler tag with the given [heading] and [body].
         */
        private fun spoiler(body: String, heading: String = "Click to show"): String =
            "<details>\n  <summary>${heading.trim()}</summary>\n\n${body.prependIndent("  ")}\n\n</details>"


        /**
         * Returns [this] if [this] is neither `null` nor blank, and returns the output of [then] otherwise.
         */
        private fun String?.ifNullOrBlank(then: () -> String): String =
            if (this.isNullOrBlank()) then() else this
    }
}
