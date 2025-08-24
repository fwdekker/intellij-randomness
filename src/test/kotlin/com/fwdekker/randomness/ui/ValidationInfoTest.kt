package com.fwdekker.randomness.ui

import com.fwdekker.randomness.integer.IntegerScheme
import com.fwdekker.randomness.testhelpers.DummyScheme
import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.beforeNonContainer
import com.fwdekker.randomness.ui.ValidatorDsl.Companion.validators
import io.kotest.assertions.withClue
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


    lateinit var scheme: IntegerScheme


    beforeNonContainer {
        scheme = IntegerScheme()
        scheme.arrayDecorator.enabled = true
    }


    context("of") {
        lateinit var scheme: DummyScheme


        beforeNonContainer {
            scheme = DummyScheme()
        }


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

        context("checkNoException") {
            test("creates a `Validator` with the DSL's configured `property`") {
                val validators = scheme.validators { of(scheme::name).checkNoException { true } }

                validators.single().property shouldBe scheme::name
            }

            test("creates a validator that fails when an exception is thrown and uses the exception's message") {
                val validators = scheme.validators { of(scheme::name).checkNoException { error("expected") } }

                validators.single().validate()?.message shouldBe "expected"
            }

            test("creates a validator that succeeds when no exception is thrown") {
                val validators = scheme.validators { of(scheme::name).checkNoException { } }

                validators.single().validate() shouldBe null
            }
        }
    }

    context("include") {
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

        test("does not run validation if the condition is false") {
            var isValidated = true
            val validators = scheme.validators { include(scheme::arrayDecorator) { isValidated } }
            validators.validate() shouldBe null

            scheme.arrayDecorator.apply { maxCount = -1 }
            validators.validate() shouldNotBe null

            isValidated = false
            validators.validate() shouldBe null

            isValidated = true
            validators.validate() shouldNotBe null
        }
    }

    context("case") {
        test("adds validators added in its body to the parent's list") {
            val validators = scheme.validators { case({ true }) { of(scheme::name).check { null } } }

            validators.single().property shouldBe scheme::name
        }

        test("adds validators added in its body to the parent's list, even if the condition is false") {
            val validators = scheme.validators { case({ false }) { of(scheme::name).check { null } } }

            validators.single().property shouldBe scheme::name
        }

        test("adds validators included in its body to the parent's list") {
            val validators = scheme.validators { case({ true }) { of(scheme::name).check { null } } }

            validators.single().property shouldBe scheme::name
        }

        test("adds validators included in its body to the parent's list, even if the condition is false") {
            val validators = scheme.validators { case({ false }) { of(scheme::name).check { null } } }

            validators.single().property shouldBe scheme::name
        }

        test("does not override the output of a validator when the condition is `true`") {
            val validators = scheme.validators { case({ true }) { of(scheme::name).check { info("expected") } } }

            validators.validate()?.message shouldBe "expected"
        }

        test("overrides the output of a validator to `null` when the condition is `false`") {
            val validators = scheme.validators { case({ false }) { of(scheme::name).check { info("unexpected") } } }

            validators.validate() shouldBe null
        }

        test("checks the condition only at validation time") {
            var condition = false
            val validators = scheme.validators { case({ condition }) { of(scheme::name).check { info("expected") } } }

            condition = true
            validators.validate()?.message shouldBe "expected"
        }

        test("does not affect validators created before the `case` block") {
            val validators = scheme.validators {
                of(scheme::name).check { info("outer") }
                case({ false }) { of(scheme::name).check { info("inner") } }
            }

            validators.validate()?.message shouldBe "outer"
        }

        test("does not affect validators created after the `case` block") {
            val validators = scheme.validators {
                case({ false }) { of(scheme::name).check { info("inner") } }
                of(scheme::name).check { info("outer") }
            }

            validators.validate()?.message shouldBe "outer"
        }

        test("can be nested to two levels conjunctively") {
            var outer = false
            var inner = false

            val validators = scheme.validators {
                case({ outer }) {
                    case({ inner }) {
                        of(scheme::name).check { info("message") }
                    }
                }
            }

            listOf(true, false).forEach { outer0 ->
                listOf(true, false).forEach { inner0 ->
                    outer = outer0
                    inner = inner0

                    withClue("($outer, $inner)") {
                        if (outer && inner) validators.validate()?.message shouldBe "message"
                        else validators.validate() shouldBe null
                    }
                }
            }
        }

        test("can be nested to three levels conjunctively") {
            var outer = false
            var middle = false
            var inner = false

            val validators = scheme.validators {
                case({ outer }) {
                    case({ middle }) {
                        case({ inner }) {
                            of(scheme::name).check { info("message") }
                        }
                    }
                }
            }

            listOf(true, false).forEach { outer0 ->
                listOf(true, false).forEach { middle0 ->
                    listOf(true, false).forEach { inner0 ->
                        outer = outer0
                        middle = middle0
                        inner = inner0

                        withClue("($outer, $middle, $inner)") {
                            if (outer && middle && inner) validators.validate()?.message shouldBe "message"
                            else validators.validate() shouldBe null
                        }
                    }
                }
            }
        }
    }


    context("validators") {
        test("returns an empty list if the body is empty") {
            val validators = scheme.validators {}

            validators should beEmpty()
        }

        test("does not include manually constructed `Validator` instances") {
            val validators = scheme.validators { Validator(scheme::name) { null } }

            validators should beEmpty()
        }

        test("includes all validators in a chain of `check` calls") {
            val validators = scheme.validators {
                of(scheme::name)
                    .check { null }
                    .check { null }
                    .check { null }
            }

            validators should haveSize(3)
        }

        test("returns the created validators") {
            val validators = scheme.validators {
                of(scheme::name)
                    .check { null }
                    .check { null }
                of(scheme::name)
                    .check({ false }, { "Message" })
                of(scheme::base)
                    .check { info("Message") }
            }

            validators should haveSize(4)
        }
    }
})
