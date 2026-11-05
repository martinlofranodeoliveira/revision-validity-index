// Copyright 2026 Lofrano Tecnologia. MIT licensed.
package br.com.lofranotecnologia.revision

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HistoricalContractTest {
    @Test
    fun `corrections and erasure do not rewrite knowledge history`() {
        val index = RevisionValidityIndex()
        val zero = index.snapshot(0)
        assertEquals(FactValue.Missing, index.query(zero, "label", 5))
        assertEquals(1L, index.put("label", 0, 10, "blue", 0))
        val one = index.snapshot(1)
        assertEquals(2L, index.put("label", 4, 7, "green", 1))
        val two = index.snapshot(2)
        assertEquals(FactValue.Present("blue"), index.query(one, "label", 5))
        assertEquals(FactValue.Present("green"), index.query(two, "label", 5))
        assertEquals(FactValue.Present("blue"), index.query(two, "label", 7))
        assertEquals(FactValue.Missing, index.query(two, "label", 10))
        assertEquals(3L, index.erase("label", 6, 9, 2))
        val three = index.snapshot(3)
        assertEquals(FactValue.Missing, index.query(three, "label", 6))
        assertEquals(FactValue.Present("green"), index.query(three, "label", 5))
        assertEquals(FactValue.Present("blue"), index.query(three, "label", 9))
        assertEquals(FactValue.Present("green"), index.query(two, "label", 6))
        assertEquals(FactValue.Missing, index.query(zero, "label", 5))
        assertSame(one, index.snapshot(1))
    }

    @Test
    fun `no-op checks expectation before returning and never allocates history`() {
        val index = RevisionValidityIndex()
        assertEquals(0L, index.erase("absent", -10, 10, 0))
        index.put("label", 0, 10, "blue", 0)
        val before = index.statistics()
        assertEquals(1L, index.put("label", 2, 8, "blue", 1))
        assertEquals(before, index.statistics())
        val error = assertThrows(StaleRevision::class.java) {
            index.put("label", 0, 10, "blue", 0)
        }
        assertEquals(0L, error.expected)
        assertEquals(1L, error.actual)
        assertEquals(before, index.statistics())
    }

    @Test
    fun `unknown revisions are distinguished from missing facts`() {
        val index = RevisionValidityIndex()
        assertThrows(UnknownRevision::class.java) { index.snapshot(-1) }
        assertThrows(UnknownRevision::class.java) { index.snapshot(1) }
        assertThrows(UnknownRevision::class.java) { index.snapshot(Long.MAX_VALUE) }
        assertEquals(FactValue.Missing, index.query(index.snapshot(0), "unknown", Long.MIN_VALUE))
    }

    @Test
    fun `empty strings are facts and comparisons do not normalize text`() {
        val index = RevisionValidityIndex()
        index.put("label", 0, 1, "", 0)
        assertEquals(FactValue.Present(""), index.query(index.snapshot(1), "label", 0))
        index.put("label", 0, 1, "é", 1)
        index.put("label", 0, 1, "e\u0301", 2)
        assertEquals(3L, index.currentRevision)
        assertEquals(FactValue.Present("é"), index.query(index.snapshot(2), "label", 0))
    }
}
