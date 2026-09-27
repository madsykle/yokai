package yokai.presentation.theme

import android.graphics.Path
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * Squircle geometry for the glass surfaces (DESIGN.md §4.3).
 *
 * A rounded rectangle built from circular arcs has a curvature *discontinuity* where the arc
 * meets the straight edge, which is what makes a "rounded rect" read as a rectangle with its
 * corners knocked off. Apple's continuous curves - and the iOS 27 reference this restyle is
 * chasing - instead use a superellipse, `|x/a|^n + |y/b|^n = 1`, whose curvature varies
 * smoothly along the whole edge. At `n = 2` it is an ellipse; the higher exponent used here
 * squares off the middle of each edge while keeping the corners soft.
 *
 * The point maths is a pure function so it can be unit tested without a device
 * (`SuperellipseTest`) - a wrong exponent or a flipped axis produces a shape that is subtly
 * wrong and very hard to eyeball.
 */
object Superellipse {

    /** Curvature exponent. 4 is a conventional squircle; 2 would be a plain ellipse. */
    const val CORNER_EXPONENT = 4.0

    /** Segments used for a full outline. 64 is smooth at the radii used here (< 30dp). */
    const val DEFAULT_SEGMENTS = 64

    /**
     * Outline points of a superellipse centred on the origin with the given half-extents,
     * written as `x0, y0, x1, y1, ...` into a flat array of `2 * segments` floats.
     *
     * The parametric form is used rather than solving `y` for each `x`, because it distributes
     * points evenly along the curve and needs no special case at the flat parts of the edge:
     * `x = a·sign(cos t)·|cos t|^(2/n)`, `y = b·sign(sin t)·|sin t|^(2/n)`.
     */
    fun points(
        halfWidth: Float,
        halfHeight: Float,
        exponent: Double = CORNER_EXPONENT,
        segments: Int = DEFAULT_SEGMENTS,
    ): FloatArray {
        require(segments >= 4) { "a superellipse needs at least 4 segments" }
        require(exponent > 0.0) { "the corner exponent must be positive" }
        val out = FloatArray(segments * 2)
        val power = 2.0 / exponent
        for (i in 0 until segments) {
            val t = 2.0 * PI * i / segments
            val c = cos(t)
            val s = sin(t)
            out[i * 2] = (halfWidth * sign(c) * abs(c).pow(power)).toFloat()
            out[i * 2 + 1] = (halfHeight * sign(s) * abs(s).pow(power)).toFloat()
        }
        return out
    }

    /**
     * The lit rim: a single *contiguous* run of outline samples covering the top [fraction] of
     * the shape's height, returned as `x0, y0, x1, y1, ...` in `0..width` × `0..height` space.
     *
     * Contiguity is the whole point. Filtering the full outline by `y` would keep the two side
     * points that happen to sit above the cut while dropping the bottom of the loop in between,
     * and the stroke would jump straight across the surface. So the run is walked outward from
     * the topmost sample, and stopped as soon as the next sample drops below the cut.
     *
     * An oval is handled as an arc for the same reason - it is the same contiguous run, and
     * sampling it as a superellipse would only flatten an already-correct circle.
     */
    fun rimPoints(
        width: Float,
        height: Float,
        inset: Float,
        fraction: Float = 0.60f,
        circle: Boolean = false,
        segments: Int = DEFAULT_SEGMENTS,
    ): FloatArray {
        if (width <= 0f || height <= 0f) return FloatArray(0)
        if (circle) {
            // An arc centred on the top of the oval, so the lit part sits symmetrically on both
            // sides exactly as it does on a rectangle.
            val sweepDegrees = 360f * fraction
            val radiusX = width / 2f - inset
            val radiusY = height / 2f - inset
            if (radiusX <= 0f || radiusY <= 0f) return FloatArray(0)
            val start = Math.toRadians((-90f - sweepDegrees / 2f).toDouble())
            val steps = 24
            val out = FloatArray((steps + 1) * 2)
            for (i in 0..steps) {
                val angle = start + Math.toRadians(sweepDegrees.toDouble()) * i / steps
                out[i * 2] = width / 2f + radiusX * cos(angle).toFloat()
                out[i * 2 + 1] = height / 2f + radiusY * sin(angle).toFloat()
            }
            return out
        }

        val halfWidth = width / 2f - inset
        val halfHeight = height / 2f - inset
        if (halfWidth <= 0f || halfHeight <= 0f) return FloatArray(0)

        val samples = points(halfWidth, halfHeight, segments = segments)
        val topIndex = (segments * 3) / 4              // t = 3π/2 -> the topmost sample
        val cut = height * fraction - height / 2f      // the cut, in centred coordinates

        var reach = 0
        while (reach + 1 < segments / 4 &&
            samples[((topIndex - reach - 1) + segments) % segments * 2 + 1] <= cut
        ) {
            reach++
        }

        val out = FloatArray((reach * 2 + 1) * 2)
        var i = 0
        // Walked left-to-right across the top, so the returned run reads the same way the
        // shape does: -reach is the left side, 0 is the top, +reach is the right side.
        for (k in -reach..reach) {
            val index = ((topIndex + k) % segments + segments) % segments
            out[i++] = width / 2f + samples[index * 2]
            out[i++] = height / 2f + samples[index * 2 + 1]
        }
        return out
    }

    /**
     * [points], turned into a [Path] fitted to `width` × `height`.
     *
     * A capsule (`circle`) is left as an oval: the superellipse is the squircle variant of the
     * same token, and forcing the exponent onto a 1:1 round control would only flatten a shape
     * that is already correct.
     */
    fun path(
        width: Float,
        height: Float,
        exponent: Double = CORNER_EXPONENT,
        circle: Boolean = false,
        segments: Int = DEFAULT_SEGMENTS,
        outPath: Path = Path(),
    ): Path {
        outPath.rewind()
        if (width <= 0f || height <= 0f) return outPath
        if (circle) {
            outPath.addOval(0f, 0f, width, height, Path.Direction.CW)
            return outPath
        }
        val halfWidth = width / 2f
        val halfHeight = height / 2f
        val pts = points(halfWidth, halfHeight, exponent, segments)
        outPath.moveTo(halfWidth + pts[0], halfHeight + pts[1])
        for (i in 1 until segments) {
            outPath.lineTo(halfWidth + pts[i * 2], halfHeight + pts[i * 2 + 1])
        }
        outPath.close()
        return outPath
    }
}
