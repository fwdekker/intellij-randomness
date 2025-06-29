package com.fwdekker.randomness

import com.fwdekker.randomness.template.Template
import com.fwdekker.randomness.template.TemplateList
import com.fwdekker.randomness.template.TemplateReference
import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.shouldValidateAsBundle
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.row
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe


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
