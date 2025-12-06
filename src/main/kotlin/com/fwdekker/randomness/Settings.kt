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
    var version: String = CURRENT_VERSION.toString(),
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
    private var loadedState: Element? = null

    /**
     * The [Settings] that should be persisted, or `null` if [loadState] failed to deserialize or upgrade the given
     * state.
     *
     * @see Settings.DEFAULT Preferred method of accessing the persistent settings instance.
     */
    var settings: Settings? = Settings()


    /**
     * Deserializes [element] into a [Settings] instance, which is then stored in [settings].
     */
    @Suppress("detekt:TooGenericExceptionCaught") // All exceptions should be wrapped
    override fun loadState(element: Element) {
        try {
            settings = deserialize(upgrade(element), Settings::class.java)
            loadedState = null
        } catch (exception: FutureSettingsException) {
            settings = null
            loadedState = element

            Notifier.showFutureSettingsError(exception.version)
        } catch (exception: Exception) {
            settings = null
            loadedState = element

            Notifier.showParseSettingsError()
            throw ParseSettingsException("Failed to parse or upgrade settings file.", exception)
        }
    }

    /**
     * Returns the [settings] as an [Element].
     *
     * If [loadState] succeeded, this returns the serialization of [settings], including any changes made by the user.
     * However, if [loadState] was not successful, then user changes are truncated, and the original state that was
     * passed to [loadState] is returned instead.
     *
     * You should not modify the returned value. To modify settings, use [settings] instead.
     */
    override fun getState(): Element = settings?.let { serialize(it) } ?: loadedState!!

    /**
     * Resets all settings, including any embedded invalidity states.
     */
    fun resetState() {
        loadedState = null
        settings = Settings()
    }


    /**
     * Upgrades the format of the settings contained in [element] to the newest format compatible with [targetVersion].
     *
     * @see UPGRADES
     */
    internal fun upgrade(element: Element, targetVersion: Version = CURRENT_VERSION): Element {
        require(targetVersion >= Version.parse("3.0.0")) { "Unsupported old upgrade target version $targetVersion." }
        require(targetVersion <= CURRENT_VERSION) { "Unsupported future upgrade target version $targetVersion." }

        val elementVersion = element.getPropertyValue("version")?.let { Version.parse(it) }
        requireNotNull(elementVersion) { "Missing version number in Randomness settings file." }
        require(elementVersion >= Version.parse("3.0.0")) {
            "Unsupported old version $elementVersion in Randomness settings file."
        }
        if (elementVersion > CURRENT_VERSION) throw FutureSettingsException(elementVersion)

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
                Version.parse("3.4.2") to
                    { settings ->
                        settings.getSchemes()
                            .filter { it.name == "UuidScheme" }
                            .forEach { scheme ->
                                val min = Timestamp("1970-01-01 00:00:00.000")
                                scheme.getMultiProperty("minDateTime")
                                    .forEach { prop ->
                                        if (Timestamp(prop.getAttributeValue("value")).isBefore(min))
                                            prop.setAttribute("value", min.value)
                                    }

                                val max = Timestamp("5236-03-31 21:21:00.684")
                                scheme.getMultiProperty("maxDateTime")
                                    .forEach { prop ->
                                        if (Timestamp(prop.getAttributeValue("value")).isAfter(max))
                                            prop.setAttribute("value", max.value)
                                    }
                            }
                    },
                Version.parse("3.5.0") to
                    { settings ->
                        // Migrate UuidScheme to UidScheme
                        settings.getSchemes()
                            .filter { it.name == "UuidScheme" }
                            .forEach { scheme ->
                                // Change scheme name from UuidScheme to UidScheme
                                scheme.name = "UidScheme"

                                // Add idTypeKey property set to "uuid"
                                scheme.addProperty("idTypeKey", "uuid")

                                // Wrap existing UUID properties into uuidConfig
                                val uuidConfigElement = Element("UuidConfig")

                                // Move UUID-specific properties to uuidConfig
                                listOf("version", "minDateTime", "maxDateTime", "isUppercase", "addDashes").forEach { propName ->
                                    scheme.getMultiProperty(propName).forEach { prop ->
                                        scheme.children.remove(prop)
                                        uuidConfigElement.addContent(prop.clone())
                                    }
                                }

                                // Add uuidConfig as a property
                                scheme.addContent(
                                    Element("option")
                                        .setAttribute("name", "uuidConfig")
                                        .addContent(uuidConfigElement)
                                )

                                // Add default nanoIdConfig
                                val nanoIdConfigElement = Element("NanoIdConfig")
                                scheme.addContent(
                                    Element("option")
                                        .setAttribute("name", "nanoIdConfig")
                                        .addContent(nanoIdConfigElement)
                                )
                            }

                        // Add default UUID and NanoID templates if they don't exist
                        val templatesElement = settings.getPropertyByPath("templateList", null, "templates", null)
                        if (templatesElement != null) {
                            val existingTemplateNames = settings.getTemplates()
                                .mapNotNull { it.getPropertyValue("name") }

                            // Add UUID template if not present
                            if ("UUID" !in existingTemplateNames) {
                                templatesElement.addContent(createUidTemplateElement("UUID", "uuid"))
                            }

                            // Add Nano ID template if not present
                            if ("Nano ID" !in existingTemplateNames) {
                                templatesElement.addContent(createUidTemplateElement("Nano ID", "nanoid"))
                            }
                        }
                    }
            )

        /**
         * The settings format version of Randomness.
         */
        val CURRENT_VERSION: Version = UPGRADES.keys.max()

        /**
         * Creates an XML Element representing a Template with a UidScheme.
         *
         * @param templateName The name of the template (e.g., "UUID" or "Nano ID")
         * @param idTypeKey The ID type key (e.g., "uuid" or "nanoid")
         */
        private fun createUidTemplateElement(templateName: String, idTypeKey: String): Element {
            val uidScheme = Element("UidScheme")
                .apply {
                    addProperty("idTypeKey", idTypeKey)
                    addContent(
                        Element("option")
                            .setAttribute("name", "uuidConfig")
                            .addContent(Element("UuidConfig"))
                    )
                    addContent(
                        Element("option")
                            .setAttribute("name", "nanoIdConfig")
                            .addContent(Element("NanoIdConfig"))
                    )
                }

            val schemesOption = Element("option")
                .setAttribute("name", "schemes")
                .addContent(
                    Element("list")
                        .addContent(uidScheme)
                )

            return Element("Template")
                .apply {
                    addProperty("name", templateName)
                    addContent(schemesOption)
                }
        }
    }
}


/**
 * Indicates that settings could not be parsed for one reason or another.
 *
 * @param message the detail message
 * @param cause the cause
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
class FutureSettingsException(val version: Version) :
    Exception("Unsupported future version $version in Randomness settings file.")
