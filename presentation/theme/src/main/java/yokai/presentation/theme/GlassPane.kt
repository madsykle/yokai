package yokai.presentation.theme

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout

/**
 * DESIGN.md §18: the one custom-drawn glass surface.
 *
 * A [FrameLayout] that draws the whole recipe itself - vertical white gradient, tiled noise,
 * top-edge lit rim, soft glow along its own edge, a drop shadow below, and a one-shot specular
 * sweep - instead of tinting a background drawable. Put chrome inside it (nav items, a toolbar,
 * a button) and the surface under that chrome is glass, with no second view needed and nothing
 * to keep in sync.
 *
 * Applied to the functional/nav layers only: the floating top bar, the bottom nav pill and its
 * circular companion, the w720dp rail, sheets, dialogs, the search bar and the reader's
 * page-slider. Deliberately **not** applied to list items, manga covers or the reader page,
 * which are content (§1.1) - glass belongs on the layers that float above content.
 *
 * ## Why some of this is rendered into a software bitmap
 *
 * The platform's own hardware-acceleration support table lists `Paint.setMaskFilter()` (which
 * is what [BlurMaskFilter] is) as **unsupported at every API level**, and
 * `setShadowLayer()` for non-text as supported only from **API 28**. minSdk here is 26, so
 * neither operation can be relied on to draw on a GPU-accelerated canvas - they are silently
 * dropped, which is exactly how you get glass that looks flat on one device and correct on
 * another.
 *
 * The documented workaround is to render the affected operation into an off-screen software
 * bitmap and draw the result. That is what [renderEdgeBitmap] does: the glow and the shadow are
 * rasterised into one small cached [Bitmap] (rebuilt only when the size or the shape changes)
 * and composited on the normal hardware path. Everything that *is* hardware accelerated - the
 * gradient, the noise, the rim, the animated sweep, the clip - stays on the GPU.
 *
 * ## Why the outline is a rounded rectangle and not the superellipse
 *
 * The drawn shape is a [Superellipse] squircle. The *outline* used for `clipToOutline` - which
 * is what clips children such as the toolbar - stays a plain rounded rectangle, because
 * `Outline.setConvexPath` is silently ignored when the platform disagrees about convexity, and
 * a child that stops being clipped at runtime is a worse failure than a ~1px difference between
 * the clip and the paint.
 */
class GlassPane @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    /**
     * Corner radius in dp. Setting it after inflation is supported (the chrome does exactly
     * that), and re-derives the shape, the shaders and the cached edge bitmap.
     */
    var cornerRadiusDp: Float = DEFAULT_CORNER_RADIUS_DP
        set(value) {
            if (field != value) {
                field = value
                radiusPx = value * resources.displayMetrics.density
                invalidateShape()
            }
        }

    /** Capsule/oval instead of a squircle - the round companion beside the nav pill. */
    var circle: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidateShape()
            }
        }

    private var radiusPx = DEFAULT_CORNER_RADIUS_DP * resources.displayMetrics.density

    private val shapePath = Path()

    private val baseFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val noisePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val sweepPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var gradientShader: LinearGradient? = null
    private var noiseTile: Bitmap? = null
    private var edgeBitmap: Bitmap? = null
    private var sweepAnimator: ValueAnimator? = null
    private var sweepProgress = 0f

    /** Rebuilt on every shape/size change: the drawn glass is expensive to assemble. */
    private var materialDirty = true

    init {
        setWillNotDraw(false)
        outlineProvider = SquircleOutlineProvider()
        clipToOutline = true
        refreshMaterial()
    }

    /**
     * Re-reads the §2.2 transparency slider and the current tier. This is the live-preview path:
     * the preference listener calls it and the surface repaints without any recreation.
     */
    fun refreshMaterial() {
        sweepPaint.shader = null
        materialDirty = true
        invalidate()
    }

    /**
     * Fires the specular sweep once: a diagonal highlight band crosses the surface and settles.
     * Called on tab switch, sheet open and screen transition - a beat marking the change, not a
     * continuous effect (DESIGN.md §18).
     *
     * A sweep already in flight is restarted rather than queued: the interaction being
     * acknowledged is always the most recent one.
     */
    fun playSpecularSweep() {
        if (width <= 0 || height <= 0) return
        sweepAnimator?.cancel()
        sweepAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = GlassRecipe.SWEEP_DURATION_MS
            addUpdateListener { animation ->
                sweepProgress = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        invalidateShape()
    }

    override fun onDetachedFromWindow() {
        sweepAnimator?.cancel()
        sweepAnimator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        if (materialDirty) buildMaterial(w, h)
        val save = canvas.save()
        canvas.clipPath(shapePath)

        // 1. Base fill: the tier's legibility floor. On the target device there is no blur
        //    behind this, so the fill - not a blur - is what keeps labels readable.
        canvas.drawPath(shapePath, baseFillPaint)

        // 2. The vertical ramp: lighter at the top, darker at the bottom.
        gradientShader?.let {
            gradientPaint.shader = it
            canvas.drawPath(shapePath, gradientPaint)
        }

        // 3. Tiled noise, blended so it breaks the ramp's banding instead of greying it out.
        if (noisePaint.shader != null) canvas.drawPath(shapePath, noisePaint)
        canvas.restore()

        // 4. Soft glow on the shape's own edge + the shadow below it, rasterised off-screen
        //    because setMaskFilter/setShadowLayer do not exist on a hardware canvas.
        edgeBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }

        // 5. The lit rim: near-white, on the top part of the edge only.
        canvas.drawPath(rimPath, rimPaint)

        // 6. The one-shot specular sweep.
        if (sweepProgress > 0f && sweepProgress < 1f) drawSweep(canvas, w, h)
    }

    /** Rim path: the shape's outline, but only the top [GlassRecipe.RIM_SWEEP_FRACTION]. */
    private val rimPath = Path()

    private fun drawSweep(canvas: Canvas, w: Float, h: Float) {
        val alpha = GlassRecipe.sweepBandAlpha(sweepProgress)
        if (alpha <= 0f) return
        val band = w * GlassRecipe.SWEEP_BAND_FRACTION
        // Travel from fully off the top-left to fully off the bottom-right, so the band enters
        // and leaves instead of popping in mid-surface.
        val travel = w + h + band * 2f
        val head = -band + travel * sweepProgress
        sweepPaint.shader = LinearGradient(
            head - band, 0f,
            head, h.coerceAtLeast(1f),
            intArrayOf(
                Color.TRANSPARENT,
                whiteAt(alpha),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        val save = canvas.save()
        canvas.clipPath(shapePath)
        canvas.drawPath(shapePath, sweepPaint)
        canvas.restore()
    }

    private fun buildMaterial(w: Float, h: Float) {
        val isDark = resources.isNightMode()
        val tier = glassTier()
        val tintAlpha = glassTintAlpha(context)

        gradientShader = fillShader(w, h, isDark, tier, tintAlpha)

        baseFillPaint.color = GlassRecipe.baseFillColor(tier, isDark)
        baseFillPaint.alpha =
            (GlassRecipe.baseFillAlpha(tier, tintAlpha, isDark) * 255f).toInt().coerceIn(0, 255)

        noisePaint.shader = buildNoiseShader()
        noisePaint.alpha = (GlassRecipe.NOISE_ALPHA * 255f).toInt()
        // BlendMode.SOFT_LIGHT landed in API 29 and PorterDuff.Mode.OVERLAY is supported in a
        // hardware framebuffer from API 28; below that the tile is composited plainly, which is
        // a slightly flatter but still correct noise layer.
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                noisePaint.blendMode = android.graphics.BlendMode.SOFT_LIGHT
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ->
                noisePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
            else -> noisePaint.xfermode = null
        }

        rimPaint.color = GlassRecipe.rimColor(isDark)
        val strokePx = (0.5f * resources.displayMetrics.density).coerceAtLeast(1f)
        rimPaint.strokeWidth = strokePx
        rimPaint.maskFilter = null

        buildRimPath(w, h, strokePx / 2f)
        edgeBitmap?.recycle()
        edgeBitmap = renderEdgeBitmap(w, h, isDark)

        materialDirty = false
    }

    private fun fillShader(
        w: Float,
        h: Float,
        isDark: Boolean,
        tier: GlassTier,
        tintAlpha: Float,
    ): LinearGradient? {
        if (h <= 0f) return null
        // On the scrim tier the base fill already carries the legibility; the ramp is then a
        // modulation of it, so it is scaled by the base alpha rather than being its own veil.
        val scale = if (tier is GlassTier.Scrim) 1f else {
            (tintAlpha * if (isDark) GlassColors.GLASS_DARK_BASE_ALPHA else 1f)
        }
        val colors = GlassRecipe.fillGradientColors(isDark).map { c ->
            Color.argb(
                (Color.alpha(c) * scale).toInt().coerceIn(0, 255),
                Color.red(c), Color.green(c), Color.blue(c),
            )
        }.toIntArray()
        return LinearGradient(0f, 0f, 0f, h, colors, null, Shader.TileMode.CLAMP)
    }

    /**
     * The lit rim: the top [GlassRecipe.RIM_SWEEP_FRACTION] of the shape's outline.
     *
     * The geometry is [Superellipse.rimPoints] - pure, shared with the Compose side, and unit
     * tested - so both the View surfaces and the Compose ones draw the same lit edge.
     */
    private fun buildRimPath(w: Float, h: Float, inset: Float) {
        rimPath.rewind()
        val rim = Superellipse.rimPoints(
            width = w,
            height = h,
            inset = inset,
            fraction = GlassRecipe.rimSweepFraction(),
            circle = circle,
        )
        if (rim.isEmpty()) return
        rimPath.moveTo(rim[0], rim[1])
        for (i in 2 until rim.size step 2) rimPath.lineTo(rim[i], rim[i + 1])
    }

    private fun buildNoiseShader(): Shader {
        val size = GlassRecipe.NOISE_TILE_PX
        noiseTile ?: run {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(
                GlassRecipe.noisePixels(size),
                0,
                size,
                0,
                0,
                size,
                size,
            )
            noiseTile = bitmap
        }
        return BitmapShader(noiseTile!!, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    }

    /**
     * Rasterises the edge glow and the drop shadow into a software bitmap.
     *
     * `Paint.setMaskFilter()` is not supported with hardware acceleration at any API level
     * (platform support table, "Support for drawing operations"), so a [BlurMaskFilter] stroke
     * is drawn here into an ordinary `Bitmap` canvas - where it does work - and the result is
     * blitted by [onDraw]. The bitmap is padded by the blur extent on every side so the glow is
     * not clipped off at the bounds.
     */
    private fun renderEdgeBitmap(w: Float, h: Float, isDark: Boolean): Bitmap {
        val d = resources.displayMetrics.density
        val glowRadius = GlassRecipe.EDGE_GLOW_RADIUS_DP * d
        val shadowRadius = GlassRecipe.SHADOW_RADIUS_DP * d
        val shadowDy = GlassRecipe.SHADOW_DY_DP * d
        val pad = (glowRadius + shadowRadius + kotlin.math.abs(shadowDy) + 2f * d)
        val width = (w + pad * 2f).toInt().coerceAtLeast(1)
        val height = (h + pad * 2f).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.translate(pad, pad)

        val path = Superellipse.path(w, h, circle = circle)
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isAntiAlias = true
            // setShadowLayer for non-text needs API 28; below that the platform ignores it and
            // the surface simply renders without a cast shadow rather than crashing.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                setShadowLayer(shadowRadius, 0f, shadowDy, blackAt(GlassRecipe.shadowAlpha(isDark)))
            }
            color = blackAt(GlassRecipe.shadowAlpha(isDark))
        }
        canvas.drawPath(path, shadow)

        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (1.5f * d)
            color = whiteAt(GlassRecipe.edgeGlowAlpha(isDark))
            maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(path, glow)
        return bitmap
    }

    private fun invalidateShape() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        Superellipse.path(w, h, circle = circle, outPath = shapePath)
        gradientShader = null
        materialDirty = true
        invalidate()
    }

    /**
     * Rounded-rectangle outline for `clipToOutline`, deliberately NOT the superellipse: see the
     * class KDoc. Distance is measured so the outline still degrades to a capsule once the
     * radius exceeds half the shorter side.
     */
    private inner class SquircleOutlineProvider : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            if (circle) {
                outline.setOval(0, 0, view.width, view.height)
                return
            }
            val maxRadius = minOf(view.width, view.height) / 2f
            val radius = radiusPx.coerceIn(0f, maxRadius)
            outline.setRoundRect(0, 0, view.width, view.height, radius)
        }
    }

    private fun whiteAt(alpha: Float): Int =
        Color.argb((alpha.coerceIn(0f, 1f) * 255f).toInt(), 255, 255, 255)

    private fun blackAt(alpha: Float): Int =
        Color.argb((alpha.coerceIn(0f, 1f) * 255f).toInt(), 0, 0, 0)

    private companion object {
        const val DEFAULT_CORNER_RADIUS_DP = 24f
    }
}

/** Night-mode check shared by the View-side glass code. */
fun android.content.res.Resources.isNightMode(): Boolean =
    (configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
    android.content.res.Configuration.UI_MODE_NIGHT_YES
