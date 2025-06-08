package com.fwdekker.randomness.testhelpers

import com.fwdekker.randomness.CapitalizationMode
import com.fwdekker.randomness.Scheme
import com.fwdekker.randomness.State
import com.fwdekker.randomness.getProperty
import com.fwdekker.randomness.integer.IntegerScheme
import com.fwdekker.randomness.string.StringScheme
import com.intellij.util.xmlb.XmlSerializer.serialize
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.Transient
import com.intellij.util.xmlb.annotations.XCollection
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.row
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe


/**
 * Unit tests for extension functions in `StateReflection`.
 */
object StateReflectionTest : FunSpec({
    context("parameters") {
        withData(
            mapOf(
                "returns an empty list if the constructor is empty and there are no fields" to
                    row(SimpleState(), emptyList()),
                "returns all constructor parameters" to
                    row(ParametersOnly(), listOf("foo")),
                "does not return the fields" to
                    row(FieldsOnly(), listOf()),
                "returns all constructor parameters, but not the fields" to
                    row(ParametersAndFields(), listOf("foo")),
                "returns the subclass' constructor parameters only" to
                    row(ParametersAndFieldsSub(), listOf("baz")),
            )
        ) { (state, parameters) ->
            state.parameters().callableNames() shouldContainExactlyInAnyOrder parameters
        }
    }

    context("properties") {
        withData(
            mapOf(
                "returns an empty list if the constructor is empty and there are no fields" to
                    row(SimpleState(), emptyList()),
                "returns all constructor parameters" to
                    row(ParametersOnly(), listOf("foo")),
                "returns all fields" to
                    row(FieldsOnly(), listOf("foo", "bar")),
                "returns all constructor parameters and all fields" to
                    row(ParametersAndFields(), listOf("foo", "bar")),
                "returns all constructor parameters and all fields from both subclass and superclass" to
                    row(ParametersAndFieldsSub(), listOf("foo", "bar", "baz", "qux")),
            )
        ) { (state, parameters) ->
            val defaults = listOf("context", "uuid", "validators")
            state.properties().callableNames() shouldContainExactlyInAnyOrder defaults + parameters
        }
    }

    context("isTransient") {
        withData(
            mapOf(
                // Constructor parameter
                "constructor parameter without annotation is not transient" to
                    row(TransientAnnotations::constructorNotTransient, false),
                "constructor parameter with `@Transient` is transient" to
                    row(TransientAnnotations::constructorTransient, true),
                "constructor parameter with `@field:Transient` is transient" to
                    row(TransientAnnotations::constructorFieldTransient, true),
                "constructor parameter with `@get:Transient` is transient" to
                    row(TransientAnnotations::constructorGetTransient, true),
                "constructor parameter with `@set:Transient` is transient" to
                    row(TransientAnnotations::constructorSetTransient, true),
                // Declared property
                "declared property without annotation is not transient" to
                    row(TransientAnnotations::declaredNotTransient, false),
                "declared property with `@Transient` is transient" to
                    row(TransientAnnotations::declaredTransient, true),
                "declared property with `@field:Transient` is transient" to
                    row(TransientAnnotations::declaredFieldTransient, true),
                "declared property with `@get:Transient` is transient" to
                    row(TransientAnnotations::declaredGetTransient, true),
                "declared property with `@set:Transient` is transient" to
                    row(TransientAnnotations::declaredSetTransient, true),
                // Inherited property with annotation
                "inherited property without annotation is not transient" to
                    row(TransientAnnotations::superNotTransient, false),
                "inherited property with `@Transient` is transient" to
                    row(TransientAnnotations::superTransient, true),
                "inherited property with `@field:Transient` is transient" to
                    row(TransientAnnotations::superFieldTransient, true),
                "inherited property with `@get:Transient` is transient" to
                    row(TransientAnnotations::superGetTransient, true),
                "inherited property with `@set:Transient` is transient" to
                    row(TransientAnnotations::superSetTransient, true),
                // Inherited property with annotation, override without annotation
                "inherited property with `@Transient` overridden without annotation is transient" to
                    row(TransientAnnotations::onlySuperTransient, true),
                "inherited property with `@field:Transient` overridden without annotation is transient" to
                    row(TransientAnnotations::onlySuperFieldTransient, true),
                "inherited property with `@get:Transient` overridden without annotation is transient" to
                    row(TransientAnnotations::onlySuperGetTransient, true),
                "inherited property with `@set:Transient` overridden without annotation is transient" to
                    row(TransientAnnotations::onlySuperSetTransient, true),
                // Inherited property without annotation, override with annotation
                "inherited property without annotation overridden with `@Transient` is transient" to
                    row(TransientAnnotations::onlySubTransient, true),
                "inherited property without annotation overridden with `@field:Transient` is transient" to
                    row(TransientAnnotations::onlySubFieldTransient, true),
                "inherited property without annotation overridden with `@get:Transient` is transient" to
                    row(TransientAnnotations::onlySubGetTransient, true),
                "inherited property without annotation overridden with `@set:Transient` is transient" to
                    row(TransientAnnotations::onlySubSetTransient, true),
                // Known cases
                "detects schemes' uuid field as non-transient" to
                    row(DummyScheme::uuid, false),
                "detects schemes' context field as transient" to
                    row(DummyScheme::context, true),
                "detects decorators' generator field as transient" to
                    row(DummyDecoratorScheme::generator, true),
            )
        ) { (property, isTransient) ->
            property.isTransient() shouldBe isTransient
        }
    }

    context("isSerialized") {
        val cases = mapOf(
            // No annotation
            "immutable primitive is not serialized" to
                row(TransientSerialized::valInt, false),
            "immutable `Scheme` is not serialized" to
                row(TransientSerialized::valScheme, false),
            "immutable `List` is not serialized" to
                row(TransientSerialized::valList, false),
            "immutable `MutableList` is not serialized" to
                row(TransientSerialized::valMutableList, false),
            "mutable primitive is serialized" to
                row(TransientSerialized::varInt, true),
            "mutable `Scheme` is serialized" to
                row(TransientSerialized::varScheme, true),
            "mutable `List` is serialized" to
                row(TransientSerialized::varList, true),
            "mutable `MutableList` is serialized" to
                row(TransientSerialized::varMutableList, true),
            // @OptionTag
            "immutable `List` with @OptionTag is serialized" to
                row(TransientSerialized::valListOptionTag, true),
            "immutable `MutableList` with @OptionTag is serialized" to
                row(TransientSerialized::valMutableListOptionTag, true),
            "mutable primitive with @OptionTag is serialized" to
                row(TransientSerialized::varIntOptionTag, true),
            "mutable `Scheme` with @OptionTag is serialized" to
                row(TransientSerialized::varSchemeOptionTag, true),
            "mutable `List` with @OptionTag is serialized" to
                row(TransientSerialized::varListOptionTag, true),
            "mutable `MutableList` with @OptionTag is serialized" to
                row(TransientSerialized::varMutableListOptionTag, true),
            // @XCollection
            "immutable `List` with @XCollection is serialized" to
                row(TransientSerialized::valListXCollection, true),
            "immutable `MutableList` with @XCollection is serialized" to
                row(TransientSerialized::valMutableListXCollection, true),
            "mutable `List` with @XCollection is serialized" to
                row(TransientSerialized::varListXCollection, true),
            "mutable `MutableList` with @XCollection is serialized" to
                row(TransientSerialized::varMutableListXCollection, true),
        )

        context("isSerialized method") {
            withData(cases) { (property, isSerialized) ->
                property.isSerialized() shouldBe isSerialized
            }
        }

        context("is actually serialized") {
            withData(cases) { (property, _) ->
                val xml = serialize(TransientSerialized())
                val xmlHasProperty = xml.getProperty(property.name) != null

                property.isSerialized() shouldBe xmlHasProperty
            }
        }
    }

    context("mutated") {
        withData(
            mapOf(
                // Primitive(-ish)
                "null" to row(null, "foo"),
                "false" to row(false, true),
                "true" to row(true, false),
                "integer" to row(79, 80),
                "long" to row(448L, 449L),
                "float" to row(181.57f, 182.57f),
                "double" to row(995.67, 996.67),
                "string" to row("frank", "foo_frank"),
                "enum" to row(CapitalizationMode.UPPER, CapitalizationMode.LOWER),
                // Scheme
                "scheme with mutable parameters" to
                    row(SchemeMutable(0, "bar"), SchemeMutable(1, "foo_bar")),
                "scheme with immutable parameters" to
                    row(SchemeImmutable(0, "bar"), SchemeImmutable(0, "bar")),
                "scheme with val immutable list" to
                    row(SchemeValImmutableList(listOf(0)), SchemeValImmutableList(listOf(0))),
                "scheme with val mutable list" to
                    row(SchemeValMutableList(mutableListOf(0)), SchemeValMutableList(mutableListOf(0))),
                "scheme with var immutable list" to
                    row(SchemeVarImmutableList(listOf(0)), SchemeVarImmutableList(listOf(1, 0))),
                "scheme with var mutable list" to
                    row(SchemeVarMutableList(mutableListOf(0)), SchemeVarMutableList(mutableListOf(1, 0))),
                "scheme with some transient fields" to
                    row(SchemeTransient(foo = 0, bar = 0), SchemeTransient(foo = 0, bar = 1)),
                // List
                "string list" to row(listOf("a", "b"), listOf("foo_a", "foo_b", "foo")),
                "scheme list" to row(
                    listOf(IntegerScheme(), StringScheme()),
                    listOf(IntegerScheme().mutated(), StringScheme().mutated(), IntegerScheme())
                ),
            )
        ) { (before, after) ->
            before.mutated() shouldBe after
        }
    }
})


/**
 * A [com.fwdekker.randomness.State] that cannot [deepCopy]. Purely for tests.
 */
internal open class SimpleState : State() {
    override fun deepCopy(retainUuid: Boolean): State = error("Not implemented.")
}


internal class FieldsOnly : SimpleState() {
    var foo: Int = 0
    val bar: Int = 0
}

internal class ParametersOnly(var foo: Int = 0) : SimpleState()

internal class ParametersAndFields(var foo: Int = 0) : SimpleState() {
    var bar: Int = 0
}

internal open class ParametersSuper(var foo: Int = 0) : SimpleState() {
    var bar: Int = 0
}

internal class ParametersAndFieldsSub(var baz: Int = 0) : ParametersSuper() {
    var qux: Int = 0
}

internal open class TransientAnnotationsSuper {
    var superNotTransient: Int = 0

    @Transient
    var superTransient: Int = 0

    @field:Transient
    var superFieldTransient: Int = 0

    @get:Transient
    var superGetTransient: Int = 0

    @set:Transient
    var superSetTransient: Int = 0

    @Transient
    open var onlySuperTransient: Int = 0

    @field:Transient
    open var onlySuperFieldTransient: Int = 0

    @get:Transient
    open var onlySuperGetTransient: Int = 0

    @set:Transient
    open var onlySuperSetTransient: Int = 0

    open var onlySubTransient: Int = 0

    open var onlySubFieldTransient: Int = 0

    open var onlySubGetTransient: Int = 0

    open var onlySubSetTransient: Int = 0
}

internal class TransientAnnotations(
    var constructorNotTransient: Int = 0,
    @Transient var constructorTransient: Int = 0,
    @field:Transient var constructorFieldTransient: Int = 0,
    @get:Transient var constructorGetTransient: Int = 0,
    @set:Transient var constructorSetTransient: Int = 0,
) : TransientAnnotationsSuper() {
    var declaredNotTransient: Int = 0

    @Transient
    var declaredTransient: Int = 0

    @field:Transient
    var declaredFieldTransient: Int = 0

    @get:Transient
    var declaredGetTransient: Int = 0

    @set:Transient
    var declaredSetTransient: Int = 0

    override var onlySuperTransient: Int = 0

    override var onlySuperFieldTransient: Int = 0

    override var onlySuperGetTransient: Int = 0

    override var onlySuperSetTransient: Int = 0

    @Transient
    override var onlySubTransient: Int = 0

    @field:Transient
    override var onlySubFieldTransient: Int = 0

    @get:Transient
    override var onlySubGetTransient: Int = 0

    @set:Transient
    override var onlySubSetTransient: Int = 0
}

internal class TransientSerialized : SimpleState() {
    val valInt: Int = 0

    val valScheme: Scheme = IntegerScheme()

    val valList: List<String> = listOf("foo", "bar")

    val valMutableList: MutableList<String> = mutableListOf("foo", "bar")

    var varInt: Int = 0

    var varScheme: Scheme = IntegerScheme()

    var varList: List<String> = listOf("foo", "bar")

    var varMutableList: MutableList<String> = mutableListOf("foo", "bar")


    @get:OptionTag
    val valListOptionTag: List<String> = listOf("foo", "bar")

    @get:OptionTag
    val valMutableListOptionTag: MutableList<String> = mutableListOf("foo", "bar")

    @get:OptionTag
    var varIntOptionTag: Int = 0

    @get:OptionTag
    var varSchemeOptionTag: Scheme = IntegerScheme()

    @get:OptionTag
    var varListOptionTag: List<String> = listOf("foo", "bar")

    @get:OptionTag
    var varMutableListOptionTag: MutableList<String> = mutableListOf("foo", "bar")


    @get:XCollection
    val valListXCollection: List<String> = listOf("foo", "bar")

    @get:XCollection
    val valMutableListXCollection: MutableList<String> = mutableListOf("foo", "bar")

    @get:XCollection
    var varListXCollection: List<String> = listOf("foo", "bar")

    @get:XCollection
    var varMutableListXCollection: MutableList<String> = mutableListOf("foo", "bar")
}

internal data class SchemeMutable(var foo: Int = 0, var bar: String = "bar") : SimpleState()

internal data class SchemeImmutable(val foo: Int = 0, val bar: String = "bar") : SimpleState()

internal data class SchemeValImmutableList(val list: List<Int> = listOf(0)) : SimpleState()

internal data class SchemeValMutableList(val list: MutableList<Int> = mutableListOf(0)) : SimpleState()

internal data class SchemeVarImmutableList(var list: List<Int> = listOf(0)) : SimpleState()

internal data class SchemeVarMutableList(var list: MutableList<Int> = mutableListOf(0)) : SimpleState()

internal data class SchemeTransient(@Transient var foo: Int = 0, var bar: Int = 0) : SimpleState()
