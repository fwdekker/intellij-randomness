package com.fwdekker.randomness

import com.fwdekker.randomness.integer.IntegerScheme
import com.fwdekker.randomness.string.StringScheme
import com.fwdekker.randomness.template.Template
import com.fwdekker.randomness.template.TemplateList
import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import com.fwdekker.randomness.testhelpers.shouldMatchXml
import com.fwdekker.randomness.uuid.UuidScheme
import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.xmlb.XmlSerializer.serialize
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import org.jdom.Element


/**
 * Unit tests for extension functions in `XmlHelpersKt`.
 */
object XmlHelpersTest : FunSpec({
    tags(Tags.PLAIN)


    context("addProperty") {
        test("adds the property with the given name") {
            val element = JDOMUtil.load("<tag></tag>")

            element.addProperty("needle")

            element shouldMatchXml "<tag><option name='needle' /></tag>"
        }

        test("adds the property with the given name even if it already exists") {
            val element = JDOMUtil.load("<tag><option name='needle' /></tag>")

            element.addProperty("needle")

            element shouldMatchXml "<tag><option name='needle' /><option name='needle' /></tag>"
        }

        test("adds the property with the given name and value") {
            val element = JDOMUtil.load("<tag></tag>")

            element.addProperty("needle", "value")

            element shouldMatchXml "<tag><option name='needle' value='value' /></tag>"
        }
    }

    context("getProperty") {
        test("returns the child with the given name") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option1 name="wrong" />
                    <option2 name="needle" />
                    <option3 attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.getProperty("needle")?.name shouldBe "option2"
        }

        test("returns `null` if no children have the given name") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option1 name="wrong" />
                    <option2 name="noodle" />
                    <option3 attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.getProperty("needle")?.name shouldBe null
        }

        test("returns `null` if multiple children have the given name") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option1 name="wrong" />
                    <option2 name="needle" />
                    <option3 name="needle" />
                    <option4 attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.getProperty("needle")?.name shouldBe null
        }
    }

    context("getMultiProperty") {
        test("returns the single child with the given name") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option1 name="wrong" />
                    <option2 name="needle" />
                    <option3 attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.getMultiProperty("needle").map { it.name } shouldContainExactly listOf("option2")
        }

        test("returns en empty list if no children have the given name") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option1 name="wrong" />
                    <option2 name="noodle" />
                    <option3 attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.getMultiProperty("needle") should beEmpty()
        }

        test("returns all children that have the given name") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option1 name="wrong" />
                    <option2 name="needle" />
                    <option3 name="needle" />
                    <option4 attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.getMultiProperty("needle").map { it.name } shouldContainExactly listOf("option2", "option3")
        }
    }

    context("getPropertyByPath") {
        test("returns the child with the given name") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option1 name="wrong" />
                    <option2 name="needle" />
                    <option3 attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.getPropertyByPath("needle")?.name shouldBe "option2"
        }

        test("returns the singular child if `null` is given") {
            val element = JDOMUtil.load("<tag><child /></tag>")

            element.getPropertyByPath(null)?.name shouldBe "child"
        }

        test("returns the element at the given path") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option1 name="wrong" />
                    <option2 name="needle">
                        <wrapper>
                            <optionA name="prong">
                                <target />
                            </optionA>
                            <optionB name="undesired" />
                        </wrapper>
                    </option2>
                    <option3 attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.getPropertyByPath("needle", null, "prong", null)?.name shouldBe "target"
        }

        test("returns `null` if no element matches the path") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option1 name="wrong" />
                    <option2 name="needle">
                        <wrapper>
                            <optionA name="prong">
                                <target />
                            </optionA>
                            <optionB name="undesired" />
                        </wrapper>
                    </option2>
                    <option3 attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.getPropertyByPath("wrong", null, "prong", null)?.name shouldBe null
        }
    }

    context("getPropertyValue") {
        test("returns the value of the property with the given name") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option name="needle" value="desired" />
                    <option attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.getPropertyValue("needle") shouldBe "desired"
        }

        test("returns `null` if the property does not exist") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option1 name="wrong" value="undesired" />
                    <option2 name="noodle" value="valet" />
                    <option3 attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.getPropertyValue("needle") shouldBe null
        }

        test("returns `null` if the property exists multiple times") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option1 name="wrong" value="undesired" />
                    <option2 name="needle" value="valet" />
                    <option3 name="needle" value="voodoo" />
                    <option4 attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.getPropertyValue("needle") shouldBe null
        }

        test("returns `null` if the property has no value") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option name="needle" />
                    <option attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.getPropertyValue("needle") shouldBe null
        }
    }

    context("getMultiPropertyValue") {
        test("returns the value of the property with the given name") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option name="needle" value="desired" />
                    <option attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.getMultiPropertyValue("needle") shouldBe listOf("desired")
        }

        test("returns the values of all properties with the given name") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option name="needle" value="value1" />
                    <option name="needle" value="value2" />
                    <option attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.getMultiPropertyValue("needle") shouldBe listOf("value1", "value2")
        }

        test("returns a list with entries only for properties with a value") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option name="needle" />
                    <option name="needle" value="desired" />
                    <option attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.getMultiPropertyValue("needle") shouldBe listOf("desired")
        }

        test("returns an empty list if the property does not exist") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option1 name="wrong" value="undesired" />
                    <option2 name="noodle" value="valet" />
                    <option3 attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.getMultiPropertyValue("needle") should beEmpty()
        }

        test("returns an empty list if the property has no value") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option name="needle" />
                    <option attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.getMultiPropertyValue("needle") should beEmpty()
        }
    }

    context("setPropertyValue") {
        test("adds the property if it does not exist") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.setPropertyValue("needle", "value")

            element shouldMatchXml
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option attribute="value" />
                    <option name="needle" value="value" />
                </tag>
                """.trimIndent()
        }

        test("adds a value to the property if it does not have one") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option name="needle" />
                    <option attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.setPropertyValue("needle", "value")

            element shouldMatchXml
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option name="needle" value="value" />
                    <option attribute="value" />
                </tag>
                """.trimIndent()
        }

        test("changes the existing property's value") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option name="needle" value="value" />
                    <option attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.setPropertyValue("needle", "new-value")

            element shouldMatchXml
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option name="needle" value="new-value" />
                    <option attribute="value" />
                </tag>
                """.trimIndent()
        }

        test("changes all existing properties' values") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option name="needle" value="valet" />
                    <option name="needle" value="voodoo" />
                    <option attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.setPropertyValue("needle", "new-value")

            element shouldMatchXml
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option name="needle" value="new-value" />
                    <option name="needle" value="new-value" />
                    <option attribute="value" />
                </tag>
                """.trimIndent()
        }
    }

    context("renameProperty") {
        test("renames the property with the given name") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option name="needle" value="desired" />
                    <option attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.renameProperty("needle", "new-needle")

            element shouldMatchXml
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option name="new-needle" value="desired" />
                    <option attribute="value" />
                </tag>
                """.trimIndent()
        }

        test("renames all properties with the given name") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option name="needle" value="desired1" />
                    <option attribute="value" />
                    <option name="needle" value="desired2" />
                </tag>
                """.trimIndent()
            )

            element.renameProperty("needle", "new-needle")

            element shouldMatchXml
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option name="new-needle" value="desired1" />
                    <option attribute="value" />
                    <option name="new-needle" value="desired2" />
                </tag>
                """.trimIndent()
        }

        test("does nothing if no property has the given name") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.renameProperty("needle", "new-needle")

            element shouldMatchXml
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option attribute="value" />
                </tag>
                """.trimIndent()
        }

        test("throws an error if the new name is already in use") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option name="needle" value="desired1" />
                    <option attribute="value" />
                    <option name="new-needle" value="desired2" />
                </tag>
                """.trimIndent()
            )

            shouldThrow<IllegalArgumentException> { element.renameProperty("needle", "new-needle") }
        }
    }

    context("removeProperty") {
        test("removes the property with the given name") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option name="needle" value="desired" />
                    <option attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.removeProperty("needle")

            element shouldMatchXml
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option attribute="value" />
                </tag>
                """.trimIndent()
        }

        test("removes all properties with the given name") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option name="needle" value="foo" />
                    <option name="wrong" value="undesired" />
                    <option name="needle" value="bar" />
                    <option attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.removeProperty("needle")

            element shouldMatchXml
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option attribute="value" />
                </tag>
                """.trimIndent()
        }

        test("does nothing if no property with the given name exists") {
            val element = JDOMUtil.load(
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option name="also_wrong" value="also_undesired" />
                    <option attribute="value" />
                </tag>
                """.trimIndent()
            )

            element.removeProperty("needle")

            element shouldMatchXml
                """
                <tag>
                    <option name="wrong" value="undesired" />
                    <option name="also_wrong" value="also_undesired" />
                    <option attribute="value" />
                </tag>
                """.trimIndent()
        }
    }


    context("accessors") {
        lateinit var settings: Settings
        lateinit var xml: Element


        beforeNonContainer {
            settings = Settings(
                templateList = TemplateList(
                    mutableListOf(
                        Template(name = "Foo", schemes = mutableListOf(IntegerScheme(), StringScheme())),
                        Template(name = "Bar", schemes = mutableListOf(UuidScheme())),
                    )
                )
            )
            xml = serialize(settings)
        }


        test("getTemplateList") {
            val xmlUuid = xml.getTemplateList()?.getPropertyValue("uuid")
            val realUuid = settings.templateList.uuid

            xmlUuid shouldBe realUuid
        }

        test("getTemplates") {
            val xmlUuids = xml.getTemplates().map { it.getPropertyValue("uuid") }
            val realUuids = settings.templates.map { it.uuid }
            realUuids shouldNot beEmpty()

            xmlUuids shouldContainExactlyInAnyOrder realUuids
        }

        test("getSchemes") {
            val xmlUuids = xml.getSchemes().map { it.getPropertyValue("uuid") }
            val realUuids = settings.templates.flatMap { it.schemes }.map { it.uuid }
            realUuids shouldNot beEmpty()

            xmlUuids shouldContainExactlyInAnyOrder realUuids
        }

        test("getDecorators") {
            val xmlUuids = xml.getDecorators().map { it.getPropertyValue("uuid") }
            val realUuids = settings.templates
                .flatMap { it.schemes }
                .flatMap { it.decorators }
                .flatMap { listOf(it) + it.decorators }
                .map { it.uuid }
            realUuids shouldNot beEmpty()

            xmlUuids shouldContainExactlyInAnyOrder realUuids
        }
    }
})
