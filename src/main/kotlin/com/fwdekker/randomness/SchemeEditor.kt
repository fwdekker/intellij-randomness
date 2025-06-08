package com.fwdekker.randomness

import com.fwdekker.randomness.ui.addChangeListenerTo
import com.fwdekker.randomness.ui.focusLater
import com.fwdekker.randomness.ui.validate
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.Cell
import javax.swing.JComponent
import kotlin.reflect.KProperty
import com.intellij.openapi.ui.ValidationInfo as JBValidationInfo


/**
 * An editor for a [Scheme].
 *
 * @param S the type of scheme edited in this editor
 * @property scheme The scheme edited in this editor.
 */
abstract class SchemeEditor<S : Scheme>(val scheme: S) : Disposable {
    /**
     * The root component of the editor.
     */
    abstract val rootComponent: DialogPanel

    /**
     * The components contained within this editor that determine the editor's current state.
     */
    val components: Collection<Any>
        get() = rootComponent.components.filterNot { it.name == null } + extraComponents

    /**
     * The additional [components] that determine the editor's current state but do not have a name.
     *
     * Do not register [SchemeEditor]s here; use the [decoratorEditors] field for that.
     */
    protected val extraComponents = mutableListOf<Any>()

    /**
     * The [SchemeEditor]s of [scheme]'s [DecoratorScheme]s.
     *
     * The editors registered in this list are automatically reset and applied in [reset] and [apply], respectively.
     */
    protected val decoratorEditors = mutableListOf<SchemeEditor<*>>()

    /**
     * The component that this editor prefers to be focused when the editor is focused.
     */
    open val preferredFocusedComponent: JComponent?
        get() = components.filterIsInstance<JComponent>().firstOrNull { it.isVisible }


    /**
     * Maps cells (containing UI components) to the properties they describe.
     */
    private val cellToProperty = mutableListOf<Pair<Cell<*>, KProperty<*>>>()

    /**
     * Registers the fact that `this` [Cell] contains the component corresponding to the given [property].
     *
     * Validation callbacks are only actually added during [implementBindValidations], which is called by [finalize],
     * because the validation callbacks should not precede auto-fix callbacks such as those added by
     * [com.fwdekker.randomness.ui.bindSpinners], for example.
     */
    protected fun <T : JComponent> Cell<T>.bindValidation(property: KProperty<*>): Cell<T> {
        cellToProperty.add(this to property)
        return this
    }

    /**
     * Sets up validation callbacks for the cells on which [bindValidation] has been invoked.
     */
    private fun implementBindValidations() {
        cellToProperty.forEach { (cell, validation) ->
            val validate = {
                scheme.validators
                    .filter { it.property == validation }
                    .validate()
                    ?.let { JBValidationInfo(it.message, cell.component) }
            }

            cell
                .validationRequestor { callback -> addChangeListenerTo(cell.component, listener = callback) }
                .validationOnInput { validate() }
                .validationOnApply { validate() }
        }
    }

    /**
     * Applies a few final common steps to the created [DialogPanel].
     *
     * This method cannot also call [reset], because [reset] requires that the entire [SchemeEditor] has completed
     * initialization.
     */
    protected fun <S : Scheme> DialogPanel.finalize(owner: SchemeEditor<S>): DialogPanel {
        implementBindValidations()
        this.registerValidators(owner)

        return this
    }


    /**
     * Resets the editor's state to that of [scheme].
     *
     * If [apply] has been called, then [reset] resets to the state at the last invocation of [apply].
     *
     * This method must not be called before the `init` block of the subclass, because this method invokes callbacks
     * which assume that the [SchemeEditor] has fully initialized.
     */
    fun reset() {
        rootComponent.reset()
        decoratorEditors.forEach { it.reset() }
    }

    /**
     * Saves the editor's state into [scheme].
     */
    fun apply() {
        rootComponent.apply()
        decoratorEditors.forEach { it.apply() }
    }

    /**
     * Validates every single field individually, displays error information inside the form, and moves focus to the
     * first field that is erroneous.
     */
    fun doValidate() {
        // Uses `toList` to ensure this does not stop prematurely at first invalid field
        (listOf(rootComponent) + decoratorEditors.map { it.rootComponent })
            .toList()
            .firstNotNullOfOrNull { it.validateAll().toList().firstOrNull() }
            ?.component
            ?.focusLater()
    }


    /**
     * Ensures [listener] is invoked on every change in this editor.
     */
    @Suppress("detekt:SpreadOperator") // Acceptable because this method is called rarely
    fun addChangeListener(listener: () -> Unit) =
        addChangeListenerTo(*(components + decoratorEditors).toTypedArray(), listener = listener)


    /**
     * Disposes this editor's resources.
     */
    override fun dispose() = decoratorEditors.forEach { Disposer.dispose(it) }
}
