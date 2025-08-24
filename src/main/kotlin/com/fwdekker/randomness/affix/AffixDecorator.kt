package com.fwdekker.randomness.affix

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.DecoratorScheme
import com.fwdekker.randomness.ui.ValidatorDsl.Companion.validators


/**
 * Decorates a string by adding a prefix and suffix.
 *
 * @property enabled Whether to apply this decorator.
 * @property descriptor The description of the affix. Here, `'\'` is the escape character (which also escapes itself),
 * and each unescaped `'@'` is replaced with the original string. If the descriptor does not contain an unescaped `'@'`,
 * then the entire descriptor is placed both in front of and after the original string. For example, affixing `"word"`
 * with descriptor `"(@)"` gives `"(word)"`, and affixing `"word"` with descriptor `"()"` gives `"()word()"`.
 */
data class AffixDecorator(
    var enabled: Boolean = DEFAULT_ENABLED,
    var descriptor: String = DEFAULT_DESCRIPTOR,
) : DecoratorScheme() {
    override val name = Bundle("affix.title")
    override val decorators = emptyList<DecoratorScheme>()
    override val isEnabled get() = enabled
    override val validators = validators {
        case({ enabled }) {
            of(::descriptor).check(
                { !it.fold(false) { escaped, char -> if (char == '\\') !escaped else false } },
                { Bundle("affix.error.trailing_escape") }
            )
        }
    }


    override fun generateUndecoratedStrings(count: Int): List<String> {
        val affixes = descriptor
            .fold(Pair(listOf(""), false)) { (parts, isEscaped), char ->
                when (char) {
                    '\\' -> Pair(parts.dropLast(1) + listOf(parts.last() + if (isEscaped) '\\' else ""), !isEscaped)
                    '@' ->
                        if (isEscaped) Pair(parts.dropLast(1) + listOf("${parts.last()}@"), false)
                        else Pair(parts + listOf(""), false)
                    else -> Pair(parts.dropLast(1) + listOf("${parts.last()}$char"), false)
                }
            }
            .first
            .let {
                if (it.size == 1) listOf(it.single(), it.single())
                else it
            }

        return generator(count).map { affixes.joinToString(it) }
    }


    override fun deepCopy(retainUuid: Boolean) = copy().deepCopyTransient(retainUuid)


    /**
     * Holds constants.
     */
    companion object {
        /**
         * The default value of the [enabled] field.
         */
        const val DEFAULT_ENABLED = false

        /**
         * The default [descriptor] of the affix.
         */
        const val DEFAULT_DESCRIPTOR = ""
    }
}
