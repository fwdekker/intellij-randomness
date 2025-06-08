package com.fwdekker.randomness

import com.fwdekker.randomness.PersistentSettings.Companion.CURRENT_VERSION
import com.fwdekker.randomness.PersistentSettings.Companion.UPGRADES
import com.fwdekker.randomness.template.Template
import com.fwdekker.randomness.template.TemplateList
import com.fwdekker.randomness.ui.ValidatorDsl.Companion.validators
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.addOptionTag
import com.intellij.util.xmlb.XmlSerializer.deserialize
import com.intellij.util.xmlb.XmlSerializer.serialize
import com.intellij.util.xmlb.annotations.OptionTag
import org.jdom.Element
import java.lang.module.ModuleDescriptor.Version
import com.intellij.openapi.components.State as JBState


/**
 * Contains references to various [State] objects.
 *
 * @property version The version of Randomness with which these settings were created.
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
         */
        val DEFAULT: Settings
            get() = service<PersistentSettings>().settings
    }
}

/**
 * The persistent [Settings] instance, stored as an [Element] to allow custom conversion for backwards compatibility.
 *
 * @see Settings.DEFAULT Preferred method of accessing the persistent settings instance.
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
     * The [Settings] that should be persisted.
     *
     * @see Settings.DEFAULT Preferred method of accessing the persistent settings instance.
     */
    var settings = Settings()


    /**
     * Returns the [settings] as an [Element].
     */
    override fun getState(): Element = serialize(settings)

    /**
     * Deserializes [element] into a [Settings] instance, which is then stored in [settings].
     */
    override fun loadState(element: Element) {
        settings = deserialize(upgrade(element), Settings::class.java)
    }


    /**
     * Upgrades the format of the settings contained in [element] to the format of the [targetVersion].
     *
     * @see UPGRADES
     */
    internal fun upgrade(element: Element, targetVersion: Version = Version.parse(CURRENT_VERSION)): Element {
        require(targetVersion >= Version.parse("3.0.0")) { "Unsupported upgrade target version $targetVersion." }

        val elementVersion = element.getPropertyValue("version")?.let { Version.parse(it) }
        requireNotNull(elementVersion) { "Missing version number in Randomness settings." }
        require(elementVersion >= Version.parse("3.0.0")) { "Unsupported Randomness config version $elementVersion." }

        if (targetVersion <= elementVersion) return element

        UPGRADES
            .filterKeys { elementVersion < it && targetVersion >= it }
            .forEach { (_, it) -> it(element) }

        element.setPropertyValue("version", targetVersion.toString())
        return element
    }


    /**
     * Holds constants.
     */
    companion object {
        /**
         * The currently-running version of Randomness.
         */
        const val CURRENT_VERSION: String = "3.3.6-dev" // Synchronize this with the version in `gradle.properties`

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
                                        prop.removeAttribute("value")

                                        val newValue = Timestamp.fromEpochMilli(oldValue).value
                                        prop.addContent(Element("Timestamp").apply { addOptionTag("value", newValue) })
                                    }
                            }
                    },
            )
    }
}
