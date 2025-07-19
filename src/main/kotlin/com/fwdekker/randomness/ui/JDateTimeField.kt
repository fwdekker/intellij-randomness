package com.fwdekker.randomness.ui

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.Timestamp
import java.text.ParseException
import javax.swing.JFormattedTextField


/**
 * A [JFormattedTextField] for [Timestamp]s.
 *
 * See [Timestamp] for how inputs are parsed and interpreted. Invalid inputs are never rejected. The [DateTimeFormatter]
 * is used only to coerce inputs into [Timestamp] objects, in doing so normalizing the notation.
 *
 * @param default the default [Timestamp] that is returned in case no [value] has ever been set
 */
class JDateTimeField(private val default: Timestamp = Timestamp("1970")) : JFormattedTextField(DateTimeFormatter()) {
    /**
     * Returns the [Timestamp] contained in this field, or [default] otherwise.
     */
    override fun getValue(): Timestamp = super.getValue() as? Timestamp ?: default

    /**
     * Sets the [Timestamp] that is contained in this field.
     */
    override fun setValue(value: Any) {
        require(value is Timestamp) { Bundle("datetime_field.error.invalid_type") }

        super.setValue(value)
    }


    /**
     * Coerces inputs into [Timestamp] objects.
     *
     * Invalid inputs are kept as-is, and are not rejected.
     */
    class DateTimeFormatter : AbstractFormatter() {
        /**
         * Constructs a [Timestamp] from [text], thereby liberally interpreting the intended timestamp and normalizing
         * the notation.
         *
         * This method will never throw a [ParseException].
         */
        override fun stringToValue(text: String?): Timestamp =
            Timestamp(text ?: "")

        /**
         * Returns the [Timestamp.value] of [value], assuming that [value] is a [Timestamp].
         *
         * @throws ParseException if [value] is not a [Timestamp]
         */
        override fun valueToString(value: Any?): String =
            if (value !is Timestamp) throw ParseException(Bundle("datetime_field.error.invalid_type"), 0)
            else value.value
    }
}


/**
 * Binds two [JDateTimeField]s together, analogous to how [com.fwdekker.randomness.ui.bindSpinners] works.
 */
fun bindDateTimes(minField: JDateTimeField, maxField: JDateTimeField) {
    addChangeListenerTo(minField) {
        if (maxField.value.isBefore(minField.value))
            maxField.value = minField.value
    }

    addChangeListenerTo(maxField) {
        if (maxField.value.isBefore(minField.value))
            minField.value = maxField.value
    }
}
