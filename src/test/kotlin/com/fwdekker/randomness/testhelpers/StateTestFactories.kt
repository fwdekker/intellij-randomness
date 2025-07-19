package com.fwdekker.randomness.testhelpers

import com.fwdekker.randomness.State
import com.fwdekker.randomness.lowerCaseFirst
import com.intellij.util.xmlb.XmlSerializer.deserialize
import com.intellij.util.xmlb.XmlSerializer.serialize
import io.kotest.core.spec.style.funSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe


/**
 * Tests the [com.fwdekker.randomness.State.deepCopy] method of the state created by [createState].
 */
fun <S : State> stateDeepCopyTestFactory(createState: () -> S) =
    funSpec {
        context("deepCopy") {
            lateinit var state: S


            beforeNonContainer {
                state = createState()
            }


            test("equals old instance") {
                state.deepCopy() shouldBe state
            }

            test("is independent of old instance") {
                val copy = state.deepCopy()

                state.uuid = "other"

                copy.uuid shouldNotBe state.uuid
            }

            test("retains uuid if chosen") {
                state.deepCopy(true).uuid shouldBe state.uuid
            }

            test("replaces uuid if chosen") {
                state.deepCopy(false).uuid shouldNotBe state.uuid
            }
        }
    }

/**
 * Tests that [com.fwdekker.randomness.PersistentSettings] serializes and deserializes [S] correctly without losing user
 * configuration.
 */
fun <S : State> stateSerializationTestFactory(createState: () -> S) =
    funSpec {
        fun <T> reserialize(any: Any, clz: Class<T>): T =
            deserialize(serialize(deserialize(serialize(any), clz)), clz)


        context("(de)serialization") {
            test("mutation is not a no-op") {
                createState().mutated() shouldNotBe createState()
            }

            test("retains default values") {
                val state = createState()
                reserialize(createState(), state::class.java) shouldBe state
            }

            test("retains modified values") {
                val state = createState().mutated()
                reserialize(state, state::class.java) shouldBe state
            }

            test("serializes exactly the properties for which `isSerialized` holds") {
                val state = createState()
                val xml = serialize(state)

                val beanProps = state.properties()
                    .filter { it.isSerialized() }
                    .map { it.name.removePrefix("is").lowerCaseFirst() }
                val xmlProps = xml.children.mapNotNull { it.getAttribute("name")?.value }

                xmlProps shouldContainExactlyInAnyOrder beanProps
            }
        }
    }
