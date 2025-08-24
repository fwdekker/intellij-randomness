package com.fwdekker.randomness.ui

import com.fwdekker.randomness.State
import com.fwdekker.randomness.ui.ValidatorDsl.Companion.validators
import kotlin.reflect.KProperty


/**
 * Information that indicates a failed validation.
 *
 * A successful validation is typically represented by a `null` value instead of a [ValidationInfo] instance.
 *
 * @property state The state that is invalid.
 * @property property The property within the [state] that is invalid.
 * @property message A string explaining why the [state] is invalid.
 */
data class ValidationInfo(val state: State, val property: KProperty<*>, val message: String) {
    /**
     * Returns a new [ValidationInfo] with the [prefix] and [separator] prepended to the [message].
     */
    fun prepend(prefix: String, separator: String = " > "): ValidationInfo =
        copy(message = "$prefix$separator$message")
}

/**
 * Associates a [property] with a method that [validate]s that property.
 *
 * @param T the type of [property]
 * @property property The property that is validated.
 * @param validate the method by which the property is validated, which returns `null` if the property is valid, and
 * returns [ValidationInfo] otherwise
 */
class Validator<T : Any?>(val property: KProperty<T>, validate: (T) -> ValidationInfo?) {
    /**
     * Partially applied form of [validate] that automatically reads the value from [property].
     */
    val validate: () -> ValidationInfo? = { validate(property.getter.call()) }
}

/**
 * Runs all given [Validator]s, returning the first non-`null` value, or `null` if all [Validator]s return `null`.
 */
fun List<Validator<*>>.validate(): ValidationInfo? = firstNotNullOfOrNull { it.validate() }


/**
 * A domain-specific language (DSL) for creating [Validator]s on a [State] object.
 *
 * @param state the [State] that this DSL creates [Validator]s for. This [State] is not modified by the DSL itself
 * @param validators the list into which to store [Validator]s that are created in this DSL
 * @param condition the pre-condition to apply to all [Validator]s created by this [DSL]. If [condition] returns `false`
 * during validation, then the output of the [Validator]s is changed to `null`. The [condition] applies only to
 * [Validator]s constructed at this level of nesting of the DSL, and not, for example, to those already in [validators]
 * at construction time
 * @see Companion.validators to construct this DSL
 */
class ValidatorDsl private constructor(
    private val state: State,
    private val validators: MutableList<Validator<*>> = mutableListOf(),
    private val condition: (() -> Boolean) = TRUE,
) {
    /**
     * Wraps around the default [Validator] constructor, but additionally adds the created [Validator] to [validators].
     */
    private fun <T : Any?> validator(property: KProperty<T>, validate: (T) -> ValidationInfo?): Validator<T> =
        Validator(property) { target -> if (condition()) validate(target) else null }
            .also { validators += it }


    /**
     * Enters a DSL for constructing [Validator]s for the given [property].
     */
    fun <T> of(property: KProperty<T>): OfDsl<T> = OfDsl(property)

    /**
     * Includes all [Validator]s of the given [property], but skips them during validation when [condition] is `false`.
     */
    fun <S : State> include(property: KProperty<S>, condition: () -> Boolean = TRUE) {
        validator(property) {
            if (condition()) property.getter.call().doValidate()?.copy(state = state)
            else null
        }
    }

    /**
     * Enters a DSL that is like the current one, but makes every [Validator] constructed inside [body] return `null`
     * when [condition] returns `false` during validation.
     */
    fun case(condition: () -> Boolean, body: ValidatorDsl.() -> Unit) =
        ValidatorDsl(state, validators) { this.condition() && condition() }.body()


    /**
     * A domain-specific language (DSL) for creating [Validator]s for a specific [property].
     *
     * @param T the type of [property]
     * @param property the property for which this DSL should create [Validator]s
     * @see ValidatorDsl.of to construct this DSL
     */
    inner class OfDsl<T : Any?>(private val property: KProperty<T>) {
        /**
         * Constructs [ValidationInfo] from the given [message] using parameters inferred from the DSL context.
         *
         * If [message] is `null`, this function returns `null`.
         *
         * Useful inside the lambdas you must pass to [check] if you want to quickly turn a [String] into
         * [ValidationInfo].
         */
        fun info(message: String?): ValidationInfo? =
            message?.let { ValidationInfo(state, property, message) }


        /**
         * Adds a [Validator] to the outer [ValidatorDsl] that checks [property]'s value with [validate].
         */
        fun check(validate: OfDsl<T>.(T) -> ValidationInfo?): OfDsl<T> {
            validator(property) { validate(it) }
            return this
        }

        /**
         * Adds a [Validator] to the outer [ValidatorDsl] that checks the [property]'s value with [isValid].
         *
         * If [isValid] is `true`, then the created [Validator] returns `null`, and returns [message] applied to
         * [property]'s value otherwise.
         */
        fun check(
            isValid: OfDsl<T>.(T) -> Boolean,
            message: OfDsl<T>.(T) -> String,
        ): OfDsl<T> {
            validator(property) { if (isValid(it)) null else info(message(it)) }
            return this
        }

        /**
         * Adds a [Validator] to the outer [ValidatorDsl] that checks that no [Exception] is thrown inside [validate].
         *
         * The [Exception]'s message is used as the [ValidationInfo] message. The exception is completely swallowed.
         */
        @Suppress("detekt:TooGenericExceptionCaught") // Cannot catch generic exceptions
        fun checkNoException(validate: OfDsl<T>.(T) -> Any?): OfDsl<T> {
            validator(property) {
                try {
                    validate(it)
                    null
                } catch (exception: Exception) {
                    info(exception.message)
                }
            }
            return this
        }
    }


    /**
     * Holds constants.
     */
    companion object {
        /**
         * Always returns `true`.
         */
        private val TRUE: () -> Boolean = { true }


        /**
         * Initiates the [ValidatorDsl] on `this` [State], and returns all [Validator]s that were created inside the DSL
         * using the DSL's own functions.
         *
         * Manually created [Validator]s are not included in the returned [List].
         *
         * This DSL does not modify `this` [State] by itself at any point.
         */
        fun State.validators(body: ValidatorDsl.() -> Unit): List<Validator<*>> =
            ValidatorDsl(this).apply(body).validators
    }
}
