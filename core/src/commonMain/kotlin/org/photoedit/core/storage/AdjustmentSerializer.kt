package org.photoedit.core.storage

import org.photoedit.core.Adjustment
import org.photoedit.core.AdjustmentRegistry

/**
 * JSON serializer for [Adjustment] lists.
 *
 * Encoding delegates to each adjustment's own [Adjustment.toFields] and [Adjustment.typeKey].
 * Decoding looks up the type key in [AdjustmentRegistry] and delegates to the matching
 * [org.photoedit.core.AdjustmentType.fromFields].
 *
 * Adding a new adjustment type requires no changes here — only adding the companion object
 * to [AdjustmentRegistry] and implementing [Adjustment.toFields] on the class.
 *
 * Example output:
 * ```json
 * [{"type":"exposure","ev":1.5},{"type":"hsl","hs":"0.0 30.0 ...","ss":"...","ls":"..."}]
 * ```
 */
object AdjustmentSerializer {

    private val byTypeKey = AdjustmentRegistry.all.associateBy { it.typeKey }

    // ── Encode ────────────────────────────────────────────────────────────────

    fun toJson(adjustments: List<Adjustment>): String =
        "[${adjustments.joinToString(",") { buildObj(it.typeKey, it.toFields()) }}]"

    // ── Decode ────────────────────────────────────────────────────────────────

    fun fromJson(json: String): List<Adjustment> =
        parseObjectArray(json.trim()).map { fields ->
            val type = fields["type"]
            val factory = checkNotNull(byTypeKey[type]) { "Unknown adjustment type in JSON: $type" }
            factory.fromFields(fields)
        }

    // ── JSON building ─────────────────────────────────────────────────────────

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
}
