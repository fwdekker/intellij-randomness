package com.fwdekker.randomness.uid

import com.fwdekker.randomness.affix.AffixDecorator
import com.fwdekker.randomness.array.ArrayDecorator
import com.fwdekker.randomness.testhelpers.Tags
import com.fwdekker.randomness.testhelpers.shouldValidateAsBundle
import com.fwdekker.randomness.testhelpers.stateDeepCopyTestFactory
import com.fwdekker.randomness.testhelpers.stateSerializationTestFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.row
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch


/**
 * Unit tests for [UidScheme].
 */
object UidSchemeTest : FunSpec({
    tags(Tags.PLAIN, Tags.SCHEME)


    context("generateStrings") {
        context("UUID") {
            withData(
                mapOf(
                    "generates lowercase UUIDv4 with dashes by default" to
                        row(
                            UidScheme(idTypeKey = IdType.Uuid.key),
                            Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
                        ),
                    "generates uppercase UUID if enabled" to
                        row(
                            UidScheme(
                                idTypeKey = IdType.Uuid.key,
                                uuidConfig = UuidConfig(isUppercase = true)
                            ),
                            Regex("[0-9A-F]{8}-[0-9A-F]{4}-4[0-9A-F]{3}-[89AB][0-9A-F]{3}-[0-9A-F]{12}")
                        ),
                    "generates UUID without dashes if disabled" to
                        row(
                            UidScheme(
                                idTypeKey = IdType.Uuid.key,
                                uuidConfig = UuidConfig(addDashes = false)
                            ),
                            Regex("[0-9a-f]{8}[0-9a-f]{4}4[0-9a-f]{3}[89ab][0-9a-f]{3}[0-9a-f]{12}")
                        ),
                    "generates UUIDv1" to
                        row(
                            UidScheme(
                                idTypeKey = IdType.Uuid.key,
                                uuidConfig = UuidConfig(version = 1)
                            ),
                            Regex("[0-9a-f]{8}-[0-9a-f]{4}-1[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
                        ),
                    "generates UUIDv6" to
                        row(
                            UidScheme(
                                idTypeKey = IdType.Uuid.key,
                                uuidConfig = UuidConfig(version = 6)
                            ),
                            Regex("[0-9a-f]{8}-[0-9a-f]{4}-6[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
                        ),
                    "generates UUIDv7" to
                        row(
                            UidScheme(
                                idTypeKey = IdType.Uuid.key,
                                uuidConfig = UuidConfig(version = 7)
                            ),
                            Regex("[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
                        ),
                    "generates UUIDv8" to
                        row(
                            UidScheme(
                                idTypeKey = IdType.Uuid.key,
                                uuidConfig = UuidConfig(version = 8)
                            ),
                            Regex("[0-9a-f]{8}-[0-9a-f]{4}-8[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
                        ),
                )
            ) { (scheme, pattern) -> scheme.generateStrings()[0] shouldMatch pattern }
        }

        context("NanoID") {
            withData(
                mapOf(
                    "generates NanoID with default size and alphabet" to
                        row(
                            UidScheme(idTypeKey = IdType.NanoId.key),
                            Regex("[_\\-0-9a-zA-Z]{21}")
                        ),
                    "generates NanoID with custom size" to
                        row(
                            UidScheme(
                                idTypeKey = IdType.NanoId.key,
                                nanoIdConfig = NanoIdConfig(size = 10)
                            ),
                            Regex("[_\\-0-9a-zA-Z]{10}")
                        ),
                    "generates NanoID with custom alphabet" to
                        row(
                            UidScheme(
                                idTypeKey = IdType.NanoId.key,
                                nanoIdConfig = NanoIdConfig(alphabet = "abc")
                            ),
                            Regex("[abc]{21}")
                        ),
                )
            ) { (scheme, pattern) -> scheme.generateStrings()[0] shouldMatch pattern }
        }

        test("applies decorators in order affix, array") {
            val scheme = UidScheme(
                idTypeKey = IdType.Uuid.key,
                uuidConfig = UuidConfig(addDashes = false),
                affixDecorator = AffixDecorator(enabled = true, descriptor = "@!"),
                arrayDecorator = ArrayDecorator(enabled = true, minCount = 2, maxCount = 2, separator = ", "),
            )

            scheme.generateStrings()[0] shouldMatch
                Regex("\\[[0-9a-f]{32}!, [0-9a-f]{32}!\\]")
        }
    }

    context("doValidate") {
        context("UUID") {
            withData(
                mapOf(
                    "succeeds for default UUID state" to
                        row(UidScheme(idTypeKey = IdType.Uuid.key), null),
                    "fails if UUID version is unsupported" to
                        row(
                            UidScheme(
                                idTypeKey = IdType.Uuid.key,
                                uuidConfig = UuidConfig(version = 3)
                            ),
                            "uuid.error.unknown_version"
                        ),
                )
            ) { (scheme, validation) -> scheme shouldValidateAsBundle validation }
        }

        context("NanoID") {
            withData(
                mapOf(
                    "succeeds for default NanoID state" to
                        row(UidScheme(idTypeKey = IdType.NanoId.key), null),
                    "fails if NanoID size is too low" to
                        row(
                            UidScheme(
                                idTypeKey = IdType.NanoId.key,
                                nanoIdConfig = NanoIdConfig(size = 0)
                            ),
                            "nanoid.error.size_too_low"
                        ),
                    "fails if NanoID alphabet is empty" to
                        row(
                            UidScheme(
                                idTypeKey = IdType.NanoId.key,
                                nanoIdConfig = NanoIdConfig(alphabet = "")
                            ),
                            "nanoid.error.alphabet_empty"
                        ),
                )
            ) { (scheme, validation) -> scheme shouldValidateAsBundle validation }
        }

        withData(
            mapOf(
                "fails if affix decorator is invalid" to
                    row(UidScheme(affixDecorator = AffixDecorator(enabled = true, descriptor = """\""")), ""),
                "fails if array decorator is invalid" to
                    row(UidScheme(arrayDecorator = ArrayDecorator(enabled = true, minCount = -24)), ""),
            )
        ) { (scheme, validation) -> scheme shouldValidateAsBundle validation }
    }

    include(stateDeepCopyTestFactory { UidScheme() })

    include(stateSerializationTestFactory { UidScheme() })
})
