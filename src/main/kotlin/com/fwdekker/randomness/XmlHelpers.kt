package com.fwdekker.randomness

import org.jdom.Element


/**
 * Adds a property with given [name] and [value].
 *
 * A property is a child element of type `option`, with an attribute `name` and optionally an attribute `value`. A
 * property may have its own children, too.
 *
 * A property is added even if another property with the given [name] already exists.
 */
fun Element.addProperty(name: String, value: String? = null) {
    addContent(
        Element("option")
            .apply {
                setAttribute("name", name)
                if (value != null) setAttribute("value", value)
            }
    )
}

/**
 * Returns the single child with attribute `name="[name]"`, or `null` if there is not exactly one such child.
 */
fun Element.getProperty(name: String): Element? =
    getMultiProperty(name).singleOrNull()

/**
 * Returns all children with attribute `name="[name]"`.
 */
fun Element.getMultiProperty(name: String): List<Element> =
    children.filter { it.getAttribute("name")?.value == name }

/**
 * Traverses a path of [Element]s based on their [names] by monadically either calling [getProperty] (if the name is not
 * `null`) or taking the single child (if the name is `null`).
 */
fun Element.getPropertyByPath(vararg names: String?): Element? =
    names.fold(this as? Element) { acc, name ->
        if (name == null) acc?.children?.singleOrNull()
        else acc?.getProperty(name)
    }

/**
 * Returns the value of the `value` attribute of property [name], or `null` if the property does not exist, the property
 * has no value, or there are multiple properties with the given [name].
 */
fun Element.getPropertyValue(name: String): String? =
    getProperty(name)?.getAttribute("value")?.value

/**
 * Returns the values of all properties with the given [name].
 *
 * Similar to [getPropertyValue], but works for multiple properties with the same name, and does not return `null`.
 */
fun Element.getMultiPropertyValue(name: String): List<String> =
    getMultiProperty(name).mapNotNull { it.getAttribute("value")?.value }

/**
 * Sets the value of the `value` attribute of property [name], or adds the property if it does not exist.
 *
 * If multiple properties have the same [name], their values are all set to [value].
 *
 * If [value] is `null`, then the `value` attribute will be removed from all properties with the given [name].
 */
fun Element.setPropertyValue(name: String, value: String?) {
    getMultiProperty(name)
        .also { props ->
            if (props.isEmpty()) addProperty(name, value)
            else if (value == null) props.forEach { it.removeAttribute("value") }
            else props.forEach { it.setAttribute("value", value) }
        }
}

/**
 * Renames the property from [oldName] to [newName].
 *
 * If there is no property with [oldName], nothing happens. If multiple properties have the same [oldName], they are all
 * renamed to [newName].
 *
 * Requires that there is not already a property with [newName].
 */
fun Element.renameProperty(oldName: String, newName: String) {
    require(getMultiProperty(newName).isEmpty()) { "Attribute '$newName' is already in use." }

    getMultiProperty(oldName).forEach { it.setAttribute("name", newName) }
}

/**
 * Removes the property with attribute `name="[name]"`.
 *
 * If multiple properties have the same [name], they are all removed.
 */
fun Element.removeProperty(name: String) =
    getMultiProperty(name).forEach { children.remove(it) }



/**
 * Assuming this is the [Element] representation of a [Settings] instance, returns the [Element] of the contained
 * [com.fwdekker.randomness.template.TemplateList], or `null` if no such [Element] could be found.
 */
fun Element.getTemplateList(): Element? =
    getPropertyByPath("templateList", null)

/**
 * Assuming this is the [Element] representation of a [Settings] instance, returns the list of [Element]s of the
 * contained [com.fwdekker.randomness.template.Template]s.
 */
fun Element.getTemplates(): List<Element> =
    getPropertyByPath("templateList", null, "templates", null)?.children ?: emptyList()

/**
 * Assuming this is the [Element] representation of a [Settings] instance, returns the list of [Element]s of the
 * contained [com.fwdekker.randomness.Scheme]s.
 */
fun Element.getSchemes(): List<Element> =
    getTemplates().mapNotNull { it.getProperty("schemes") }.flatMap { it.children }

/**
 * Assuming this is the [Element] representation of a [Settings] instance, returns the list of [Element]s of the
 * contained [com.fwdekker.randomness.DecoratorScheme]s. Searches recursively, so also returns decorators of decorators,
 * and so on.
 */
fun Element.getDecorators(): List<Element> {
    val decorators = mutableListOf<Element>()

    var schemes = getSchemes()
    while (schemes.isNotEmpty()) {
        val containedDecorators = schemes.flatMap { scheme ->
            scheme.children
                .filter { child -> child.getAttribute("name")?.value.let { it != null && it.endsWith("Decorator") } }
                .map { it.children.single() }
        }
        schemes = containedDecorators
        decorators += containedDecorators
    }

    return decorators
}
