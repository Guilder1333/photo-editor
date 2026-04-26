package org.photoedit.core.storage

import org.photoedit.core.Adjustment
import org.photoedit.core.adjustments.*

/**
 * Hand-crafted JSON serializer for [Adjustment] lists.
 *
 * Produces a JSON array where each element is a flat object with a `"type"` discriminator
 * and type-specific float fields. Float arrays (Hsl, Curves) are encoded as
 * space-separated strings to keep the format single-level and easy to parse without
 * a JSON library.
 *
 * Example output:
 * ```json
 * [{"type":"exposure","ev":1.5},{"type":"hsl","hs":"0.0 30.0 0.0 0.0 0.0 0.0 0.0 0.0","ss":"0.0 ...","ls":"0.0 ..."}]
 * ```
 */
object AdjustmentSerializer {

    // ── Encode ────────────────────────────────────────────────────────────────

    fun toJson(adjustments: List<Adjustment>): String =
        "[${adjustments.joinToString(",") { encode(it) }}]"

    private fun encode(adj: Adjustment): String = when (adj) {
        is Exposure       -> obj("exposure",        "ev"       to adj.ev)
        is Brightness     -> obj("brightness",      "value"    to adj.value)
        is Contrast       -> obj("contrast",        "value"    to adj.value)
        is Highlights     -> obj("highlights",      "value"    to adj.value)
        is Shadows        -> obj("shadows",         "value"    to adj.value)
        is Whites         -> obj("whites",          "value"    to adj.value)
        is Blacks         -> obj("blacks",          "value"    to adj.value)
        is Temperature    -> obj("temperature",     "value"    to adj.value)
        is Tint           -> obj("tint",            "value"    to adj.value)
        is Saturation     -> obj("saturation",      "value"    to adj.value)
        is Vibrance       -> obj("vibrance",        "value"    to adj.value)
        is Sharpness      -> obj("sharpness",       "value"    to adj.value)
        is Clarity        -> obj("clarity",         "value"    to adj.value)
        is NoiseReduction -> obj("noise_reduction", "strength" to adj.strength)
        is Vignette       -> obj("vignette",        "strength" to adj.strength, "feather" to adj.feather)
        is Crop           -> obj("crop",            "left" to adj.left, "top" to adj.top, "right" to adj.right, "bottom" to adj.bottom)
        is Curves         -> obj(
            "curves",
            "rgb"   to encodeCurve(adj.rgb),
            "red"   to adj.red?.let   { encodeCurve(it) },
            "green" to adj.green?.let { encodeCurve(it) },
            "blue"  to adj.blue?.let  { encodeCurve(it) },
        )
        is Hsl            -> obj(
            "hsl",
            "hs" to encodeFloatArray(adj.hueShifts),
            "ss" to encodeFloatArray(adj.saturationShifts),
            "ls" to encodeFloatArray(adj.lightnessShifts),
        )
        else -> throw IllegalArgumentException("Unknown adjustment type: ${adj::class.simpleName}")
    }

    private fun obj(type: String, vararg fields: Pair<String, Any?>): String {
        val sb = StringBuilder("{\"type\":\"$type\"")
        for ((k, v) in fields) {
            sb.append(",\"$k\":")
            when (v) {
                null       -> sb.append("null")
                is String  -> sb.append("\"$v\"")
                is Float   -> sb.append(v.toString())
                else       -> sb.append("\"$v\"")
            }
        }
        sb.append("}")
        return sb.toString()
    }

    /** Encodes a curve as space-separated x y pairs: "x0 y0 x1 y1 ..." */
    private fun encodeCurve(curve: List<Pair<Float, Float>>): String =
        curve.joinToString(" ") { (x, y) -> "$x $y" }

    /** Encodes a FloatArray as space-separated values. */
    private fun encodeFloatArray(arr: FloatArray): String = arr.joinToString(" ")

    // ── Decode ────────────────────────────────────────────────────────────────

    fun fromJson(json: String): List<Adjustment> =
        parseObjectArray(json.trim()).map { decode(it) }

    private fun decode(fields: Map<String, String?>): Adjustment {
        fun float(key: String) = fields.getValue(key)!!.toFloat()
        fun floatOrDefault(key: String, default: Float) = fields[key]?.toFloat() ?: default

        return when (val type = fields["type"]) {
            "exposure"       -> Exposure(float("ev"))
            "brightness"     -> Brightness(float("value"))
            "contrast"       -> Contrast(float("value"))
            "highlights"     -> Highlights(float("value"))
            "shadows"        -> Shadows(float("value"))
            "whites"         -> Whites(float("value"))
            "blacks"         -> Blacks(float("value"))
            "temperature"    -> Temperature(float("value"))
            "tint"           -> Tint(float("value"))
            "saturation"     -> Saturation(float("value"))
            "vibrance"       -> Vibrance(float("value"))
            "sharpness"      -> Sharpness(float("value"))
            "clarity"        -> Clarity(float("value"))
            "noise_reduction"-> NoiseReduction(float("strength"))
            "vignette"       -> Vignette(float("strength"), floatOrDefault("feather", 0.5f))
            "crop"           -> Crop(
                left   = floatOrDefault("left",   0f),
                top    = floatOrDefault("top",    0f),
                right  = floatOrDefault("right",  0f),
                bottom = floatOrDefault("bottom", 0f),
            )
            "curves"         -> Curves(
                rgb   = decodeCurve(fields.getValue("rgb")!!),
                red   = fields["red"]?.let   { decodeCurve(it) },
                green = fields["green"]?.let { decodeCurve(it) },
                blue  = fields["blue"]?.let  { decodeCurve(it) },
            )
            "hsl"            -> Hsl(
                hueShifts        = decodeFloatArray(fields.getValue("hs")!!),
                saturationShifts = decodeFloatArray(fields.getValue("ss")!!),
                lightnessShifts  = decodeFloatArray(fields.getValue("ls")!!),
            )
            else -> throw IllegalArgumentException("Unknown adjustment type in JSON: $type")
        }
    }

    /** Decodes "x0 y0 x1 y1 ..." back into a list of (x, y) pairs. */
    private fun decodeCurve(encoded: String): List<Pair<Float, Float>> {
        val nums = encoded.trim().split(" ").filter { it.isNotEmpty() }.map { it.toFloat() }
        return (nums.indices step 2).map { nums[it] to nums[it + 1] }
    }

    private fun decodeFloatArray(encoded: String): FloatArray =
        encoded.trim().split(" ").filter { it.isNotEmpty() }.map { it.toFloat() }.toFloatArray()

    // ── JSON parser ───────────────────────────────────────────────────────────

    /** Splits a JSON array string into a list of parsed field maps (one per object). */
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

    /**
     * Parses a flat JSON object like `{"type":"exposure","ev":1.5}` into a
     * `Map<String, String?>` where values are the raw string content (unquoted for
     * numbers, stripped of quotes for strings, null for JSON null).
     */
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
            if (pos < body.length && body[pos] == ':') pos++ // skip ':'
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
            if (pos < body.length && body[pos] == ',') pos++ // skip comma between fields
        }
        return result
    }
}
