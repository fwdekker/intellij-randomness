package com.fwdekker.randomness

import com.fwdekker.randomness.Icons.ARRAY
import com.fwdekker.randomness.Icons.REFERENCE
import com.fwdekker.randomness.Icons.REPEAT
import com.fwdekker.randomness.Icons.SETTINGS
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.ColorUtil
import com.intellij.ui.LayeredIcon
import com.intellij.ui.RowIcon
import com.intellij.ui.SizedIcon
import com.intellij.ui.icons.FilteredIcon
import com.intellij.ui.icons.RgbImageFilterSupplier
import com.intellij.ui.icons.RowIcon.Alignment
import com.intellij.ui.icons.TextIcon
import com.intellij.util.IconUtil
import java.awt.Color
import java.awt.Component
import java.awt.image.BufferedImage
import java.awt.image.RGBImageFilter
import javax.swing.Icon
import javax.swing.SwingConstants
import kotlin.math.atan2


/**
 * Basic Randomness icons.
 */
object Icons {
    /**
     * The main icon of Randomness.
     */
    @JvmField
    val RANDOMNESS = IconLoader.getIcon("/icons/randomness.svg", javaClass)

    /**
     * The template icon for template icons.
     */
    @JvmField
    val TEMPLATE = IconLoader.getIcon("/icons/template.svg", javaClass)

    /**
     * The template icon for scheme icons.
     */
    @JvmField
    val SCHEME = IconLoader.getIcon("/icons/scheme.svg", javaClass)

    /**
     * An icon for settings.
     */
    @JvmField
    val SETTINGS = IconLoader.getIcon("/icons/settings.svg", javaClass)

    /**
     * A filled-in version of [SETTINGS].
     */
    @JvmField
    val SETTINGS_FILLED = IconLoader.getIcon("/icons/settings-filled.svg", javaClass)

    /**
     * An icon for arrays.
     */
    @JvmField
    val ARRAY = IconLoader.getIcon("/icons/array.svg", javaClass)

    /**
     * A filled-in version of [ARRAY].
     */
    @JvmField
    val ARRAY_FILLED = IconLoader.getIcon("/icons/array-filled.svg", javaClass)

    /**
     * An icon for references.
     */
    @JvmField
    val REFERENCE = IconLoader.getIcon("/icons/reference.svg", javaClass)

    /**
     * A filled-in version of [REFERENCE].
     */
    @JvmField
    val REFERENCE_FILLED = IconLoader.getIcon("/icons/reference-filled.svg", javaClass)

    /**
     * An icon for repeated insertions.
     */
    @JvmField
    val REPEAT = IconLoader.getIcon("/icons/repeat.svg", javaClass)

    /**
     * A filled-in version of [REPEAT].
     */
    @JvmField
    val REPEAT_FILLED = IconLoader.getIcon("/icons/repeat-filled.svg", javaClass)
}


/**
 * Describes an [Icon], and can instantiate an icon, but is not itself an icon.
 *
 * You may think it's better to just extend the [Icon] class instead of having this class in between. However, as noted
 * in the comments to [IJPL-163887](https://youtrack.jetbrains.com/issue/IJPL-163887/), this leads to issues with
 * IntelliJ's built-in icon mechanisms that are responsible for loading, caching, and scaling.
 *
 * You may also think it's better to just create functions that return [Icon]s directly, i.e. replace this class with
 * a function that implements only the [get] function. However, this would result in a loss of metadata. For example,
 * with [TypeIcon], the returned [LayeredIcon] does not tell you what the original [TypeIcon.text] and [TypeIcon.colors]
 * are, making a function such as [TypeIcon.combine] a real mess to implement.
 *
 * Before [get] is called, the graphics context for [Icon]s has not been fully initialised yet. Therefore, implementing
 * classes must not validate or otherwise access [Icon]-related properties before [get] is called. Violating this
 * principle, for example by checking whether an [Icon] passed in the constructor is square, will result in exceptions;
 * see for example [#R13](https://github.com/FWDekkerBot/intellij-randomness-issues/issues/13) and
 * [#R44](https://github.com/FWDekkerBot/intellij-randomness-issues/issues/44). Such validation must be deferred to when
 * [get] is called for the first time.
 */
interface IconDescriptor {
    /**
     * Creates or returns the described [Icon].
     */
    fun get(): Icon
}

/**
 * Describes a colored icon with some text in it.
 *
 * @property base The underlying icon which should be given color; must be square.
 * @property text The text to display inside the [base].
 * @property colors The colors to give to the [base].
 */
data class TypeIcon(val base: Icon, val text: String, val colors: List<Color>) : IconDescriptor {
    init {
        require(colors.isNotEmpty()) { "At least one color must be defined." }
    }


    private fun truncateWidth(icon: Icon, width: Int): Icon =
        SizedIcon(icon, width, icon.iconHeight)

    override fun get(): Icon {
        // Must not be checked in constructor, see documentation of [IconDescriptor]
        require(base.iconWidth == base.iconHeight) { "Base must be square." }

        val component = object : Component() {}
        val baseSize = base.iconWidth
        val filter = RadialColorReplacementFilter(colors, Pair(baseSize / 2, baseSize / 2))

        return LayeredIcon(2).apply {
            setIcon(filterIcon(base, filter), 0)
            setIcon(truncateWidth(TextIcon(text, component, FONT_SIZE * baseSize), baseSize), 1, SwingConstants.CENTER)
        }
    }


    /**
     * Holds constants.
     */
    companion object {
        /**
         * The scale of the text inside the icon relative to the icon's size.
         */
        private const val FONT_SIZE = 12f / 32f


        /**
         * Returns a single icon that describes all given [TypeIcon]s, or `null` if [icons] is empty.
         */
        fun combine(icons: Collection<TypeIcon>): TypeIcon? =
            if (icons.isEmpty()) null
            else TypeIcon(
                Icons.TEMPLATE,
                icons.map { it.text }.toSet().singleOrNull() ?: "",
                icons.flatMap { it.colors }
            )
    }
}

/**
 * An overlay icon, which can be displayed on top of other icons.
 *
 * The icon consists of a [base] and a [fill]. The [base] is the actually important image, and the [fill] is useful for
 * creating some space around the [base] image. Specifically, the [fill] is the same as the [base], except with
 * everything within the [base] shape's boundaries also being filled in. For example, if [base] is a hollow circle, then
 * [fill] would be a filled-in circle of the same size.
 *
 * The [get] function simply returns the [base] and does not use the [fill] at all. Instead, the [fill] is used by
 * [OverlayedIcon]. When this [OverlayIcon] is placed on top of some underlying image, the [OverlayedIcon] will use the
 * [fill] to perforate the underlying image, and then place the [base] on top in the newly created empty space. When
 * doing this, the [OverlayedIcon] will slightly scale up the [fill] relative to the [base] so that there is also a
 * slight margin around the [base].
 *
 * @property base The underlying base icon; must be square.
 * @property fill A filled-in version of the [base]; must be the exact same size as the [base].
 */
data class OverlayIcon(val base: Icon, val fill: Icon = base) : IconDescriptor {
    override fun get(): Icon {
        // Must not be checked in constructor, see documentation of [IconDescriptor]
        require(base.iconWidth == base.iconHeight) { "Base must be square." }
        require(fill.iconWidth == base.iconWidth && fill.iconHeight == base.iconHeight) {
            "Fill must have same size as base."
        }

        return base
    }


    /**
     * Holds constants.
     */
    companion object {
        /**
         * Overlay icon for arrays.
         */
        val ARRAY get() = OverlayIcon(Icons.ARRAY, Icons.ARRAY_FILLED)

        /**
         * Overlay icon for template references.
         */
        val REFERENCE get() = OverlayIcon(Icons.REFERENCE, Icons.REFERENCE_FILLED)

        /**
         * Overlay icon for repeated insertion.
         */
        val REPEAT get() = OverlayIcon(Icons.REPEAT, Icons.REPEAT_FILLED)

        /**
         * Overlay icon for settings.
         */
        val SETTINGS get() = OverlayIcon(Icons.SETTINGS, Icons.SETTINGS_FILLED)
    }
}

/**
 * An overlayed icon, which is a [base] icon with various [overlays] placed on top.
 *
 * See the documentation of [OverlayIcon] for more information on its components.
 *
 * @property base The underlying base icon; must be square.
 * @property overlays The various [OverlayIcon]s that are overlayed on top of the [base].
 */
data class OverlayedIcon(val base: TypeIcon, val overlays: List<OverlayIcon> = emptyList()) : IconDescriptor {
    init {
        require(overlays.size <= OVERLAYS_PER_ROW) { "A maximum of $OVERLAYS_PER_ROW overlays is currently the limit." }
    }


    /**
     * Returns a new [OverlayedIcon] that has one extra [overlay].
     */
    fun plusOverlay(overlay: OverlayIcon): OverlayedIcon = copy(overlays = overlays + overlay)


    /**
     * Creates a [RowIcon] consisting of appropriately-scaled horizontally-laid-out [icons].
     *
     * Each (square) icon is first scaled to an [innerSize] square and then centered within an [outerSize] square. There
     * is no additional space between the icons.
     */
    private fun createOverlayRow(icons: List<Icon>, outerSize: Int, innerSize: Number = outerSize): RowIcon {
        val rowIcon = RowIcon(icons.size, alignment = Alignment.BOTTOM)
        icons.forEachIndexed { idx, icon ->
            val innerIcon = IconUtil.scale(icon, null, innerSize.toFloat() / icon.iconWidth)
            val outerIcon = SizedIcon(innerIcon, outerSize, outerSize)

            rowIcon.setIcon(outerIcon, idx)
        }
        return rowIcon
    }

    override fun get(): Icon {
        val baseIcon = base.get()
        if (overlays.isEmpty()) return baseIcon

        val overlayOuterSize = baseIcon.iconWidth / OVERLAYS_PER_ROW
        val overlayInnerSize = (1f - 2f * MARGIN) * overlayOuterSize

        val overlayBaseRowIcon = createOverlayRow(overlays.map { it.base }, overlayOuterSize, overlayInnerSize)
        val overlayShadowRowIcon = createOverlayRow(overlays.map { it.fill }, overlayOuterSize)

        return LayeredIcon(2).apply {
            setIcon(filterIcon(baseIcon, SubtractionFilter(overlayShadowRowIcon)), 0)
            setIcon(overlayBaseRowIcon, 1, SwingConstants.NORTH_WEST)
        }
    }


    /**
     * Holds constants.
     */
    companion object {
        /**
         * The number of overlays to fit in each row.
         *
         * The total size of each individual overlay is the [base] image's width divided by this number.
         */
        private const val OVERLAYS_PER_ROW = 2

        /**
         * The margin around each overlay base image.
         *
         * This number is a fraction relative to the total size of each overlay.
         */
        private const val MARGIN = 4f / 32f
    }
}


/**
 * Replaces all colors with one of [colors] depending on the angle relative to [center].
 *
 * @param colors the colors that should be used, in clockwise order starting north
 * @param center the center relative to which colors should be calculated; not required if only one color is given
 */
class RadialColorReplacementFilter(
    private val colors: List<Color>,
    private val center: Pair<Int, Int>? = null,
) : RGBImageFilter() {
    init {
        require(colors.isNotEmpty()) { "At least one color must be defined." }
        require(colors.size == 1 || center != null) { "Center must be defined if more than one color is given." }
    }


    /**
     * Returns the color to be displayed at ([x], [y]), considering the coordinates relative to the [center] and the
     * relative alpha of the encountered color.
     *
     * @param x the X coordinate of the pixel
     * @param y the Y coordinate of the pixel
     * @param rgb `0` if and only if the pixel's color should be replaced
     * @return `0` if [rgb] is `0`, or one of [colors] with its alpha shifted by [rgb]'s alpha otherwise
     */
    @Suppress("UseJBColor") // Filtering works the same in both themes
    override fun filterRGB(x: Int, y: Int, rgb: Int) =
        if (rgb == 0) 0
        else if (center == null || colors.size == 1) shiftAlpha(colors[0], Color(rgb, true)).rgb
        else shiftAlpha(positionToColor(Pair(x - center.first, y - center.second)), Color(rgb, true)).rgb


    /**
     * Returns [base] after multiplying its alpha by the alpha of [shiftBy].
     */
    private fun shiftAlpha(base: Color, shiftBy: Color): Color =
        ColorUtil.withAlpha(base, asFraction(base.alpha) * asFraction(shiftBy.alpha))

    /**
     * Represents a [number] in the range `[0, 256)` as a fraction of that range.
     */
    private fun asFraction(number: Int): Double = number / COMPONENT_MAX

    /**
     * Returns the appropriate color from [colors] for an [offset] relative to the [center].
     */
    private fun positionToColor(offset: Pair<Int, Int>): Color {
        val angle = 2 * Math.PI + STARTING_ANGLE + atan2(offset.second.toDouble(), offset.first.toDouble())
        val index = angle / (2 * Math.PI / colors.size)
        return colors[Math.floorMod(index.toInt(), colors.size)]
    }


    /**
     * Holds constants.
     */
    companion object {
        /**
         * Maximum value for an RGB component.
         */
        const val COMPONENT_MAX: Double = 255.0

        /**
         * The angle in radians at which the first color should start being displayed.
         */
        const val STARTING_ANGLE: Double = .25 * (2 * Math.PI)
    }
}

/**
 * Interprets the given [icon] as a mask in which non-empty pixels indicate which pixels should be removed.
 */
class SubtractionFilter(icon: Icon) : RGBImageFilter() {
    /**
     * A buffered copy of the [icon].
     */
    private val mask: BufferedImage?


    init {
        if (icon.iconWidth <= 0 || icon.iconHeight <= 0) {
            mask = null
        } else {
            @Suppress("UndesirableClassUsage") // A simple 2D image is preferred over fancy hi-DPI alternatives
            mask = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_ARGB)
            with(mask.createGraphics()) {
                icon.paintIcon(null, this, 0, 0)
                dispose()
            }
        }
    }


    /**
     * Filters the given [rgb] color through the [icon] at the point ([x], [y]).
     */
    override fun filterRGB(x: Int, y: Int, rgb: Int): Int {
        if (mask == null || x !in 0..<mask.width || y !in 0..<mask.height) return rgb

        return if (mask.getRGB(x, y) == 0) rgb
        else 0
    }
}

/**
 * Applies the given [filter] to the given [base].
 *
 * This method is not nice and should really not exist. However, IntelliJ's API is not ready for that yet. See also
 * [#552](https://github.com/FWDekker/intellij-randomness/issues/552) and
 * [IJPL-5285](https://youtrack.jetbrains.com/issue/IJPL-5285/).
 *
 * @see IconUtil.filterIcon
 * @see IconLoader.filterIcon
 * @see FilteredIcon
 */
@Suppress("UnstableApiUsage") // No alternative, see JavaDoc above
private fun filterIcon(base: Icon, filter: RGBImageFilter): FilteredIcon =
    FilteredIcon(
        base,
        object : RgbImageFilterSupplier {
            override fun getFilter(): RGBImageFilter = filter
        }
    )
