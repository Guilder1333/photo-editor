package org.photoedit.core.storage

import org.photoedit.core.Adjustment
import org.photoedit.core.adjustments.*
import kotlin.reflect.KClass

/**
 * Hand-crafted JSON serializer for [Adjustment] lists.
 *
 * New adjustment types are supported by adding one entry to [registry] — no type checks
 * anywhere else in this file need to change.
 *
 * Example output:
 * ```json
 * [{"type":"exposure","ev":1.5},{"type":"hsl","hs":"0.0 30.0 ...","ss":"...","ls":"..."}]
 * ```
 */
object AdjustmentSerializer {

    // ── Codec registry ────────────────────────────────────────────────────────

    private class Codec<T : Adjustment>(
        val typeKey: String,
        val klass: KClass<T>,
        private val toFields: (T) -> List<Pair<String, Any?>>,
        val fromFields: (Map<String, String?>) -> T,
    ) {
        @Suppress("UNCHECKED_CAST")
        fun encodeUnchecked(adj: Adjustment): List<Pair<String, Any?>> = toFields(adj as T)
    }

    private fun <T : Adjustment> codec(
        typeKey: String,
        klass: KClass<T>,
        toFields: (T) -> List<Pair<String, Any?>>,
        fromFields: (Map<String, String?>) -> T,
    ): Codec<T> = Codec(typeKey, klass, toFields, fromFields)

    private val registry: List<Codec<*>> = listOf(
        codec("exposure",        Exposure::class,       { listOf("ev"       to it.ev) },       { Exposure(it.float("ev")) }),
        codec("brightness",      Brightness::class,     { listOf("value"    to it.value) },    { Brightness(it.float("value")) }),
        codec("contrast",        Contrast::class,       { listOf("value"    to it.value) },    { Contrast(it.float("value")) }),
        codec("highlights",      Highlights::class,     { listOf("value"    to it.value) },    { Highlights(it.float("value")) }),
        codec("shadows",         Shadows::class,        { listOf("value"    to it.value) },    { Shadows(it.float("value")) }),
        codec("whites",          Whites::class,         { listOf("value"    to it.value) },    { Whites(it.float("value")) }),
        codec("blacks",          Blacks::class,         { listOf("value"    to it.value) },    { Blacks(it.float("value")) }),
        codec("temperature",     Temperature::class,    { listOf("value"    to it.value) },    { Temperature(it.float("value")) }),
        codec("tint",            Tint::class,           { listOf("value"    to it.value) },    { Tint(it.float("value")) }),
        codec("saturation",      Saturation::class,     { listOf("value"    to it.value) },    { Saturation(it.float("value")) }),
        codec("vibrance",        Vibrance::class,       { listOf("value"    to it.value) },    { Vibrance(it.float("value")) }),
        codec("sharpness",       Sharpness::class,      { listOf("value"    to it.value) },    { Sharpness(it.float("value")) }),
        codec("clarity",         Clarity::class,        { listOf("value"    to it.value) },    { Clarity(it.float("value")) }),
        codec("noise_reduction", NoiseReduction::class, { listOf("strength" to it.strength) }, { NoiseReduction(it.float("strength")) }),
        codec("vignette", Vignette::class,
            { listOf("strength" to it.strength, "feather" to it.feather) },
            { Vignette(it.float("strength"), it.floatOr("feather", 0.5f)) },
        ),
        codec("crop", Crop::class,
            { listOf("left" to it.left, "top" to it.top, "right" to it.right, "bottom" to it.bottom) },
            { Crop(it.floatOr("left", 0f), it.floatOr("top", 0f), it.floatOr("right", 0f), it.floatOr("bottom", 0f)) },
        ),
        codec("curves", Curves::class,
            { listOf(
                "rgb"   to encodeCurve(it.rgb),
                "red"   to it.red?.let(::encodeCurve),
                "green" to it.green?.let(::encodeCurve),
                "blue"  to it.blue?.let(::encodeCurve),
            ) },
            { Curves(
                rgb   = decodeCurve(it.str("rgb")),
                red   = it["red"]?.let(::decodeCurve),
                green = it["green"]?.let(::decodeCurve),
                blue  = it["blue"]?.let(::decodeCurve),
            ) },
        ),
        codec("hsl", Hsl::class,
            { listOf(
                "hs" to encodeFloatArray(it.hueShifts),
                "ss" to encodeFloatArray(it.saturationShifts),
                "ls" to encodeFloatArray(it.lightnessShifts),
            ) },
            { Hsl(
                hueShifts        = decodeFloatArray(it.str("hs")),
                saturationShifts = decodeFloatArray(it.str("ss")),
                lightnessShifts  = decodeFloatArray(it.str("ls")),
            ) },
        ),
    )

    private val byClass: Map<KClass<*>, Codec<*>> = registry.associateBy { it.klass }
    private val byTypeKey: Map<String, Codec<*>> = registry.associateBy { it.typeKey }

    // ── Field reader helpers ───────────────────────────────────────────────────

    private fun Map<String, String?>.float(key: String) = getValue(key)!!.toFloat()
    private fun Map<String, String?>.floatOr(key: String, default: Float) = get(key)?.toFloat() ?: default
    private fun Map<String, String?>.str(key: String) = getValue(key)!!

    // ── Encode ────────────────────────────────────────────────────────────────

    fun toJson(adjustments: List<Adjustment>): String =
        "[${adjustments.joinToString(",") { encode(it) }}]"

    private fun encode(adj: Adjustment): String {
        val codec = checkNotNull(byClass[adj::class]) { "No codec registered for ${adj::class.simpleName}" }
        return buildObj(codec.typeKey, codec.encodeUnchecked(adj))
    }

    // ── Decode ────────────────────────────────────────────────────────────────

    fun fromJson(json: String): List<Adjustment> =
        parseObjectArray(json.trim()).map { decode(it) }

    private fun decode(fields: Map<String, String?>): Adjustment {
        val type = fields["type"]
        val codec = checkNotNull(byTypeKey[type]) { "Unknown adjustment type in JSON: $type" }
        return codec.fromFields(fields)
    }

    // ── JSON building helpers ─────────────────────────────────────────────────

    private fun buildObj(type: String, fields: List<Pair<String, Any?>>): String {
        val sb = StringBuilder("{\"type\":\"$type\"")
        for ((k, v) in fields) {
            sb.append(",\"$k\":")
            when (v) {
                null      -> sb.append("null")
                is String -> sb.append("\"$v\"")
                is Float  -> sb.append(v.toString())
                else      -> sb.append("\"$v\"")
            }
        }
        sb.append("}")
        return sb.toString()
    }

    private fun encodeCurve(curve: List<Pair<Float, Float>>): String =
        curve.joinToString(" ") { (x, y) -> "$x $y" }

    private fun encodeFloatArray(arr: FloatArray): String = arr.joinToString(" ")

    // ── JSON parser ───────────────────────────────────────────────────────────

    private fun parseObjectArray(json: String): List<Map<String, String?>> {
        val body = json.removePrefix("[").removeSuffix("]").trim()
        if (body.isEmpty()) return emptyList()

        val results = mutableListOf<Map<String, String?>>()
        var depth = 0; var start = 0; var inString = false
        var i = 0
        while (i < body.length) {
            val c = body[i]
            when {
                c == '"' && (i == 0 || body[i - 1] != '\\') -> inString = !inString
                !inString && c == '{' -> { if (depth++ == 0) start = i }
                !inString && c == '}' -> { if (--depth == 0) results.add(parseFields(body.substring(start, i + 1))) }
            }
            i++
        }
        return results
    }

    private fun parseFields(objectJson: String): Map<String, String?> {
        val result = LinkedHashMap<String, String?>()
        val body = objectJson.trim().removePrefix("{").removeSuffix("}").trim()
        var pos = 0

        fun skipWs() { while (pos < body.length && body[pos].isWhitespace()) pos++ }

        fun readQuotedString(): String {
            pos++ // skip opening '"'
            val sb = StringBuilder()
            while (pos < body.length && body[pos] != '"') {
                if (body[pos] == '\\' && pos + 1 < body.length) { pos++; sb.append(body[pos]) }
                else sb.append(body[pos])
                pos++
            }
            if (pos < body.length) pos++ // skip closing '"'
            return sb.toString()
        }

        while (pos < body.length) {
            skipWs()
            if (pos >= body.length || body[pos] != '"') break

            val key = readQuotedString()

            skipWs()
            if (pos < body.length && body[pos] == ':') pos++
            skipWs()

            val value: String? = when {
                pos >= body.length               -> null
                body[pos] == '"'                 -> readQuotedString()
                body.startsWith("null", pos)     -> { pos += 4; null }
                else -> {
                    val start = pos
                    while (pos < body.length && body[pos] != ',' && body[pos] != '}') pos++
                    body.substring(start, pos).trim()
                }
            }
            result[key] = value

            skipWs()
            if (pos < body.length && body[pos] == ',') pos++
        }
        return result
    }

    private fun decodeCurve(encoded: String): List<Pair<Float, Float>> {
        val nums = encoded.trim().split(" ").filter { it.isNotEmpty() }.map { it.toFloat() }
        return (nums.indices step 2).map { nums[it] to nums[it + 1] }
    }

    private fun decodeFloatArray(encoded: String): FloatArray =
        encoded.trim().split(" ").filter { it.isNotEmpty() }.map { it.toFloat() }.toFloatArray()
}
