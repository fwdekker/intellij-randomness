package com.fwdekker.randomness

import com.fwdekker.randomness.template.Template
import com.fwdekker.randomness.template.TemplateList
import com.fwdekker.randomness.template.TemplateReference
import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import com.fwdekker.randomness.testhelpers.shouldMatchXml
import com.fwdekker.randomness.testhelpers.shouldValidateAsBundle
import com.intellij.openapi.util.JDOMUtil
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.row
import io.kotest.datatest.withData
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import java.io.FileNotFoundException
import java.lang.module.ModuleDescriptor.Version
import java.net.URL


/**
 * Unit tests for [Settings].
 */
object SettingsTest : FunSpec({
    tags(Tags.PLAIN)


    context("doValidate") {
        withData(
            mapOf(
                "succeeds for default state" to
                    row(Settings(), null),
                "fails if template list is invalid" to
                    row(
                        Settings(
                            templateList = TemplateList(mutableListOf(Template("Duplicate"), Template("Duplicate"))),
                        ),
                        "",
                    ),
            )
        ) { (scheme, validation) -> scheme shouldValidateAsBundle validation }
    }

    context("deepCopy") {
        test("deep-copies the template list") {
            val settings = Settings(templateList = TemplateList(mutableListOf(Template("old"))))

            val copy = settings.deepCopy()
            copy.templates[0].name = "new"

            settings.templates[0].name shouldBe "old"
        }

        test("sets the copy's context to itself") {
            val referenced = Template("old")
            val reference = Template("ref", schemes = mutableListOf(TemplateReference(referenced.uuid)))
            val settings = Settings(templateList = TemplateList(mutableListOf(referenced, reference)))

            val copy = settings.deepCopy(retainUuid = true)
            copy.templates.single { it.name == "old" }.name = "new"

            val referencingCopy = copy.templates.single { it.name == "ref" }
            (referencingCopy.schemes.single() as TemplateReference).template?.name shouldBe "new"
        }
    }
})

/**
 * Unit tests for [PersistentSettings].
 */
object PersistentSettingsTest : FunSpec({
    tags(Tags.PLAIN)


    lateinit var settings: PersistentSettings

    fun getTestConfig(path: String): URL =
        javaClass.getResource(path) ?: throw FileNotFoundException("Could not find resource '$path'.")


    beforeNonContainer {
        settings = PersistentSettings()
    }


    context("loadState") {
        test("does not throw an exception if the stored config's version is newer than is supported") {
            val stored = JDOMUtil.load("""<component><option name="version" value="9.9.9"/></component>""")

            shouldNotThrow<Exception> { settings.loadState(stored) }
        }

        test("throws a ParseSettingsException if the XML cannot be deserialized") {
            val stored = JDOMUtil.load("""<component><option name="version" value="1"/></component>""")

            shouldThrow<ParseSettingsException> { settings.loadState(stored) }
                .message shouldBe "Failed to parse or upgrade settings file."
        }
    }

    context("upgrade") {
        context("input validation") {
            test("fails if the target version is below v3.0.0") {
                val stored = JDOMUtil.load("""<component><option name="version" value="3.0.0"/></component>""")

                shouldThrow<IllegalArgumentException> { settings.upgrade(stored, Version.parse("2.0.0")) }
                    .message shouldStartWith "Unsupported upgrade target version"
            }

            test("fails if the stored version is below v3.0.0") {
                val stored = JDOMUtil.load("""<component><option name="version" value="2.0.0"/></component>""")

                shouldThrow<IllegalArgumentException> { settings.upgrade(stored) }
                    .message shouldStartWith "Unsupported old version"
            }

            test("fails if the stored version is newer than the current version") {
                val stored = JDOMUtil.load("""<component><option name="version" value="9.9.9"/></component>""")

                shouldThrow<FutureSettingsException> { settings.upgrade(stored) }
                    .message shouldStartWith "Unsupported future version"
            }

            test("fails if the stored version is missing") {
                val stored = JDOMUtil.load("""<component></component>""")

                shouldThrow<IllegalArgumentException> { settings.upgrade(stored) }
                    .message shouldStartWith "Missing version number"
            }
        }

        context("generic behaviour") {
            test("bumps the version number if the target version is higher than the stored version") {
                val stored = JDOMUtil.load("""<component><option name="version" value="3.1.0"/></component>""")
                stored.getPropertyValue("version") shouldBe "3.1.0"

                val patched = settings.upgrade(stored, Version.parse("3.2.0"))
                patched.getPropertyValue("version") shouldBe "3.2.0"
            }

            test("does not bump the version number if the target version is lower than the stored version") {
                val stored = JDOMUtil.load("""<component><option name="version" value="3.1.0"/></component>""")
                stored.getPropertyValue("version") shouldBe "3.1.0"

                val patched = settings.upgrade(stored, Version.parse("3.0.0"))
                patched.getPropertyValue("version") shouldBe "3.1.0"
            }

            test("applies multiple upgrades in sequence if needed") {
                val stored = JDOMUtil.load(getTestConfig("/settings-upgrades/v3.1.0-v3.3.5.xml"))
                stored.getSchemes().single().run {
                    getPropertyValue("type") shouldBe "1"
                    getProperty("version") shouldBe null
                }
                stored.getDecorators() shouldNot beEmpty()
                stored.getDecorators().forEach { it.getProperty("generator") shouldNotBe null }

                val patched = settings.upgrade(stored, Version.parse("3.3.5"))
                patched.getSchemes().single().run {
                    getProperty("type") shouldBe null
                    getPropertyValue("version") shouldBe "1"
                }
                patched.getDecorators() shouldNot beEmpty()
                patched.getDecorators().forEach { it.getProperty("generator") shouldBe null }
            }

            test("upgrades only up to the specified version") {
                val stored = JDOMUtil.load(getTestConfig("/settings-upgrades/v3.1.0-v3.3.5.xml"))
                stored.getSchemes().single().run {
                    getPropertyValue("type") shouldBe "1"
                    getProperty("version") shouldBe null
                }
                stored.getDecorators() shouldNot beEmpty()
                stored.getDecorators().forEach { it.getProperty("generator") shouldNotBe null }

                val patched = settings.upgrade(stored, Version.parse("3.2.0"))
                patched.getSchemes().single().run {
                    getProperty("type") shouldBe null
                    getPropertyValue("version") shouldBe "1"
                }
                stored.getDecorators() shouldNot beEmpty()
                stored.getDecorators().forEach { it.getProperty("generator") shouldNotBe null }
            }
        }

        context("specific upgrades") {
            withData(
                nameFn = { "v${it.a} to v${it.b} (${it.c})" },
                row("3.1.0", "3.2.0", "renames `type` to `version` for UUIDs"),
                row("3.3.4", "3.3.5", "removes `generator` fields"),
                row("3.3.6", "3.4.0", "patches epochs to timestamp strings"),
            ) { (from, to, _) ->
                val unpatched = JDOMUtil.load(getTestConfig("/settings-upgrades/v$from-v$to-before.xml"))

                val patched = settings.upgrade(unpatched, Version.parse(to))

                patched shouldMatchXml getTestConfig("/settings-upgrades/v$from-v$to-after.xml").readText()
            }
        }
    }
})
