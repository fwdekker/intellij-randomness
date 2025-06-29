package com.fwdekker.randomness.ui

import com.fwdekker.randomness.integer.IntegerScheme
import com.fwdekker.randomness.testhelpers.DummyScheme
import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import com.fwdekker.randomness.ui.ValidatorDsl.Companion.validators
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.collections.haveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs


/**
 * Unit tests for [ValidationInfo].
 */
object ValidationInfoTest : FunSpec({
    tags(Tags.PLAIN)


    context("prepend") {
        test("prepends the given strings") {
            val scheme = DummyScheme()
            val info = ValidationInfo(scheme, scheme::valid, "Message")

            info.prepend("Foo", "Bar").message shouldBe "FooBarMessage"
        }
    }
})

/**
 * Unit tests for [Validator].
 */
object ValidatorTest : FunSpec({
    tags(Tags.PLAIN)


    context("validate") {
        test("returns the validation function's value") {
            val scheme = DummyScheme(valid = false)
            val info = ValidationInfo(scheme, scheme::valid, "Message")
            val validator = Validator(scheme::name) { info }

            validator.validate() shouldBeSameInstanceAs info
        }

        test("uses the property's value at the moment of invocation") {
            val scheme = DummyScheme(valid = false)
            val validator = Validator(scheme::name) { ValidationInfo(scheme, scheme::valid, it) }

            scheme.name = "Dingo"
            validator.validate()?.message shouldBe "Dingo"

            scheme.name = "Nova"
            validator.validate()?.message shouldBe "Nova"
        }
    }

    context("List.validate") {
        test("returns `null` if the list is empty") {
            val validators = listOf<Validator<*>>()

            validators.validate() shouldBe null
        }

        test("returns `null` if the only validator returns `null`") {
            val validators = listOf(Validator(DummyScheme()::name) { null })

            validators.validate() shouldBe null
        }

        test("returns `null` if all validators return `null`") {
            val scheme = DummyScheme()
            val validators = listOf(
                Validator(scheme::name) { null },
                Validator(scheme::valid) { null },
                Validator(scheme::prefix) { null },
            )

            validators.validate() shouldBe null
        }

        test("returns the first non-full value") {
            val scheme = DummyScheme()
            val validators = listOf(
                Validator(scheme::name) { null },
                Validator(scheme::valid) { ValidationInfo(scheme, scheme::valid, "expected") },
                Validator(scheme::prefix) { ValidationInfo(scheme, scheme::prefix, "wrong") },
            )

            validators.validate()?.message shouldBe "expected"
        }
    }
})


/**
 * Unit tests for [ValidatorDsl] and [ValidatorDsl.OfDsl].
 */
object ValidatorDslTest : FunSpec({
    tags(Tags.PLAIN)


    lateinit var scheme: DummyScheme


    beforeNonContainer {
        scheme = DummyScheme()
    }


    context("of") {
        context("info") {
            test("returns `null` if the given message is `null`") {
                var theInfo: ValidationInfo? = ValidationInfo(scheme, scheme::valid, "wrong")

                scheme.validators { theInfo = of(scheme::valid).info(null) }

                theInfo shouldBe null
            }

            test("returns `ValidationInfo` with the DSL's configured `state` and `property`") {
                var theInfo: ValidationInfo? = null

                scheme.validators { theInfo = of(scheme::valid).info("expected") }

                theInfo?.message shouldBe "expected"
            }
        }

        context("check (with args `validate`)") {
            test("creates a `Validator` with the DSL's configured `property`") {
                val validators = scheme.validators { of(scheme::name).check { null } }

                validators.single().property shouldBe scheme::name
            }

            test("adds the created validator to the outer DSL's return value") {
                val info = ValidationInfo(scheme, scheme::valid, scheme.name)

                val validators = scheme.validators { of(scheme::valid).check { info } }

                validators.single().validate() shouldBeSameInstanceAs info
            }
        }

        context("check (with args `isValid` and `message`)") {
            test("creates a `Validator` with the DSL's configured `property`") {
                val validators = scheme.validators { of(scheme::name).check({ true }, { "wrong" }) }

                validators.single().property shouldBe scheme::name
            }

            test("creates a validator that fails when the condition is false, and uses the message") {
                val validators = scheme.validators { of(scheme::name).check({ false }, { "expected" }) }

                validators.single().validate()?.message shouldBe "expected"
            }

            test("checks the condition and message at the moment of invocation") {
                val validators = scheme.validators { of(scheme::valid).check({ it }, { scheme.name }) }

                scheme.valid = true
                validators.single().validate() shouldBe null

                scheme.name = "expected"
                scheme.valid = false
                validators.single().validate()?.message shouldBe "expected"
            }

            test("creates a validator that succeeds when the condition is true") {
                val validators = scheme.validators { of(scheme::name).check({ true }, { "wrong" }) }

                validators.single().validate() shouldBe null
            }
        }
    }

    context("include") {
        lateinit var scheme: IntegerScheme


        beforeNonContainer {
            scheme = IntegerScheme()
        }


        test("adds the property's validators to the list") {
            val validators = scheme.validators { include(scheme::arrayDecorator) }
            validators.validate() shouldBe null

            scheme.arrayDecorator.apply { maxCount = -1 }
            validators.validate() shouldNotBe null
        }

        test("uses the property's value at the moment of invocation") {
            val validators = scheme.validators { include(scheme::arrayDecorator) }
            validators.validate() shouldBe null

            scheme.arrayDecorator.apply { maxCount = -1 }
            validators.validate() shouldNotBe null

            scheme.arrayDecorator.apply { maxCount = minCount + 1 }
            validators.validate() shouldBe null
        }

        @Suppress("AssignedValueIsNeverRead") // False positive
        test("does not run validation if the condition is false") {
            var enabled = true
            val validators = scheme.validators { include(scheme::arrayDecorator) { enabled } }
            validators.validate() shouldBe null

            scheme.arrayDecorator.apply { maxCount = -1 }
            validators.validate() shouldNotBe null

            enabled = false
            validators.validate() shouldBe null

            enabled = true
            validators.validate() shouldNotBe null
        }
    }

    context("validators") {
        test("returns an empty list if the body is empty") {
            val validators = scheme.validators {}

            validators should beEmpty()
        }

        test("does not include manually constructed `Validator` instances") {
            val validators = scheme.validators { Validator(scheme::valid) { null } }

            validators should beEmpty()
        }

        test("includes all validators in a chain of `check` calls") {
            val validators = scheme.validators {
                of(scheme::valid)
                    .check { null }
                    .check { null }
                    .check { null }
            }

            validators should haveSize(3)
        }

        test("returns the created validators") {
            val validators = scheme.validators {
                of(scheme::valid)
                    .check { null }
                    .check { null }
                of(scheme::name)
                    .check({ false }, { "Message" })
                of(scheme::prefix)
                    .check { info("Message") }
            }

            validators should haveSize(4)
        }
    }
})
