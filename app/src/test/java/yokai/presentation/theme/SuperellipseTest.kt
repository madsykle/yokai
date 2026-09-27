package yokai.presentation.theme

import io.kotest.matchers.floats.shouldBeBetween
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow

/**
 * The squircle is the kind of geometry that is wrong in ways nobody can see: an outline that is
 * subtly egg-shaped, corners that do not meet, or a rim stroke that jumps across the surface.
 * The maths is pure precisely so those cases can be pinned here instead of on a device.
 */
class SuperellipseTest {

    private val tolerance = 0.01f

    /** Float comparisons throughout: the maths is Float, so the assertions are too. */
    private fun assertAbout(actual: Float, expected: Float, tolerance: Float = 0.01f) {
        actual.shouldBeBetween(expected - tolerance, expected + tolerance, tolerance)
    }

    @Test
    fun `every point lies on the superellipse`() {
        val a = 100f
        val b = 40f
        val n = Superellipse.CORNER_EXPONENT
        val points = Superellipse.points(a, b)
        for (i in points.indices step 2) {
            val x = abs(points[i].toDouble() / a)
            val y = abs(points[i + 1].toDouble() / b)
            // |x/a|^n + |y/b|^n = 1 is the definition the class documents.
            assertAbout((x.pow(n) + y.pow(n)).toFloat(), 1f, 0.02f)
        }
    }

    @Test
    fun `the outline stays inside its half extents`() {
        val points = Superellipse.points(halfWidth = 50f, halfHeight = 20f)
        for (i in points.indices step 2) {
            (abs(points[i]) <= 50f + tolerance) shouldBe true
            (abs(points[i + 1]) <= 20f + tolerance) shouldBe true
        }
    }

    @Test
    fun `the outline is symmetric about both axes`() {
        val points = Superellipse.points(halfWidth = 50f, halfHeight = 20f, segments = 64)
        // Sample 0 is the right extreme, and the top is a quarter turn further round.
        assertAbout(points[0], 50f)
        assertAbout(points[1], 0f)
        // The flat array interleaves x and y, so sample `i` lives at `2 * i`.
        val topIndex = (64 * 3) / 4
        assertAbout(points[topIndex * 2], 0f)
        assertAbout(points[topIndex * 2 + 1], -20f)
    }

    @Test
    fun `a low exponent tends towards an ellipse`() {
        // With n = 2 the superellipse IS the circle, which ties the parametric form in `points`
        // back to the equation the class documents.
        val points = Superellipse.points(halfWidth = 40f, halfHeight = 40f, exponent = 2.0)
        for (i in points.indices step 2) {
            assertAbout(hypot(points[i], points[i + 1]), 40f)
        }
    }

    @Test
    fun `the rim traces the top of the shape and stops at the cut`() {
        val width = 200f
        val height = 100f
        val rim = Superellipse.rimPoints(width, height, inset = 0f, fraction = 0.6f)
        (rim.size > 4) shouldBe true
        // Every sample sits in the top fraction of the surface, never below the cut...
        val cut = height * 0.6f
        for (i in rim.indices step 2) {
            (rim[i + 1] <= cut + tolerance) shouldBe true
        }
        // ...and the run is contiguous, so walking it never jumps across the surface. Filtering
        // the full outline by `y` instead of walking outwards from the top is exactly the bug
        // this asserts against.
        for (i in 2 until rim.size step 2) {
            hypot(rim[i] - rim[i - 2], rim[i + 1] - rim[i - 1])
                .shouldBeBetween(0f, width * 0.35f, tolerance)
        }
    }

    @Test
    fun `the rim starts left of the middle and ends right of it`() {
        val rim = Superellipse.rimPoints(width = 200f, height = 100f, inset = 0f, fraction = 0.6f)
        (rim[0] < 100f) shouldBe true
        (rim[rim.size - 2] > 100f) shouldBe true
    }

    @Test
    fun `a circle rim is an arc rather than a squircle sample run`() {
        val rim = Superellipse.rimPoints(
            width = 100f,
            height = 100f,
            inset = 0f,
            fraction = 0.6f,
            circle = true,
        )
        (rim.size > 4) shouldBe true
        // A 60% arc of a 100px circle is 0.6 * pi * 100 ≈ 188px of travel.
        var length = 0f
        for (i in 2 until rim.size step 2) {
            length += hypot(rim[i] - rim[i - 2], rim[i + 1] - rim[i - 1])
        }
        assertAbout(length, 188.5f, 3f)
    }

    @Test
    fun `a degenerate surface produces no rim instead of throwing`() {
        Superellipse.rimPoints(width = 0f, height = 100f, inset = 0f).size shouldBe 0
        Superellipse.rimPoints(width = 100f, height = 0f, inset = 0f).size shouldBe 0
        // An inset larger than the half extents makes the sample run meaningless.
        Superellipse.rimPoints(width = 100f, height = 100f, inset = 60f).size shouldBe 0
    }
}
