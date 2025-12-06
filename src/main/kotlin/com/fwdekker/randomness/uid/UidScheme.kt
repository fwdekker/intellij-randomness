package com.fwdekker.randomness.uid

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.Icons
import com.fwdekker.randomness.Scheme
import com.fwdekker.randomness.TypeIcon
import com.fwdekker.randomness.affix.AffixDecorator
import com.fwdekker.randomness.array.ArrayDecorator
import com.fwdekker.randomness.uid.NanoIdConfig.Companion.MIN_SIZE
import com.fwdekker.randomness.uid.UuidConfig.Companion.MAX_MAX_DATE_TIME
import com.fwdekker.randomness.uid.UuidConfig.Companion.MIN_MIN_DATE_TIME
import com.fwdekker.randomness.uid.UuidConfig.Companion.SUPPORTED_VERSIONS
import com.fwdekker.randomness.uid.UuidConfig.Companion.TIME_BASED_VERSIONS
import com.fwdekker.randomness.ui.ValidatorDsl.Companion.validators
import com.intellij.ui.JBColor
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.Transient
import java.awt.Color


/**
 * Contains settings for generating unique identifiers (UIDs).
 *
 * This scheme supports multiple ID types (UUID, NanoID) and delegates generation
 * to the appropriate configuration based on the selected [idType].
 *
 * @property idTypeKey The key of the selected ID type, used for serialization.
 * @property uuidConfig Configuration for UUID generation.
 * @property nanoIdConfig Configuration for NanoID generation.
 * @property affixDecorator The affixation to apply to the generated values.
 * @property arrayDecorator Settings that determine whether the output should be an array of values.
 */
data class UidScheme(
    var idTypeKey: String = IdType.DEFAULT.key,
    @OptionTag val uuidConfig: UuidConfig = UuidConfig(),
    @OptionTag val nanoIdConfig: NanoIdConfig = NanoIdConfig(),
    @OptionTag val affixDecorator: AffixDecorator = DEFAULT_AFFIX_DECORATOR,
    @OptionTag val arrayDecorator: ArrayDecorator = DEFAULT_ARRAY_DECORATOR,
) : Scheme() {
    /**
     * The selected ID type.
     */
    @get:Transient
    var idType: IdType
        get() = IdType.fromKey(idTypeKey)
        set(value) {
            idTypeKey = value.key
        }

    @get:Transient
    override val name = Bundle("uid.title")

    override val typeIcon get() = BASE_ICON

    override val decorators get() = listOf(affixDecorator, arrayDecorator)

    override val validators = validators {
        case({ idType == IdType.Uuid }) {
            of(uuidConfig::version)
                .check({ it in SUPPORTED_VERSIONS }, { Bundle("uuid.error.unknown_version", it) })
            case({ uuidConfig.version in TIME_BASED_VERSIONS }) {
                of(uuidConfig::minDateTime)
                    .checkNoException { it.epochMilli }
                    .check(
                        { !it.isBefore(MIN_MIN_DATE_TIME) },
                        { Bundle("timestamp.error.too_old", MIN_MIN_DATE_TIME.value) }
                    )
                of(uuidConfig::maxDateTime)
                    .checkNoException { it.epochMilli }
                    .check(
                        { !it.isAfter(MAX_MAX_DATE_TIME) },
                        { Bundle("timestamp.error.too_new", MAX_MAX_DATE_TIME.value) }
                    )
                    .check(
                        { !it.isBefore(uuidConfig.minDateTime) },
                        { Bundle("datetime.error.min_datetime_above_max") }
                    )
            }
        }
        case({ idType == IdType.NanoId }) {
            of(nanoIdConfig::size)
                .check({ it >= MIN_SIZE }, { Bundle("nanoid.error.size_too_low", MIN_SIZE) })
            of(nanoIdConfig::alphabet)
                .check({ it.isNotEmpty() }, { Bundle("nanoid.error.alphabet_empty") })
        }
        include(::affixDecorator)
        include(::arrayDecorator)
    }


    /**
     * Generates [count] random UIDs based on the selected [idType].
     */
    override fun generateUndecoratedStrings(count: Int): List<String> =
        when (idType) {
            IdType.Uuid -> uuidConfig.generate(count, random)
            IdType.NanoId -> nanoIdConfig.generate(count, random)
        }


    override fun deepCopy(retainUuid: Boolean) =
        copy(
            uuidConfig = uuidConfig.deepCopy(),
            nanoIdConfig = nanoIdConfig.deepCopy(),
            affixDecorator = affixDecorator.deepCopy(retainUuid),
            arrayDecorator = arrayDecorator.deepCopy(retainUuid),
        ).deepCopyTransient(retainUuid)


    companion object {
        /**
         * The base icon for UIDs.
         */
        val BASE_ICON
            get() = TypeIcon(Icons.SCHEME, "id", listOf(JBColor(Color(185, 155, 248, 154), Color(185, 155, 248, 154))))

        /**
         * The preset values for the [affixDecorator] field.
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
