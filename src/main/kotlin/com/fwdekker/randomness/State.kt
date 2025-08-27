package com.fwdekker.randomness

import com.fasterxml.uuid.Generators
import com.fwdekker.randomness.ui.ValidationInfo
import com.fwdekker.randomness.ui.Validator
import com.fwdekker.randomness.ui.validate
import com.intellij.util.xmlb.annotations.Transient
import kotlin.random.Random
import kotlin.random.asJavaRandom


/**
 * A state holds variables that can be configured, validated, copied, and loaded.
 *
 * In a [State], fields are typically mutable, because the user can change the field. However, fields that themselves
 * also contain data (e.g. a [Collection] or a [DecoratorScheme]) should be stored as immutable references (but may
 * themselves be mutable). For example, instead of having the field `var list: List<String>`, a [State] should have the
 * field `val list: MutableList<String>`. This reflects the typical case in which the user does not change the reference
 * to the object, but changes the properties inside the object. And, more importantly, means that nested a
 * [SchemeEditor] can share a single unchanging reference to a [State] with its nested [SchemeEditor]s.
 *
 * Properties should be annotated with [Transient] (not [kotlin.jvm.Transient]) to ensure they are not stored. The
 * specifics of how the annotation is applied and inherited are complicated. The following rules apply:
 * - Immutable non-[Collection]s (e.g. `val foo: Int` or `val bar: State`) are not serialized. These properties should
 *   not be annotated, not even "just in case" nor "for clarity".
 * - Immutable [Collection]s (e.g. `val foo: List<Int>` or `val bar: List<State>`) are not serialized, unless annotated
 *   with a serialization annotation such as [com.intellij.util.xmlb.annotations.XCollection] or
 *   [com.intellij.util.xmlb.annotations.OptionTag].
 * - Mutable properties (i.e. `var`) are serialized, unless annotated with [Transient].
 * - Do not combine [Transient] with any other serialization annotations.
 * - To annotate a mutable property (i.e. `var`), use `@get:Transient`.
 * - To annotate a lateinit property (i.e. `lateinit var`), use both `@field:Transient` and `@get:Transient`.
 * - When a property overrides a property from a superclass, only the annotations in the subclass are relevant.
 * - Since `abstract` properties must always be overridden, they should be annotated only in the subclass. Adding
 *   additional annotations "for clarity" in a superclass should be avoided, since it is completely useless and
 *   therefore misleading.
 * - Since `open` properties are not always overridden, if they are annotated, they should be annotated in both the
 *   superclass and the subclass.
 * - See also `isSerialized` in `StateReflection` in Randomness' test module.
 *
 * These observations are based on IntelliJ Community 2024.1. See also the bug report at
 * [IJPL-175868](https://youtrack.jetbrains.com/issue/IJPL-175868/).
 */
abstract class State {
    /**
     * A UUID to uniquely track this scheme even when it is copied.
     */
    var uuid: String = Generators.randomBasedGenerator(Random.asJavaRandom()).generate().toString()

    /**
     * The context of this state in the form of a reference to the [Settings].
     *
     * Useful in case the scheme's behavior depends not only on its own internal state, but also on that of other
     * schemes.
     *
     * @see applyContext
     */
    @field:Transient
    @get:Transient
    var context: Box<Settings> = Box({ Settings.DEFAULT })
        protected set

    /**
     * Lists the [Validator]s relevant to this [State].
     *
     * @see doValidate
     */
    open val validators: List<Validator<*>> = emptyList()


    /**
     * Sets the [State.context] of this [State] to be a reference to [context].
     */
    fun applyContext(context: Settings) = applyContext(Box({ context }))

    /**
     * Sets the [State.context] of this [State] to [context].
     */
    open fun applyContext(context: Box<Settings>) {
        this.context = context
    }


    /**
     * Validates the state, and indicates whether and why it is invalid.
     *
     * @return `null` if the state is valid, or a string explaining why the state is invalid
     * @see validators
     */
    fun doValidate(): ValidationInfo? = validators.validate()

    /**
     * Returns a deep copy, retaining the [uuid] if and only if [retainUuid] is `true`.
     *
     * Fields annotated with [Transient] are shallow-copied.
     *
     * @see deepCopyTransient utility function for subclasses that want to implement [deepCopy]
     */
    abstract fun deepCopy(retainUuid: Boolean = false): State

    /**
     * When invoked by the instance `this` on (another) instance `self` as `self.deepCopyTransient()`, this method
     * copies [Transient] fields from `this` to `self`, and returns `self`.
     *
     * @see deepCopy
     */
    protected fun <SELF : State> SELF.deepCopyTransient(retainUuid: Boolean): SELF {
        val self: SELF = this
        val thiz: State = this@State

        if (retainUuid) self.uuid = thiz.uuid
        self.applyContext(thiz.context.copy()) // Copies the [Box], not the context itself

        return self
    }
}
