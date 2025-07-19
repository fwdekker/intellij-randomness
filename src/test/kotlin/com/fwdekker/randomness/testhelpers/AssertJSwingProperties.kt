package com.fwdekker.randomness.testhelpers

import com.fwdekker.randomness.ui.JDateTimeField
import com.intellij.ui.dsl.builder.MutableProperty
import org.assertj.swing.fixture.AbstractTwoStateButtonFixture
import org.assertj.swing.fixture.JComboBoxFixture
import org.assertj.swing.fixture.JSpinnerFixture
import org.assertj.swing.fixture.JTextComponentFixture
import javax.swing.AbstractButton
import kotlin.reflect.KMutableProperty0


/**
 * Converts a [KMutableProperty0] of type [T] into a [MutableProperty] of type `Any?`.
 */
@Suppress("UNCHECKED_CAST")
fun <T> KMutableProperty0<T>.prop(): MutableProperty<Any?> = MutableProperty({ get() }, { set(it as T) })

/**
 * Creates a [MutableProperty] of type `Any?` from the given [get] and [set] functions.
 */
@Suppress("UNCHECKED_CAST")
fun <SELF, FIELD> SELF.prop(get: (SELF) -> (() -> FIELD), set: (SELF) -> ((FIELD) -> Unit)): MutableProperty<Any?> =
    MutableProperty(get(this)) { set(this)(it as FIELD) }


/**
 * Creates a [MutableProperty] for the [AbstractButton.isSelected] field.
 *
 * Required until issue KT-8575 is solved.
 */
fun <S, T : AbstractButton> AbstractTwoStateButtonFixture<S, T>.isSelectedProp() =
    this.prop({ this.target()::isSelected }, { this.target()::setSelected })

/**
 * Creates a [MutableProperty] for the [javax.swing.JComboBox.getSelectedItem] field.
 *
 * Required until issue KT-8575 is solved.
 */
fun JComboBoxFixture.itemProp() = this.prop({ this.target()::getSelectedItem }, { this.target()::setSelectedItem })

/**
 * Creates a [MutableProperty] for the [javax.swing.ComboBoxEditor.getItem] field.
 *
 * Required until issue KT-8575 is solved.
 */
fun JComboBoxFixture.textProp() = this.prop({ this.target().editor::getItem }, { this.target().editor::setItem })

/**
 * Creates a [MutableProperty] for the [javax.swing.JSpinner.getValue] field.
 *
 * Required until issue KT-8575 is solved.
 */
fun JSpinnerFixture.valueProp() = this.prop({ this.target()::getValue }, { this.target()::setValue })

/**
 * Creates a [MutableProperty] for [JDateTimeField]'s contained [com.fwdekker.randomness.Timestamp].
 *
 * Required until issue KT-8575 is solved.
 */
fun JTextComponentFixture.timestampProp() =
    (this.target() as JDateTimeField).let { target -> this.prop({ target::getValue }, { target::setValue }) }

/**
 * Creates a [MutableProperty] for the [javax.swing.text.JTextComponent.getText] field.
 *
 * Required until issue KT-8575 is solved.
 */
fun JTextComponentFixture.textProp() = this.prop({ this.target()::getText }, { this.target()::setText })
