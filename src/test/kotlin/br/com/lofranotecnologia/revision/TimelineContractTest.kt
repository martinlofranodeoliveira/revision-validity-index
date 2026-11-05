// Copyright 2026 Lofrano Tecnologia. MIT licensed.
package br.com.lofranotecnologia.revision

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TimelineContractTest {
    private fun triples(snapshot: Snapshot): List<Triple<Long, Long, String>> =
        snapshot.entities["x"].orEmpty().map { Triple(it.from, it.until, it.value) }

    @Test
    fun `adjacent equals coalesce but historical endpoint stays fixed`() {
        val index = RevisionValidityIndex()
        index.put("x", 0, 4, "blue", 0)
        val old = index.snapshot(1)
        index.put("x", 4, 10, "blue", 1)
        assertEquals(listOf(Triple(0L, 10L, "blue")), triples(index.snapshot(2)))
        assertEquals(listOf(Triple(0L, 4L, "blue")), triples(old))
        assertEquals(FactValue.Missing, index.query(old, "x", 4))
    }

    @Test
    fun `overwriting a bridge merges both neighbors`() {
        val index = RevisionValidityIndex()
        index.put("x", -4, 0, "a", 0)
        index.put("x", 3, 7, "a", 1)
        index.put("x", 0, 3, "a", 2)
        assertEquals(listOf(Triple(-4L, 7L, "a")), triples(index.snapshot(3)))
        index.put("x", -1, 2, "b", 3)
        assertEquals(
            listOf(Triple(-4L, -1L, "a"), Triple(-1L, 2L, "b"), Triple(2L, 7L, "a")),
            triples(index.snapshot(4))
        )
        index.put("x", -1, 2, "a", 4)
        assertEquals(listOf(Triple(-4L, 7L, "a")), triples(index.snapshot(5)))
    }

    @Test
    fun `erasing across several intervals leaves a gap and removes empty entities`() {
        val index = RevisionValidityIndex()
        index.put("x", -5, 0, "left", 0)
        index.put("x", 0, 5, "middle", 1)
        index.put("x", 5, 10, "right", 2)
        index.erase("x", -2, 8, 3)
        val partial = index.snapshot(4)
        assertEquals(listOf(Triple(-5L, -2L, "left"), Triple(8L, 10L, "right")), triples(partial))
        assertEquals(FactValue.Missing, index.query(partial, "x", 0))
        index.erase("x", -10, 20, 4)
        assertTrue(index.snapshot(5).entities.isEmpty())
        assertEquals(0L, index.snapshot(5).intervalCount)
        assertThrows(StaleRevision::class.java) { index.erase("absent", 0, 1, 4) }
        assertEquals(5L, index.erase("absent", 0, 1, 5))
        assertEquals(5L, index.currentRevision)
    }

    @Test
    fun `inserting before after and inside gaps preserves ordering`() {
        val index = RevisionValidityIndex()
        index.put("x", 10, 20, "b", 0)
        index.put("x", -10, 0, "a", 1)
        index.put("x", 30, 40, "d", 2)
        index.put("x", 22, 24, "c", 3)
        assertEquals(
            listOf(Triple(-10L, 0L, "a"), Triple(10L, 20L, "b"),
                Triple(22L, 24L, "c"), Triple(30L, 40L, "d")),
            triples(index.snapshot(4))
        )
    }
}
