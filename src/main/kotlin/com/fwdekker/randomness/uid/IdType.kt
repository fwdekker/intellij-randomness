package com.fwdekker.randomness.uid

import com.fwdekker.randomness.Bundle


/**
 * Represents the type of unique identifier to generate.
 *
 * This sealed class allows for type-safe selection of ID types and is designed
 * to be extensible for adding new ID types in the future.
 */
sealed class IdType {
    /**
     * The display name shown in the UI dropdown.
     */
    abstract val displayName: String

    /**
     * A unique key used for serialization and identification.
     */
    abstract val key: String


    /**
     * UUID (Universally Unique Identifier) type.
     */
    data object Uuid : IdType() {
        override val displayName: String get() = Bundle("uid.type.uuid")
        override val key: String = "uuid"
    }

    /**
     * NanoID type - a tiny, secure, URL-friendly unique string ID generator.
     */
    data object NanoId : IdType() {
        override val displayName: String get() = Bundle("uid.type.nanoid")
        override val key: String = "nanoid"
    }


    companion object {
        /**
         * All available ID types.
         */
        val entries: List<IdType> get() = listOf(Uuid, NanoId)

        /**
         * The default ID type.
         */
        val DEFAULT: IdType get() = Uuid

        /**
         * Returns the [IdType] with the given [key], or [DEFAULT] if not found.
         */
        fun fromKey(key: String): IdType = entries.find { it.key == key } ?: DEFAULT
    }
}
