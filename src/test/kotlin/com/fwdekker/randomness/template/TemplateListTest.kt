package com.fwdekker.randomness.template

import com.fwdekker.randomness.Settings
import com.fwdekker.randomness.testhelpers.DummyScheme
import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.shouldValidateAsBundle
import com.fwdekker.randomness.testhelpers.stateDeepCopyTestFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.row
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs


/**
 * Unit tests for [TemplateList].
 */
object TemplateListTest : FunSpec({
    tags(Tags.PLAIN)


    context("applyContext") {
        test("applies the context to itself") {
            val newContext = Settings()
            val list = TemplateList()

            list.applyContext(newContext)

            +list.context shouldBeSameInstanceAs newContext
        }

        test("applies the context to each template") {
            val newContext = Settings()
            val template = Template()
            val list = TemplateList(mutableListOf(template))

            list.applyContext(newContext)

            +template.context shouldBeSameInstanceAs newContext
        }
    }


    context("getTemplateByUuid") {
        test("returns null if no such template can be found") {
            val list = TemplateList()

            list.getTemplateByUuid("46840969-0b31-4fef-92b0-42f34dbc9f8b") shouldBe null
        }

        test("returns the template with the given UUID") {
            val list = TemplateList()

            val template = list.templates[0]

            list.getTemplateByUuid(template.uuid) shouldBeSameInstanceAs template
        }

        test("returns null even if the UUID corresponds to a scheme") {
            val list = TemplateList()

            val scheme = list.templates[0].schemes[0]

            list.getTemplateByUuid(scheme.uuid) shouldBe null
        }
    }

    context("getSchemeByUuid") {
        test("returns null if no such scheme can be found") {
            val list = TemplateList()

            list.getSchemeByUuid("46840969-0b31-4fef-92b0-42f34dbc9f8b") shouldBe null
        }

        test("returns the scheme with the given UUID") {
            val list = TemplateList()

            val scheme = list.templates[0].schemes[0]

            list.getSchemeByUuid(scheme.uuid) shouldBeSameInstanceAs scheme
        }

        test("returns the template with the given UUID") {
            val list = TemplateList()

            val template = list.templates[0]

            list.getSchemeByUuid(template.uuid) shouldBeSameInstanceAs template
        }
    }


    context("doValidate") {
        withData(
            mapOf(
                "succeeds for default state" to
                    row(TemplateList(), null),
                "succeeds with no templates" to
                    row(TemplateList(mutableListOf()), null),
                "fails if multiple templates have the same name" to
                    row(
                        TemplateList(mutableListOf(Template("same"), Template("same"))),
                        "template_list.error.duplicate_name",
                    ),
                "fails if the single template is invalid" to
                    row(TemplateList(mutableListOf(Template(schemes = mutableListOf(DummyScheme(valid = false))))), ""),
                "fails if one of multiple templates is invalid" to
                    row(
                        TemplateList(
                            mutableListOf(
                                Template("Valid"),
                                Template("Invalid", mutableListOf(DummyScheme(valid = false))),
                            ),
                        ),
                        "",
                    ),
            )
        ) { (scheme, validation) -> scheme shouldValidateAsBundle validation }
    }

    include(stateDeepCopyTestFactory { TemplateList() })
})
