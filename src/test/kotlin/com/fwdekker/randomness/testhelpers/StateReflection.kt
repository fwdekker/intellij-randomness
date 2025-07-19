package com.fwdekker.randomness.testhelpers

import com.fwdekker.randomness.CapitalizationMode
import com.fwdekker.randomness.Scheme
import com.fwdekker.randomness.State
import com.fwdekker.randomness.Timestamp
import com.fwdekker.randomness.Timestamp.Companion.FORMATTER
import com.fwdekker.randomness.getMod
import com.fwdekker.randomness.integer.IntegerScheme
import com.github.sisyphsu.dateparser.DateParserUtils
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.Transient
import com.intellij.util.xmlb.annotations.XCollection
import kotlin.reflect.KCallable
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure


/**
 * Maps each element to its name, or returns an empty list of `this` is `null`.
 */
fun Collection<KCallable<*>>?.callableNames(): Set<String> = (this ?: emptyList()).map { it.name }.toSet()

/**
 * Maps each element to its name, or returns an empty list of `this` is `null`.
 */
fun Collection<KParameter>?.parameterNames(): Set<String> = (this ?: emptyList()).map { it.name!! }.toSet()


/**
 * Returns all of the [com.fwdekker.randomness.State]'s properties ("fields"), i.e. everything preceded by `val` or
 * `var` in this class or any of its superclasses.
 *
 * @param S the type of state containing the returned properties
 */
fun <S : State> S.properties(): List<KProperty1<out S, *>> = this::class.memberProperties.toList()

/**
 * Returns all of [State]'s [parameters] that are defined in the primary constructor.
 *
 * @param S the type of state containing the returned parameters
 */
fun <S : State> S.parameters(): List<KProperty1<out S, *>> {
    val params = this::class.primaryConstructor?.parameters.parameterNames()
    return properties().filter { it.name in params }
}

/**
 * Returns `true` if and only if this property is annotated with [A], or if any of the superclasses of the class that
 * contains `this` property has a property with the same name as `this` property that is annotated with [A].
 */
private inline fun <reified A : Annotation> KProperty<*>.hasAnnotationInChain(): Boolean {
    val ownerClass = parameters
        .also { require(it.size == 1) { "Type detection requires that property has only one parameter." } }
        .let { it[0].type.jvmErasure }

    return (ownerClass.allSuperclasses + ownerClass)
        .map { clz -> clz.memberProperties.filter { it.name == name } }
        .onEach { require(it.size <= 1) { "(Super)class unexpectedly has multiple properties with same name." } }
        .flatten()
        .any {
            it.hasAnnotation<A>() ||
                it.getter.hasAnnotation<A>() ||
                it is KMutableProperty<*> && it.setter.hasAnnotation<A>() ||
                it.javaField?.getAnnotation(A::class.java) != null
        }
}

/**
 * Returns `true` if and only if this property is annotated as transient in this class itself or in one of its
 * superclasses.
 */
fun KProperty<*>.isTransient(): Boolean = hasAnnotationInChain<Transient>()

/**
 * Returns `true` if this property will (presumably) be serialized by [com.intellij.util.xmlb.XmlSerializer].
 */
fun KProperty<*>.isSerialized(): Boolean =
    !hasAnnotationInChain<Transient>() &&
        (this is KMutableProperty<*> || hasAnnotationInChain<OptionTag>() || hasAnnotationInChain<XCollection>())


/**
 * Recursively mutates this element and the properties contained within.
 *
 * The logic is broadly as follows:
 * - Given a primitive, a primitive with a different value is returned.
 * - Given an enum, the next element is returned.
 * - Given a list, this function is invoked recursively on each element, and a shallow copy of the list with one element
 *   appended is returned.
 * - Given a [State], enumerate over each of its [properties], and invoke this function recursively on each. The
 *   returned value is written into the [State] instance if and only if the property is mutable.
 */
@Suppress("detekt:CyclomaticComplexMethod")
fun Any?.mutated(): Any {
    return when (this) {
        null -> "foo"
        is Boolean -> not()
        is Int -> inc()
        is Long -> inc()
        is Float -> inc()
        is Double -> inc()
        is String -> "foo_$this"
        is CapitalizationMode -> CapitalizationMode.entries.getMod(ordinal + 1)
        is Timestamp ->
            if (epochMilli == null) Timestamp("foo_$value")
            else Timestamp(DateParserUtils.parseDateTime(value).plusSeconds(1).format(FORMATTER))

        is State ->
            properties()
                .filter { it.isSerialized() }
                .forEach {
                    val newValue = it.getter.call(this).mutated()
                    if (it is KMutableProperty<*>)
                        it.setter.call(this, newValue)
                }
                .let { this }

        is List<*> ->
            if (isEmpty()) error("Cannot mutate empty list.")
            else when (first()) {
                is Int -> map { it.mutated() } + 0
                is String -> map { it.mutated() } + "foo"
                is Scheme -> map { it.mutated() } + IntegerScheme()
                else -> error("Cannot mutate lists with elements such as `${first()}`.")
            }

        else -> error("Cannot mutate value `$this`.")
    }
}
