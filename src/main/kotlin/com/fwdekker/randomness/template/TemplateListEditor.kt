package com.fwdekker.randomness.template

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.Scheme
import com.fwdekker.randomness.SchemeEditor
import com.fwdekker.randomness.Settings
import com.fwdekker.randomness.datetime.DateTimeScheme
import com.fwdekker.randomness.datetime.DateTimeSchemeEditor
import com.fwdekker.randomness.decimal.DecimalScheme
import com.fwdekker.randomness.decimal.DecimalSchemeEditor
import com.fwdekker.randomness.integer.IntegerScheme
import com.fwdekker.randomness.integer.IntegerSchemeEditor
import com.fwdekker.randomness.setAll
import com.fwdekker.randomness.string.StringScheme
import com.fwdekker.randomness.string.StringSchemeEditor
import com.fwdekker.randomness.template.TemplateListEditor.Companion.useTestSplitter
import com.fwdekker.randomness.ui.PreviewPanel
import com.fwdekker.randomness.ui.ValidationInfo
import com.fwdekker.randomness.ui.addChangeListenerTo
import com.fwdekker.randomness.ui.focusLater
import com.fwdekker.randomness.uuid.UuidScheme
import com.fwdekker.randomness.uuid.UuidSchemeEditor
import com.fwdekker.randomness.word.WordScheme
import com.fwdekker.randomness.word.WordSchemeEditor
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBSplitter
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBEmptyBorder
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.KeyboardFocusManager
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities


/**
 * Component for editing a [TemplateList]s.
 *
 * The editor essentially has a master-detail layout. On the left, a [TemplateJTree] shows all templates and the schemes
 * contained within them, and on the right, a [SchemeEditor] for the currently-selected template or scheme is shown.
 *
 * @property originalTemplateList The templates to edit.
 * @param initialSelection the UUID of the scheme to select initially, or `null` or an invalid UUID to select the first
 * template
 * @see TemplateListConfigurable
 */
class TemplateListEditor(
    val originalTemplateList: TemplateList = Settings.DEFAULT.templateList,
    initialSelection: String? = null,
) : Disposable {
    /**
     * The root component of the editor.
     */
    val rootComponent = JPanel(BorderLayout())

    private val currentTemplateList = TemplateList() // Synced with [originalTemplateList] in [reset]
    private val templateTree = TemplateJTree(originalTemplateList, currentTemplateList)
    private val schemeEditorPanel = JPanel(BorderLayout())
    private var schemeEditor: SchemeEditor<*>? = null
    private val previewPanel =
        PreviewPanel { templateTree.selectedTemplate ?: StringScheme("") }
            .also { Disposer.register(this, it) }


    init {
        val splitter = createSplitter()
        rootComponent.add(splitter, BorderLayout.CENTER)

        // Left half
        templateTree.addTreeSelectionListener { onTreeSelection() }
        splitter.firstComponent = templateTree.asDecoratedPanel()

        // Right half
        addChangeListenerTo(templateTree) { previewPanel.updatePreview() }
        schemeEditorPanel.border = JBEmptyBorder(EDITOR_PANEL_MARGIN)
        schemeEditorPanel.add(previewPanel.rootComponent, BorderLayout.SOUTH)

        splitter.secondComponent = schemeEditorPanel

        // Load current state
        reset()
        templateTree.expandAll()

        // Select a scheme
        initialSelection
            ?.let { currentTemplateList.getSchemeByUuid(it) }
            ?.also {
                templateTree.selectedScheme = it
                schemeEditor?.preferredFocusedComponent?.focusLater()
            }
    }

    /**
     * Invoked when an entry is (de)selected in the tree.
     */
    private fun onTreeSelection() {
        // Remove old editor
        schemeEditor?.also { editor ->
            schemeEditorPanel.remove((schemeEditorPanel.layout as BorderLayout).getLayoutComponent(BorderLayout.CENTER))
            schemeEditor = null
            schemeEditorPanel.revalidate() // Hide editor immediately

            Disposer.dispose(editor)
        }

        // Create new editor
        val selectedState = templateTree.selectedScheme ?: return
        schemeEditor = createEditor(selectedState)
            .also { editor ->
                editor.addChangeListener {
                    editor.apply()
                    templateTree.reload(selectedState)
                }
                editor.apply() // Apply validation fixes from UI

                schemeEditorPanel.add(
                    JBScrollPane(editor.rootComponent).also {
                        it.border = null
                        it.preferredSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
                    },
                    BorderLayout.CENTER
                )

                ScrollOnFocusListener(KeyboardFocusManager.getCurrentKeyboardFocusManager(), editor).install()
            }

        // Show new editor
        templateTree.reload(selectedState)
        schemeEditorPanel.revalidate() // Show editor immediately
        schemeEditor?.doValidate() // Forcefully show validation information
    }

    /**
     * Creates a new splitter.
     *
     * If a test that depends on [TemplateListEditor] freezes for a long time or fails to initialize the UI, try
     * setting [useTestSplitter] to `true`.
     */
    private fun createSplitter() =
        if (useTestSplitter) JBSplitter(false, SPLITTER_PROPORTION_KEY, DEFAULT_SPLITTER_PROPORTION)
        else OnePixelSplitter(false, SPLITTER_PROPORTION_KEY, DEFAULT_SPLITTER_PROPORTION)

    /**
     * Creates an editor to edit [scheme].
     */
    private fun createEditor(scheme: Scheme) =
        when (scheme) {
            is IntegerScheme -> IntegerSchemeEditor(scheme)
            is DecimalScheme -> DecimalSchemeEditor(scheme)
            is StringScheme -> StringSchemeEditor(scheme)
            is UuidScheme -> UuidSchemeEditor(scheme)
            is WordScheme -> WordSchemeEditor(scheme)
            is DateTimeScheme -> DateTimeSchemeEditor(scheme)
            is TemplateReference -> TemplateReferenceEditor(scheme)
            is Template -> TemplateEditor(scheme)
            else -> error(Bundle("template_list.error.unknown_scheme_type", scheme.javaClass.canonicalName))
        }.also { Disposer.register(this, it) }


    /**
     * Validates the state of the editor and indicates whether and why it is invalid.
     *
     * @return `null` if the state is valid, or a string explaining why the state is invalid
     */
    fun doValidate(): ValidationInfo? = currentTemplateList.doValidate()

    /**
     * Returns `true` if and only if the editor contains modifications relative to the last saved state.
     */
    fun isModified() = originalTemplateList != currentTemplateList

    /**
     * Saves the editor's state into [originalTemplateList].
     *
     * Does nothing if and only if [isModified] returns `false`.
     */
    fun apply() {
        originalTemplateList.templates.setAll(currentTemplateList.deepCopy(retainUuid = true).templates)
        originalTemplateList.applyContext((+originalTemplateList.context).copy(templateList = originalTemplateList))
    }

    /**
     * Resets the editor's state to the last saved state.
     *
     * Does nothing if and only if [isModified] return `false`.
     */
    fun reset() {
        currentTemplateList.templates.setAll(originalTemplateList.deepCopy(retainUuid = true).templates)
        currentTemplateList.applyContext((+currentTemplateList.context).copy(templateList = currentTemplateList))

        templateTree.reload()
    }

    /**
     * Disposes this editor's resources.
     */
    override fun dispose() = Unit


    /**
     * Explicitly selects the given scheme (based on its UUID).
     */
    fun selectScheme(scheme: Scheme) {
        templateTree.selectedScheme = scheme
    }


    /**
     * Scrolls to the focused element within the [editor], registering this listener with the [focusManager], and
     * automatically disposing itself when the [editor] is disposed.
     */
    private class ScrollOnFocusListener<S : Scheme>(
        private val focusManager: KeyboardFocusManager,
        private val editor: SchemeEditor<S>,
    ) : PropertyChangeListener, Disposable {
        /**
         * Scrolls to the newly focused element if that element is in the [editor].
         */
        override fun propertyChange(event: PropertyChangeEvent) {
            val focused = event.newValue as? JComponent
            if (focused == null || !editor.rootComponent.isAncestorOf(focused))
                return

            val target = SwingUtilities.convertRectangle(focused, focused.bounds, editor.rootComponent)
            editor.rootComponent.scrollRectToVisible(target)
        }


        /**
         * Installs this listener with the [focusManager].
         */
        fun install() {
            Disposer.register(editor, this)
            focusManager.addPropertyChangeListener("focusOwner", this)
        }

        /**
         * Removes this listener from the [focusManager].
         */
        override fun dispose() = focusManager.removePropertyChangeListener("focusOwner", this)
    }


    /**
     * Holds constants.
     */
    companion object {
        /**
         * The key to store the user's last-used splitter proportion under.
         */
        const val SPLITTER_PROPORTION_KEY = "com.fwdekker.randomness.template.TemplateListEditor"

        /**
         * The default proportion of the splitter component.
         */
        const val DEFAULT_SPLITTER_PROPORTION = .25f

        /**
         * Pixels of margin outside the editor panel.
         */
        const val EDITOR_PANEL_MARGIN = 10

        /**
         * Whether [createSplitter] should use a separate kind of splitter that is more compatible with tests.
         */
        var useTestSplitter: Boolean = false
    }
}
