package org.photoedit.core

/**
 * Implemented by each [Adjustment] companion object to describe that adjustment type.
 *
 * The registry ([AdjustmentRegistry]) lists all known implementations. The serializer
 * uses [typeKey] for decoding and [fromFields] for reconstruction; other subsystems
 * (UI, command palette, etc.) can use the same registry for discovery without coupling
 * to any storage concern.
 */
interface AdjustmentType {
    val typeKey: String
    fun fromFields(fields: Map<String, String?>): Adjustment
}
