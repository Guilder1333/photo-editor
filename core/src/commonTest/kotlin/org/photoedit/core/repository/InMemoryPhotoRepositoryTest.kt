package org.photoedit.core.repository

import org.photoedit.core.model.Photo
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryPhotoRepositoryTest {

    private lateinit var repo: InMemoryPhotoRepository

    @BeforeTest
    fun setUp() {
        repo = InMemoryPhotoRepository()
    }

    private fun photo(id: String) = Photo(
        id = id, uri = "file://$id.jpg", width = 100, height = 100, timestampMs = 0L,
    )

    @Test
    fun `getAll returns empty list initially`() {
        assertTrue(repo.getAll().isEmpty())
    }

    @Test
    fun `add then getById returns the photo`() {
        val p = photo("a")
        repo.add(p)
        assertEquals(p, repo.getById("a"))
    }

    @Test
    fun `getById returns null for unknown id`() {
        assertNull(repo.getById("missing"))
    }

    @Test
    fun `add multiple photos preserves insertion order`() {
        repo.add(photo("x"))
        repo.add(photo("y"))
        repo.add(photo("z"))
        assertEquals(listOf("x", "y", "z"), repo.getAll().map { it.id })
    }

    @Test
    fun `add with existing id replaces the record`() {
        repo.add(photo("a"))
        val updated = photo("a").copy(width = 999)
        repo.add(updated)
        assertEquals(999, repo.getById("a")?.width)
        assertEquals(1, repo.getAll().size)
    }

    @Test
    fun `remove deletes the photo`() {
        repo.add(photo("a"))
        repo.remove("a")
        assertNull(repo.getById("a"))
        assertTrue(repo.getAll().isEmpty())
    }

    @Test
    fun `remove on unknown id is a no-op`() {
        repo.add(photo("a"))
        repo.remove("does-not-exist")
        assertEquals(1, repo.getAll().size)
    }
}
