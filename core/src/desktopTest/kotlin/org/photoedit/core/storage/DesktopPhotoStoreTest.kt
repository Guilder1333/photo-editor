package org.photoedit.core.storage

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopPhotoStoreTest {

    private lateinit var rootDir: File
    private lateinit var store: DesktopPhotoStore

    @BeforeTest
    fun setUp() {
        rootDir = createTempDirectory("desktop-photo-store-test").toFile()
        store = DesktopPhotoStore(rootDir)
    }

    @AfterTest
    fun tearDown() {
        rootDir.deleteRecursively()
    }

    @Test
    fun `write and read round-trips bytes exactly`() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        store.write("photo1", DataSlot.ORIGINAL, data)
        val result = store.read("photo1", DataSlot.ORIGINAL)
        assertNotNull(result)
        assertTrue(data.contentEquals(result))
    }

    @Test
    fun `read returns null for unknown photoId`() {
        assertNull(store.read("nonexistent", DataSlot.ORIGINAL))
    }

    @Test
    fun `read returns null for missing slot`() {
        store.write("photo1", DataSlot.ORIGINAL, byteArrayOf(1, 2, 3))
        assertNull(store.read("photo1", DataSlot.EDITS))
    }

    @Test
    fun `write creates correct directory and filename`() {
        store.write("photo1", DataSlot.ORIGINAL, byteArrayOf(42))
        val expected = rootDir.resolve("photo1").resolve("original.jpg")
        assertTrue(expected.exists(), "Expected file at $expected")
    }

    @Test
    fun `write overwrites existing slot`() {
        val first  = byteArrayOf(10, 20)
        val second = byteArrayOf(30, 40, 50)
        store.write("photo1", DataSlot.ORIGINAL, first)
        store.write("photo1", DataSlot.ORIGINAL, second)
        val result = store.read("photo1", DataSlot.ORIGINAL)!!
        assertTrue(second.contentEquals(result))
    }

    @Test
    fun `all four slots are independent`() {
        DataSlot.entries.forEachIndexed { idx, slot ->
            store.write("photo1", slot, byteArrayOf(idx.toByte()))
        }
        DataSlot.entries.forEachIndexed { idx, slot ->
            val result = store.read("photo1", slot)!!
            assertEquals(idx.toByte(), result[0], "Wrong bytes for slot $slot")
        }
    }

    @Test
    fun `delete removes all files for the photo`() {
        DataSlot.entries.forEach { slot ->
            store.write("photo1", slot, byteArrayOf(1))
        }
        store.delete("photo1")
        DataSlot.entries.forEach { slot ->
            assertNull(store.read("photo1", slot), "$slot should be deleted")
        }
        assertTrue(!rootDir.resolve("photo1").exists())
    }

    @Test
    fun `delete is idempotent for unknown photoId`() {
        store.delete("nonexistent")  // must not throw
    }

    @Test
    fun `listPhotoIds returns saved IDs sorted`() {
        store.write("beta",  DataSlot.ORIGINAL, byteArrayOf(1))
        store.write("alpha", DataSlot.ORIGINAL, byteArrayOf(2))
        store.write("gamma", DataSlot.ORIGINAL, byteArrayOf(3))
        val ids = store.listPhotoIds()
        assertEquals(listOf("alpha", "beta", "gamma"), ids)
    }

    @Test
    fun `listPhotoIds is empty when nothing saved`() {
        assertTrue(store.listPhotoIds().isEmpty())
    }

    @Test
    fun `listPhotoIds excludes deleted photos`() {
        store.write("p1", DataSlot.ORIGINAL, byteArrayOf(1))
        store.write("p2", DataSlot.ORIGINAL, byteArrayOf(2))
        store.delete("p1")
        val ids = store.listPhotoIds()
        assertEquals(listOf("p2"), ids)
    }

    @Test
    fun `unsafe characters in photoId are sanitized`() {
        val unsafeId = "photo/with:unsafe*chars"
        store.write(unsafeId, DataSlot.EDITS, byteArrayOf(7))
        val result = store.read(unsafeId, DataSlot.EDITS)
        assertNotNull(result)
        assertEquals(7, result[0])
        // The sanitized dir should exist and the raw one should not
        assertTrue(!rootDir.resolve(unsafeId).exists())
    }

    @Test
    fun `rootDir is created automatically`() {
        val nested = rootDir.resolve("nested/dir")
        val nestedStore = DesktopPhotoStore(nested)
        assertTrue(nested.exists(), "DesktopPhotoStore should create rootDir if missing")
        nestedStore.write("p", DataSlot.ORIGINAL, byteArrayOf(1))
        assertNotNull(nestedStore.read("p", DataSlot.ORIGINAL))
        nested.deleteRecursively()
    }

    @Test
    fun `multiple photos are stored independently`() {
        store.write("p1", DataSlot.ORIGINAL, byteArrayOf(1))
        store.write("p2", DataSlot.ORIGINAL, byteArrayOf(2))
        assertEquals(1, store.read("p1", DataSlot.ORIGINAL)!![0])
        assertEquals(2, store.read("p2", DataSlot.ORIGINAL)!![0])
    }
}
