package uz.shushop.app.util

import kotlin.math.abs

/**
 * Colour helpers for the drawn shoe pictures. Colours here are plain 0xRRGGBB integers
 * (no alpha) so they can be tested without Android or Compose.
 */
object ShoeColors {
    const val FALLBACK = 0xCDBFAA
    const val FALLBACK_ACCENT = 0xF26A3D
    private val ACCENTS = intArrayOf(0xF26A3D, 0x3D6FB4, 0xE0B44C, 0x4F7A5A, 0xD6456B)

    /** "#cdbfaa" -> 0xCDBFAA, or [fallback] when the text is not a 6-digit hex colour. */
    fun parse(hex: String?, fallback: Int = FALLBACK): Int {
        if (hex == null || !Regex("^#[0-9a-fA-F]{6}$").matches(hex)) return fallback
        return hex.substring(1).toInt(16)
    }

    fun toHex(rgb: Int): String = "#" + (rgb and 0xFFFFFF).toString(16).padStart(6, '0')

    private fun channels(rgb: Int) = intArrayOf((rgb shr 16) and 255, (rgb shr 8) and 255, rgb and 255)

    /** amount > 0 mixes with white, < 0 mixes with black. Same maths as the website (JavaScript Math.round). */
    fun shade(rgb: Int, amount: Double): Int {
        val target = if (amount >= 0) 255.0 else 0.0
        val a = abs(amount)
        val c = channels(rgb).map { v ->
            val mixed = Math.floor(v + (target - v) * a + 0.5).toInt()
            mixed.coerceIn(0, 255)
        }
        return (c[0] shl 16) or (c[1] shl 8) or c[2]
    }

    fun isDark(rgb: Int): Boolean {
        val (r, g, b) = channels(rgb)
        return 0.299 * r + 0.587 * g + 0.114 * b < 110
    }

    /** Each shoe gets one of five accent colours, picked by its id, so the shop looks varied but stable. */
    fun accentFor(productId: Long): Int = ACCENTS[(abs(productId) % ACCENTS.size).toInt()]

    /** The accent must not vanish against the shoe colour: when too close, it is made lighter or darker. */
    fun visibleAccent(accent: Int, base: Int): Int {
        val diff = abs(channels(accent).sum() - channels(base).sum())
        return if (diff < 90) shade(accent, if (isDark(base)) 0.5 else -0.4) else accent
    }

    /** Colour [rgb] with opacity [alpha] (0..1) as an ARGB integer. */
    fun withAlpha(rgb: Int, alpha: Double): Int = (Math.round(alpha * 255).toInt() shl 24) or (rgb and 0xFFFFFF)
}

/** One thing to draw, in the 600 x 600 picture space. The Compose canvas walks these in order. */
sealed interface ShoeLayer {
    /** A filled shape. [argb] includes the opacity. */
    class Fill(val path: String, val argb: Int) : ShoeLayer

    /** The soft light-to-dark overlay that gives a flat shape some volume. */
    class Shade(val path: String) : ShoeLayer

    class Line(val path: String, val argb: Int, val width: Float, val dashed: Boolean = false) : ShoeLayer

    /** Rectangles painted only where they overlap [clipPath] (the outsole band and the lugs). */
    class ClippedBoxes(val clipPath: String, val boxes: List<Box>) : ShoeLayer
    class Box(val x: Float, val y: Float, val w: Float, val h: Float, val radius: Float, val argb: Int)
}

/** Everything needed to draw one shoe: the picture offset and the layers of the upper and of the sole. */
class ShoeSpec(val style: String, val offsetY: Float, val upperLift: Float, val upper: List<ShoeLayer>, val sole: List<ShoeLayer>)

object ShoeGeometry {
    val STYLES = listOf("court", "runner", "high", "chunky", "slip", "boot")

    /** Builds the drawing for [style] in the colour [colorHex]. Mirrors shoeSvg() of the website, so both look the same. */
    fun spec(style: String, colorHex: String?, accentHex: String? = null): ShoeSpec {
        val s = if (style in STYLES) style else "court"
        val outline = SHOE_OUTLINES.getValue(s)
        val base = ShoeColors.parse(colorHex)
        val acc = ShoeColors.parse(accentHex, ShoeColors.FALLBACK_ACCENT)
        val dark = ShoeColors.shade(base, -0.28)
        val darker = ShoeColors.shade(base, -0.5)
        val light = ShoeColors.shade(base, 0.2)
        val accent = ShoeColors.visibleAccent(acc, base)
        val soleFill = if (ShoeColors.isDark(base) && s != "boot") 0xF1EEE6 else 0xF6F3EC
        val lace = if (ShoeColors.isDark(base)) 0xF4F1EA else 0xFFFFFF
        val opaque = { rgb: Int -> ShoeColors.withAlpha(rgb, 1.0) }
        val white = 0xFFFFFF

        val upper = ArrayList<ShoeLayer>()
        upper += ShoeLayer.Fill(outline.upper, opaque(base))
        upper += ShoeLayer.Shade(outline.upper)

        // inner collar / opening
        when (s) {
            "slip" -> {
                upper += ShoeLayer.Fill("M340 290 C366 282 400 286 430 292 C466 296 500 292 520 286 C500 304 466 310 430 308 C400 306 366 300 340 290 Z", opaque(darker))
                upper += ShoeLayer.Fill("M236 321 C252 319 272 319 288 314 L288 336 C270 342 252 342 236 340 Z", opaque(accent))
                upper += ShoeLayer.Line("M246 328 h30", opaque(ShoeColors.shade(accent, 0.5)), 3f)
            }
            "boot" -> {
                upper += ShoeLayer.Fill("M330 118 L500 108 C510 110 518 122 522 142 C440 130 380 134 330 150 Z", opaque(darker))
                upper += ShoeLayer.Fill("M500 108 C524 112 534 170 532 250 C512 220 506 160 500 108 Z", opaque(dark))
            }
            else -> {
                val inner = if (s == "high") "M332 160 C338 178 354 198 382 206 C412 212 442 198 474 180 C446 190 414 198 384 194 C356 188 340 176 332 160 Z"
                else "M346 222 C352 244 376 262 404 262 C436 262 470 248 502 240 C470 246 436 254 404 252 C376 252 352 238 346 222 Z"
                upper += ShoeLayer.Fill(inner, opaque(darker))
            }
        }

        // toe cap and heel counter
        upper += if (s != "slip") {
            ShoeLayer.Fill("M84 380 C78 352 96 326 132 316 C150 311 166 322 168 342 L170 380 Z", ShoeColors.withAlpha(light, 0.85))
        } else {
            ShoeLayer.Fill("M84 380 C78 356 98 334 134 326 C150 323 164 332 166 348 L168 380 Z", ShoeColors.withAlpha(light, 0.8))
        }
        val heelTop = when (s) { "court" -> 240; "runner" -> 242; "high" -> 172; "chunky" -> 238; "slip" -> 286; else -> 150 }
        upper += ShoeLayer.Fill(
            "M531 ${heelTop + 76} C534 ${heelTop + 30} 522 $heelTop 502 $heelTop L472 ${heelTop + 12} C480 ${heelTop + 60} 482 ${heelTop + 100} 478 380 L529 380 Z",
            ShoeColors.withAlpha(dark, 0.9))

        // side panel in the accent colour
        when (s) {
            "court", "chunky", "high" -> upper += ShoeLayer.Fill("M262 288 L308 284 C322 314 326 346 322 380 L278 380 C282 346 276 316 262 288 Z", opaque(accent))
            "runner" -> upper += ShoeLayer.Fill("M250 360 C310 356 392 330 468 282 C436 338 362 374 262 378 Z", opaque(accent))
            "boot" -> upper += ShoeLayer.Fill("M236 330 C300 322 372 318 524 314 L526 350 C420 352 320 360 238 366 Z", opaque(dark))
        }

        // lace panel and laces
        if (outline.laces.isNotEmpty()) {
            val panel = if (s == "high" || s == "boot") "M262 282 L" + (if (s == "boot") "330 118" else "300 168") else "M254 290 L160 312"
            val panelWidth = if (s == "boot") 26f else if (s == "high") 22f else 16f
            upper += ShoeLayer.Line(panel, ShoeColors.withAlpha(dark, 0.55), panelWidth)
            val laceColor = opaque(lace)
            val laceBack = ShoeColors.withAlpha(lace, 0.85)
            for ((x, y) in outline.laces) {
                upper += ShoeLayer.Line("M${x - 9} ${y - 9} L${x + 9} ${y + 9}", laceColor, 5f)
                upper += ShoeLayer.Line("M${x + 9} ${y - 9} L${x - 9} ${y + 9}", laceBack, 5f)
            }
        }

        // stitching along the bottom edge of the upper
        upper += ShoeLayer.Line("M96 364 C150 360 260 358 330 356 C400 354 470 358 520 362", ShoeColors.withAlpha(white, 0.5), 2f, dashed = true)

        // sole on top of the upper
        val sole = ArrayList<ShoeLayer>()
        sole += ShoeLayer.Fill(outline.sole, opaque(soleFill))
        sole += ShoeLayer.Shade(outline.sole)
        val boxes = ArrayList<ShoeLayer.Box>()
        boxes += ShoeLayer.Box(60f, outline.outsoleY.toFloat(), 500f, 24f, 0f, opaque(ShoeColors.shade(soleFill, -0.2)))
        if (s == "chunky" || s == "boot") {
            var x = 90
            while (x < 520) {
                boxes += ShoeLayer.Box(x.toFloat(), (outline.outsoleY + 4).toFloat(), 14f, 14f, 3f, opaque(ShoeColors.shade(soleFill, -0.4)))
                x += 26
            }
        }
        sole += ShoeLayer.ClippedBoxes(outline.sole, boxes)
        val lineShift = if (s == "chunky") -14 else 0
        val y1 = (if (s == "slip") 380 else 372) + lineShift
        val y2 = (if (s == "runner") 344 else if (s == "slip") 380 else 372) + lineShift
        sole += ShoeLayer.Line("M86 $y1 L530 $y2", ShoeColors.withAlpha(white, 0.7), 2f)

        val offsetY = when (s) { "boot" -> 32f; "high" -> 14f; else -> -22f }
        val lift = if (s == "chunky") -10f else 0f
        return ShoeSpec(s, offsetY, lift, upper, sole)
    }
}
