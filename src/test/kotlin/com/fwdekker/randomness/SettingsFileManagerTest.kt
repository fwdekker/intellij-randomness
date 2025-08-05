package com.fwdekker.randomness

import com.fwdekker.randomness.template.Template
import com.fwdekker.randomness.template.TemplateList
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import com.fwdekker.randomness.testhelpers.from
import com.fwdekker.randomness.testhelpers.serializeToXmlString
import com.fwdekker.randomness.testhelpers.useBareIdeaFixture
import com.intellij.configurationStore.StoreUtil
import com.intellij.openapi.application.ApplicationManager
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.file.exist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.shouldNotBe
import java.io.File


/**
 * Unit tests for [SettingsFileManager].
 */
object SettingsFileManagerTest : FunSpec({
    lateinit var file: File

    fun saveSettingsToDisk() =
        StoreUtil.saveSettings(ApplicationManager.getApplication(), forceSavingAllSettings = true)


    useBareIdeaFixture()

    beforeNonContainer {
        file = tempfile("xml")
    }


    context("backUpTo") {
        test("backs up the settings to the designated file") {
            Settings.DEFAULT.templates.apply {
                clear()
                add(Template(name = "tongue"))
            }
            val oldSettings = Settings.DEFAULT.deepCopy(retainUuid = true)

            saveSettingsToDisk()
            SettingsFileManager.backUpTo(file)

            Settings.from(file) shouldBe oldSettings
        }
    }

    context("restoreFrom") {
        test("loads the settings from the designated file") {
            val settings = Settings(templateList = TemplateList(mutableListOf(Template("jasper"))))
            file.writeText(settings.serializeToXmlString())
            Settings.DEFAULT shouldNotBe settings

            SettingsFileManager.restoreFrom(file)

            Settings.DEFAULT shouldBe settings
        }
    }

    context("deleteSettings") {
        test("deletes the settings file") {
            Settings.DEFAULT.templates.apply {
                clear()
                add(Template(name = "nun"))
            }

            saveSettingsToDisk()
            SettingsFileManager.deleteSettings()

            SettingsFileManager.SETTINGS_FILE shouldNot exist()
        }

        test("resets all custom changes in the in-memory settings") {
            Settings.DEFAULT.templates.apply {
                clear()
                add(Template(name = "lime"))
            }
            val oldSettings = Settings.DEFAULT.deepCopy(retainUuid = true)

            SettingsFileManager.deleteSettings()

            val newSettings = Settings.DEFAULT
            newSettings shouldNotBe oldSettings
        }
    }
})
