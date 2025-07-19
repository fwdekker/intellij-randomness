package com.fwdekker.randomness

import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import com.fwdekker.randomness.testhelpers.shouldMatchXml
import com.intellij.openapi.util.JDOMUtil
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


    context("version upgrades") {
        context("input validation") {
            test("fails if the target version is below v3.0.0") {
                val stored = JDOMUtil.load("""<component><option name="version" value="3.0.0"/></component>""")

                shouldThrow<IllegalArgumentException> { settings.upgrade(stored, Version.parse("2.0.0")) }
                    .message shouldStartWith "Unsupported upgrade target version"
            }

            test("fails if the stored version below v3.0.0") {
                val stored = JDOMUtil.load("""<component><option name="version" value="2.0.0"/></component>""")

                shouldThrow<IllegalArgumentException> { settings.upgrade(stored) }
                    .message shouldStartWith "Unsupported Randomness config version"
            }

            test("fails if the stored version is missing") {
                val stored = JDOMUtil.load("""<component></component>""")

                shouldThrow<IllegalArgumentException> { settings.upgrade(stored) }
                    .message shouldStartWith "Missing version number"
            }
        }

        context("generic behaviour") {
            test("bumps the version number if the target version is higher than the stored version") {
                val stored = JDOMUtil.load("""<component><option name="version" value="9.9.8"/></component>""")
                stored.getPropertyValue("version") shouldBe "9.9.8"

                val patched = settings.upgrade(stored, Version.parse("9.9.9"))
                patched.getPropertyValue("version") shouldBe "9.9.9"
            }

            test("does not bump the version number if the target version is lower than the stored version") {
                val stored = JDOMUtil.load("""<component><option name="version" value="9.9.8"/></component>""")
                stored.getPropertyValue("version") shouldBe "9.9.8"

                val patched = settings.upgrade(stored, Version.parse("9.9.7"))
                patched.getPropertyValue("version") shouldBe "9.9.8"
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
                row("3.3.6", "3.4.0", "patches epochs to `Timestamp` objects"),
            ) { (from, to, _) ->
                val unpatched = JDOMUtil.load(getTestConfig("/settings-upgrades/v$from-v$to-before.xml"))

                val patched = settings.upgrade(unpatched, Version.parse(to))

                patched shouldMatchXml getTestConfig("/settings-upgrades/v$from-v$to-after.xml").readText()
            }
        }
    }
})
