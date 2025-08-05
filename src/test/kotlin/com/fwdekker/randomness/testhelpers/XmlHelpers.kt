package com.fwdekker.randomness.testhelpers

import com.fwdekker.randomness.Settings
import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element
import org.jdom.output.XMLOutputter
import java.io.File
import java.net.URL


/**
 * Returns the serialized [Element] representation of `this` object.
 *
 * If `this` is a [String], you're probably doing something wrong!
 */
fun Any.serialize(): Element = XmlSerializer.serialize(this)

/**
 * Returns the XML representation of this serialized [Element] object.
 */
fun Element.toXmlString(): String = XMLOutputter().outputString(this)

/**
 * Returns the XML representation of `this` object.
 */
fun Any.serializeToXmlString(): String = this.serialize().toXmlString()

/**
 * Interprets `this` as an XML string and parses it to an [Element].
 */
fun String.parseXml(): Element = JDOMUtil.load(this)

/**
 * Interprets `this` as a URL pointing to an XML string and parses that to an [Element].
 */
fun URL.parseXml(): Element = JDOMUtil.load(this)

/**
 * Interprets `this` as a file containing an XML string and parses that to an [Element].
 */
fun File.parseXml(): Element = JDOMUtil.load(this)


/**
 * Parses a [Settings] object from the contents of this [Element].
 */
fun Settings.Companion.from(element: Element): Settings =
    XmlSerializer.deserialize(
        when (element.name) {
            "application" -> element.children.single()
            "component" -> element
            "Settings" -> element
            else -> error("Unknown element name '${element.name}'.")
        },
        Settings::class.java
    )

/**
 * Interprets `this` as an XML string and parses it to a [Settings] object.
 */
fun Settings.Companion.from(text: String): Settings = Settings.from(text.parseXml())

/**
 * Interprets `this` as a URL pointing to an XML string and parses that to a [Settings] object.
 */
fun Settings.Companion.from(url: URL): Settings = Settings.from(url.parseXml())

/**
 * Interprets `this` as a file containing an XML string and parses that to a [Settings] object.
 */
fun Settings.Companion.from(file: File): Settings = Settings.from(file.parseXml())
