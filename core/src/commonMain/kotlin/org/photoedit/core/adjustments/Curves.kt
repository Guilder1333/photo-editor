package org.photoedit.core.adjustments

import org.photoedit.core.Adjustment
import org.photoedit.core.AdjustmentId
import org.photoedit.core.ImageBuffer
import org.photoedit.core.Order
import kotlin.math.sqrt

/**
 * Per-channel tone curve adjustment using monotone cubic (Fritsch-Carlson) spline interpolation.
 *
 * Each curve is a list of (input, output) control points in [0, 1]. Points are sorted by
 * input value; the identity curve is `[(0, 0), (1, 1)]`.
 *
 * Application order: the [rgb] curve is applied first to all channels, then the individual
 * [red], [green], and [blue] curves are applied on top. Null per-channel curves are identity.
 *
 * @param rgb    Global RGB curve applied to all channels. Defaults to identity.
 * @param red    Red-channel curve. Null = identity.
 * @param green  Green-channel curve. Null = identity.
 * @param blue   Blue-channel curve. Null = identity.
 */
class Curves(
    val rgb: List<Pair<Float, Float>> = IDENTITY,
    val red: List<Pair<Float, Float>>? = null,
    val green: List<Pair<Float, Float>>? = null,
    val blue: List<Pair<Float, Float>>? = null,
) : Adjustment {

    override val id = AdjustmentId("curves")
    override val order = Order.CURVES

    private val rgbLut   = if (isIdentityCurve(rgb))           null else buildLut(rgb)
    private val redLut   = red?.let   { if (isIdentityCurve(it)) null else buildLut(it) }
    private val greenLut = green?.let { if (isIdentityCurve(it)) null else buildLut(it) }
    private val blueLut  = blue?.let  { if (isIdentityCurve(it)) null else buildLut(it) }

    override fun isIdentity() = rgbLut == null && redLut == null && greenLut == null && blueLut == null

    override fun apply(input: ImageBuffer): ImageBuffer {
        val p = input.pixels
        val out = FloatArray(p.size)
        var i = 0
        while (i < p.size) {
            var r = p[i]; var g = p[i + 1]; var b = p[i + 2]
            rgbLut?.let { r = it.eval(r); g = it.eval(g); b = it.eval(b) }
            redLut?.let   { r = it.eval(r) }
            greenLut?.let { g = it.eval(g) }
            blueLut?.let  { b = it.eval(b) }
            out[i]     = r
            out[i + 1] = g
            out[i + 2] = b
            out[i + 3] = p[i + 3]
            i += 4
        }
        return ImageBuffer(input.width, input.height, out)
    }

    companion object {
        val IDENTITY: List<Pair<Float, Float>> = listOf(0f to 0f, 1f to 1f)

        fun isIdentityCurve(points: List<Pair<Float, Float>>): Boolean {
            if (points.size == 2) {
                val (x0, y0) = points[0]; val (x1, y1) = points[1]
                return x0 == 0f && y0 == 0f && x1 == 1f && y1 == 1f
            }
            return points.all { (x, y) -> x == y }
        }

        internal fun buildLut(points: List<Pair<Float, Float>>): Lut {
            val sorted = points.sortedBy { it.first }
            val xs = FloatArray(sorted.size) { sorted[it].first }
            val ys = FloatArray(sorted.size) { sorted[it].second }
            return Lut(xs, ys, computeTangents(xs, ys))
        }

        // Fritsch-Carlson monotone cubic spline tangents.
        private fun computeTangents(xs: FloatArray, ys: FloatArray): FloatArray {
            val n = xs.size
            val m = FloatArray(n)
            if (n == 1) return m

            val delta = FloatArray(n - 1) { (ys[it + 1] - ys[it]) / (xs[it + 1] - xs[it]) }

            m[0] = delta[0]
            m[n - 1] = delta[n - 2]
            for (k in 1 until n - 1) m[k] = (delta[k - 1] + delta[k]) / 2f

            for (k in 0 until n - 1) {
                if (delta[k] == 0f) {
                    m[k] = 0f; m[k + 1] = 0f
                } else {
                    val alpha = m[k] / delta[k]
                    val beta  = m[k + 1] / delta[k]
                    val h = alpha * alpha + beta * beta
                    if (h > 9f) {
                        val tau = 3f / sqrt(h)
                        m[k]     = tau * alpha * delta[k]
                        m[k + 1] = tau * beta  * delta[k]
                    }
                }
            }
            return m
        }
    }

    internal class Lut(
        private val xs: FloatArray,
        private val ys: FloatArray,
        private val ms: FloatArray,
    ) {
        fun eval(x: Float): Float {
            val n = xs.size
            if (x <= xs[0])     return ys[0].coerceIn(0f, 1f)
            if (x >= xs[n - 1]) return ys[n - 1].coerceIn(0f, 1f)

            var lo = 0; var hi = n - 2
            while (lo < hi) {
                val mid = (lo + hi) / 2
                if (xs[mid + 1] < x) lo = mid + 1 else hi = mid
            }
            val k = lo
            val h = xs[k + 1] - xs[k]
            val t = (x - xs[k]) / h
            val t2 = t * t; val t3 = t2 * t

            return (
                (2f * t3 - 3f * t2 + 1f) * ys[k] +
                (t3 - 2f * t2 + t)        * h * ms[k] +
                (-2f * t3 + 3f * t2)      * ys[k + 1] +
                (t3 - t2)                 * h * ms[k + 1]
            ).coerceIn(0f, 1f)
        }
    }
}
