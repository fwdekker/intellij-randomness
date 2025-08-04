package com.fwdekker.randomness

import com.fwdekker.randomness.PersistentSettings.Companion.CURRENT_VERSION
import com.fwdekker.randomness.PersistentSettings.Companion.UPGRADES
import com.fwdekker.randomness.template.Template
import com.fwdekker.randomness.template.TemplateList
import com.fwdekker.randomness.ui.ValidatorDsl.Companion.validators
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.ExceptionWithAttachments
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.xmlb.XmlSerializer.deserialize
import com.intellij.util.xmlb.XmlSerializer.serialize
import com.intellij.util.xmlb.annotations.OptionTag
import org.jdom.Element
import java.lang.module.ModuleDescriptor.Version
import com.intellij.openapi.components.State as JBState


/**
 * Contains references to various [State] objects.
 *
 * @property version The oldest version of Randomness with which these settings are compatible.
 * @property templateList The template list.
 */
data class Settings(
    var version: String = CURRENT_VERSION,
    @OptionTag
    val templateList: TemplateList = TemplateList(),
) : State() {
    override val validators = validators { include(::templateList) }

    /**
     * @see TemplateList.templates
     */
    val templates: MutableList<Template> get() = templateList.templates


    init {
        applyContext(this)
    }


    override fun applyContext(context: Box<Settings>) {
        super.applyContext(context)
        templateList.applyContext(context)
    }


    override fun deepCopy(retainUuid: Boolean) =
        copy(templateList = templateList.deepCopy(retainUuid = retainUuid))
            .deepCopyTransient(retainUuid)
            .also { it.applyContext(it) }


    /**
     * Holds constants.
     */
    companion object {
        /**
         * The persistent [Settings] instance.
         *
         * If the user's settings file could not be loaded, this will instead return a default [Settings] object,
         * changes to which will not be persisted in the settings file.
         */
        val DEFAULT: Settings
            get() = service<PersistentSettings>().settings ?: Settings()
    }
}

/**
 * The persistent [Settings] instance, stored as an [Element] to allow custom conversion for backwards compatibility.
 *
 * @see Settings.DEFAULT Preferred method of accessing the persistent [Settings] instance.
 */
@JBState(
    name = "Randomness",
    storages = [
        Storage("randomness-beta.xml", deprecated = true, exportable = true),
        Storage("randomness3.xml", exportable = true),
    ],
    category = SettingsCategory.PLUGINS,
)
internal class PersistentSettings : PersistentStateComponent<Element> {
    /**
     * Contains the state passed to [loadState] if [loadState] failed to deserialize or upgrade the given state, and
     * contains `null` otherwise.
     *
     * @see getState
     */
    private var oldState: Element? = null

    /**
     * The [Settings] that should be persisted, or `null` if [loadState] failed to deserialize or upgrade the given
     * state.
     *
     * @see Settings.DEFAULT Preferred method of accessing the persistent settings instance.
     */
    var settings: Settings? = Settings()


    /**
     * Returns the [settings] as an [Element].
     *
     * If [loadState] succeeded, this returns the serialization of [settings], including any changes made by the user.
     * However, if [loadState] was not successful, then user changes are truncated, and the original state that was
     * passed to [loadState] is returned instead.
     */
    override fun getState(): Element = settings?.let { serialize(it) } ?: oldState!!

    /**
     * Deserializes [element] into a [Settings] instance, which is then stored in [settings].
     */
    @Suppress("detekt:TooGenericExceptionCaught") // All exceptions should be wrapped
    override fun loadState(element: Element) {
        try {
            settings = deserialize(upgrade(element), Settings::class.java)
            oldState = null

            LOG.debug("Successfully loaded state.")
        } catch (exception: FutureSettingsException) {
            settings = null
            oldState = element

            LOG.debug("Failed to load state: FutureSettingsException.")
            Notifier.showFutureSettingsErrorNotification(exception.version)
        } catch (exception: Exception) {
            settings = null
            oldState = element

            LOG.debug("Failed to load state: Exception.", exception)
            Notifier.showParseSettingsErrorNotification()
            throw ParseSettingsException("Failed to parse or upgrade settings file.", exception)
        }
    }

    fun resetState() {
        oldState = null
        settings = Settings()
    }


    /**
     * Upgrades the format of the settings contained in [element] to the newest format compatible with [targetVersion].
     *
     * @see UPGRADES
     */
    internal fun upgrade(element: Element, targetVersion: Version = Version.parse(CURRENT_VERSION)): Element {
        require(targetVersion >= Version.parse("3.0.0")) { "Unsupported upgrade target version $targetVersion." }

        val elementVersion = element.getPropertyValue("version")?.let { Version.parse(it) }
        requireNotNull(elementVersion) { "Missing version number in Randomness settings file." }
        require(elementVersion >= Version.parse("3.0.0")) {
            "Unsupported old version $elementVersion in Randomness settings file."
        }
        if (elementVersion > UPGRADES.keys.max())
            throw FutureSettingsException(
                elementVersion,
                "Unsupported future version $targetVersion in Randomness settings file."
            )

        UPGRADES
            .filterKeys { elementVersion < it && targetVersion >= it }
            .forEach { (version, it) ->
                it(element)
                element.setPropertyValue("version", version.toString())
            }

        return element
    }


    /**
     * Holds constants.
     */
    companion object {
        /**
         * The logger to log events to.
         */
        val LOG: Logger = Logger.getInstance(PersistentSettings::class.java)

        /**
         * The currently-running version of Randomness.
         *
         * Update this value *only* if something has changed in the serialization format of the settings.
         */
        const val CURRENT_VERSION: String = "3.4.0" // Synchronize this with the version in `gradle.properties`

        /**
         * The upgrade functions to apply to configuration [Element]s in the [upgrade] method.
         *
         * Each entry in this map consists of an upgrade function that mutates an [Element] so that it is upgrades to
         * the [Version] specified in the entry's key. That is, the key denotes the [Version] number of that entry's
         * output, not of its input.
         */
        private val UPGRADES: Map<Version, (Element) -> Unit> =
            mapOf(
                Version.parse("3.2.0") to
                    { settings ->
                        settings.getSchemes()
                            .filter { it.name == "UuidScheme" }
                            .forEach { it.renameProperty("type", "version") }
                    },
                Version.parse("3.3.5") to
                    { settings -> settings.getDecorators().forEach { it.removeProperty("generator") } },
                Version.parse("3.4.0") to
                    { settings ->
                        settings.getSchemes()
                            .filter { it.name == "DateTimeScheme" }
                            .forEach { scheme ->
                                (scheme.getMultiProperty("minDateTime") + scheme.getMultiProperty("maxDateTime"))
                                    .forEach { prop ->
                                        val oldValue = prop.getAttributeValue("value").toLong()
                                        prop.setAttribute("value", Timestamp.fromEpochMilli(oldValue).value)
                                    }
                            }
                    },
            )
    }
}


/**
 * Indicates that settings could not be parsed for one reason or another.
 */
class ParseSettingsException(message: String? = null, cause: Throwable? = null) :
    IllegalArgumentException(message, cause), ExceptionWithAttachments {
    /**
     * Returns the user's Randomness settings file as an attachment, if it can be read.
     */
    override fun getAttachments(): Array<out Attachment?> {
        val path = PathManager.getOptionsFile("randomness3")
        val contents = if (path.canRead()) path.readText() else "Settings file could not be read."

        return arrayOf(Attachment("randomness3.xml", contents))
    }
}

/**
 * Indicates that settings could not be parsed correctly because settings from a future release of Randomness were
 * loaded.
 *
 * @property version The version from the future that was encountered.
 */
class FutureSettingsException(val version: Version, message: String? = null) : Exception(message)
