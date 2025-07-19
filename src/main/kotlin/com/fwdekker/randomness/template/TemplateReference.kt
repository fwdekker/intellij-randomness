package com.fwdekker.randomness.template

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.CapitalizationMode
import com.fwdekker.randomness.DataGenerationException
import com.fwdekker.randomness.Icons
import com.fwdekker.randomness.OverlayIcon
import com.fwdekker.randomness.OverlayedIcon
import com.fwdekker.randomness.Scheme
import com.fwdekker.randomness.TypeIcon
import com.fwdekker.randomness.affix.AffixDecorator
import com.fwdekker.randomness.array.ArrayDecorator
import com.fwdekker.randomness.ui.ValidatorDsl.Companion.validators
import com.intellij.ui.Gray
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.Transient


/**
 * A reference to an existing template, to allow using a template as if it were a scheme.
 *
 * @property templateUuid The UUID of the referenced template; defaults to the first template in [TemplateList].
 * @property capitalization The way in which the generated word should be capitalized.
 * @property affixDecorator The affixation to apply to the generated values.
 * @property arrayDecorator Settings that determine whether the output should be an array of values.
 */
data class TemplateReference(
    var templateUuid: String? = null,
    var capitalization: CapitalizationMode = DEFAULT_CAPITALIZATION,
    @OptionTag val affixDecorator: AffixDecorator = DEFAULT_AFFIX_DECORATOR,
    @OptionTag val arrayDecorator: ArrayDecorator = DEFAULT_ARRAY_DECORATOR,
) : Scheme() {
    @get:Transient
    override val name get() = template?.name?.let { "[$it]" } ?: Bundle("reference.title")
    override val typeIcon get() = template?.typeIcon ?: DEFAULT_ICON
    override val icon get() = OverlayedIcon(typeIcon, decorators.mapNotNull { it.overlayIcon } + OverlayIcon.REFERENCE)
    override val decorators get() = listOf(arrayDecorator)
    override val validators = validators {
        of(::templateUuid)
            .check({ it != null }, { Bundle("reference.error.no_selection") })
            .check({ template != null }, { Bundle("reference.error.not_found") })
            .check {
                getReferenceCycleOrNull()
                    ?.let { cycle -> Bundle("reference.error.recursion", "(${cycle.joinToString(" → ") { it.name }})") }
                    ?.let { info(it) }
            }
        include(::affixDecorator)
        include(::arrayDecorator)
    }

    /**
     * The [Template] in the [context]'s [TemplateList] that contains this [TemplateReference].
     */
    @get:Transient
    val parent: Template
        get() = (+context).templates.single { template -> template.schemes.any { it.uuid == uuid } }

    /**
     * The [Template] that is being referenced, or `null` if it could not be found in the [context]'s [TemplateList].
     */
    @get:Transient
    var template: Template?
        get() = (+context).templates.singleOrNull { it.uuid == templateUuid }
        set(value) {
            templateUuid = value?.uuid
        }


    /**
     * Tries to find a recursive cycle of references within the current [context] starting at [parent], returning `null`
     * if there is no such cycle.
     */
    private fun getReferenceCycleOrNull(): List<Template>? {
        fun helper(source: TemplateReference, history: List<TemplateReference>): List<Template>? =
            if (source in history) listOf(source.parent)
            else source.template?.schemes
                ?.filterIsInstance<TemplateReference>()
                ?.firstNotNullOfOrNull { helper(it, history + source) }
                ?.let { listOf(source.parent) + it }

        return helper(this, emptyList())
    }

    /**
     * Returns `true` if and only if `this` would not be part of a cycle of recursive references in [context] after
     * setting [template] to [target].
     */
    fun canReference(target: Template): Boolean {
        val originalUuid = templateUuid

        templateUuid = target.uuid
        val createsCycle = getReferenceCycleOrNull() == null
        templateUuid = originalUuid

        return createsCycle
    }


    override fun generateUndecoratedStrings(count: Int) =
        (this.template ?: throw DataGenerationException(Bundle("reference.error.generate_null")))
            .also { it.random = random }
            .generateStrings(count)
            .map { capitalization.transform(it, random) }


    /**
     * Note that the [context] must be updated manually.
     *
     * @see Scheme.deepCopy
     */
    override fun deepCopy(retainUuid: Boolean) =
        copy(arrayDecorator = arrayDecorator.deepCopy(retainUuid)).deepCopyTransient(retainUuid)


    /**
     * Holds constants.
     */
    companion object {
        /**
         * The base icon for references when the reference is invalid.
         */
        val DEFAULT_ICON get() = TypeIcon(Icons.TEMPLATE, "", listOf(Gray._110))

        /**
         * The preset values for the [capitalization] field.
         */
        val PRESET_CAPITALIZATION = listOf(
            CapitalizationMode.RETAIN,
            CapitalizationMode.LOWER,
            CapitalizationMode.UPPER,
            CapitalizationMode.RANDOM,
            CapitalizationMode.SENTENCE,
            CapitalizationMode.FIRST_LETTER,
        )

        /**
         * The default value of the [capitalization] field.
         */
        val DEFAULT_CAPITALIZATION = CapitalizationMode.RETAIN

        /**
         * The preset values for the [affixDecorator] descriptor.
         */
        val PRESET_AFFIX_DECORATOR_DESCRIPTORS = listOf("'", "\"", "`")

        /**
         * The default value of the [affixDecorator] field.
         */
        val DEFAULT_AFFIX_DECORATOR get() = AffixDecorator(enabled = false, descriptor = "\"")

        /**
         * The default value of the [arrayDecorator] field.
         */
        val DEFAULT_ARRAY_DECORATOR get() = ArrayDecorator()
    }
}
