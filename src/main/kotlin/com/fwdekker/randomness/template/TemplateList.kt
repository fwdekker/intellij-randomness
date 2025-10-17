package com.fwdekker.randomness.template

import com.fwdekker.randomness.Box
import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.CapitalizationMode
import com.fwdekker.randomness.Settings
import com.fwdekker.randomness.State
import com.fwdekker.randomness.affix.AffixDecorator
import com.fwdekker.randomness.array.ArrayDecorator
import com.fwdekker.randomness.datetime.DateTimeScheme
import com.fwdekker.randomness.decimal.DecimalScheme
import com.fwdekker.randomness.integer.IntegerScheme
import com.fwdekker.randomness.nanoid.NanoIdScheme
import com.fwdekker.randomness.string.StringScheme
import com.fwdekker.randomness.ui.ValidatorDsl.Companion.validators
import com.fwdekker.randomness.uuid.UuidScheme
import com.fwdekker.randomness.word.DefaultWordList
import com.fwdekker.randomness.word.WordScheme
import com.intellij.util.xmlb.annotations.OptionTag


/**
 * A collection of different templates.
 *
 * @property templates The collection of templates, each with a unique name.
 */
data class TemplateList(
    @OptionTag
    val templates: MutableList<Template> = DEFAULT_TEMPLATES,
) : State() {
    override val validators = validators {
        of(::templates).check { templates ->
            val templateNames = templates.map { it.name }
            val duplicate = templateNames.firstOrNull { templateNames.indexOf(it) != templateNames.lastIndexOf(it) }
            val invalid = templates.firstNotNullOfOrNull { it.doValidate()?.prepend(it.name) }

            when {
                duplicate != null -> info(Bundle("template_list.error.duplicate_name", duplicate))
                invalid != null -> invalid
                else -> null
            }
        }
    }


    override fun applyContext(context: Box<Settings>) {
        super.applyContext(context)
        templates.forEach { it.applyContext(context) }
    }


    /**
     * Returns the template in this list that has [uuid] as its UUID, or `null` if there is no such template.
     */
    fun getTemplateByUuid(uuid: String) = templates.singleOrNull { it.uuid == uuid }

    /**
     * Returns the template or scheme in this list that has [uuid] as its UUID, or `null` if there is no such template
     * or scheme.
     */
    fun getSchemeByUuid(uuid: String) = templates.flatMap { listOf(it) + it.schemes }.singleOrNull { it.uuid == uuid }


    /**
     * Note that the [context] must be updated manually.
     *
     * @see State.deepCopy
     */
    override fun deepCopy(retainUuid: Boolean) =
        copy(templates = templates.map { it.deepCopy(retainUuid) }.toMutableList()).deepCopyTransient(retainUuid)


    /**
     * Holds constants.
     */
    companion object {
        /**
         * The default value of the [templates] field.
         */
        val DEFAULT_TEMPLATES
            get() = mutableListOf(
                Template("Integer", mutableListOf(IntegerScheme())),
                Template("Decimal", mutableListOf(DecimalScheme())),
                Template("Alphanumerical String", mutableListOf(StringScheme())),
                Template(
                    "Personal Name",
                    mutableListOf(
                        WordScheme(
                            words = DefaultWordList.WORD_LIST_MAP["Forenames"]!!.words,
                            affixDecorator = AffixDecorator(enabled = true, descriptor = "@ "),
                        ),
                        WordScheme(words = DefaultWordList.WORD_LIST_MAP["Surnames"]!!.words),
                    )
                ),
                Template(
                    "Lorem Ipsum",
                    mutableListOf(
                        WordScheme(
                            words = DefaultWordList.WORD_LIST_MAP["Lorem"]!!.words,
                            capitalization = CapitalizationMode.FIRST_LETTER,
                            affixDecorator = AffixDecorator(enabled = true, descriptor = "@ "),
                        ),
                        WordScheme(
                            words = DefaultWordList.WORD_LIST_MAP["Lorem"]!!.words,
                            capitalization = CapitalizationMode.LOWER,
                            arrayDecorator = ArrayDecorator(
                                enabled = true,
                                minCount = 3,
                                maxCount = 7,
                                separator = " ",
                                affixDecorator = AffixDecorator(enabled = true, descriptor = "@."),
                            )
                        ),
                    )
                ),
                Template(
                    "Hex Color",
                    mutableListOf(
                        IntegerScheme(
                            minValue = 0L,
                            maxValue = 256L,
                            base = 16,
                            affixDecorator = AffixDecorator(enabled = false),
                            arrayDecorator = ArrayDecorator(
                                enabled = true,
                                minCount = 3,
                                maxCount = 3,
                                separatorEnabled = false,
                                affixDecorator = AffixDecorator(
                                    enabled = true,
                                    descriptor = "#@",
                                ),
                            ),
                        )
                    )
                ),
                Template("UUID", mutableListOf(UuidScheme())),
                Template("Nano ID", mutableListOf(NanoIdScheme())),
                Template("Date-Time", mutableListOf(DateTimeScheme())),
                Template(
                    "IP address",
                    mutableListOf(
                        IntegerScheme(
                            minValue = 0L,
                            maxValue = 255L,
                            arrayDecorator = ArrayDecorator(
                                enabled = true,
                                minCount = 4,
                                maxCount = 4,
                                separator = ".",
                                affixDecorator = AffixDecorator(enabled = false),
                            ),
                        ),
                    )
                ),
                Template(
                    "Constructor",
                    mutableListOf(
                        StringScheme(pattern = "MyClass(name = ", isRegex = false),
                        StringScheme(pattern = "\"[a-zA-Z0-9]{5,8}\"", isRegex = true),
                        StringScheme(pattern = ", value = ", isRegex = false),
                        IntegerScheme(),
                        StringScheme(pattern = ")", isRegex = false),
                    )
                ),
            )
    }
}
