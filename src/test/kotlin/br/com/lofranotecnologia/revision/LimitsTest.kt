// Copyright 2026 Lofrano Tecnologia. MIT licensed.
package br.com.lofranotecnologia.revision

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LimitsTest {
    @Test
    fun `two retained changes exclude zero and reject third atomically`() {
        val index = RevisionValidityIndex(Limits(maxRetainedRevisions = 2))
        index.put("x", 0, 10, "a", 0)
        index.put("x", 4, 6, "b", 1)
        val snapshots = (0L..2L).map { index.snapshot(it) }
        val before = index.statistics()
        val failure = assertThrows(HistoryLimit::class.java) {
            index.erase("x", 0, 10, 2)
        }
        assertEquals(LimitResource.RETAINED_REVISIONS, failure.resource)
        assertEquals(before, index.statistics())
        snapshots.forEach { assertSame(it, index.snapshot(it.revision)) }
        assertEquals(FactValue.Present("a"), index.query(snapshots[1], "x", 5))
        assertEquals(FactValue.Present("b"), index.query(snapshots[2], "x", 5))
        assertEquals(2L, index.put("x", 4, 6, "b", 2))
        assertThrows(StaleRevision::class.java) { index.put("x", 4, 6, "b", 1) }
        assertEquals(before, index.statistics())
    }

    @Test
    fun `interval cap applies after coalescing and rejects a split`() {
        val index = RevisionValidityIndex(Limits(maxIntervalsPerEntity = 2))
        index.put("x", 0, 1, "a", 0)
        index.put("x", 2, 3, "a", 1)
        assertEquals(3L, index.put("x", 1, 2, "a", 2))
        assertEquals(1L, index.snapshot(3).intervalCount)
        val strict = RevisionValidityIndex(Limits(maxIntervalsPerEntity = 1))
        strict.put("x", 0, 4, "a", 0)
        val before = strict.statistics()
        val failure = assertThrows(HistoryLimit::class.java) { strict.erase("x", 1, 3, 1) }
        assertEquals(LimitResource.INTERVALS_PER_ENTITY, failure.resource)
        assertEquals(before, strict.statistics())
        assertEquals(FactValue.Present("a"), strict.query(strict.snapshot(1), "x", 2))
    }

    @Test
    fun `references count each retained snapshot even with unchanged intervals`() {
        val index = RevisionValidityIndex(Limits(maxRetainedIntervalReferences = 3))
        index.put("a", 0, 1, "same", 0)
        index.put("b", 0, 1, "other", 1)
        assertEquals(3L, index.statistics().retainedIntervalReferences)
        val failure = assertThrows(HistoryLimit::class.java) { index.erase("b", 0, 1, 2) }
        assertEquals(LimitResource.RETAINED_INTERVAL_REFERENCES, failure.resource)
        assertEquals(2L, index.currentRevision)
        assertEquals(FactValue.Present("other"), index.query(index.snapshot(2), "b", 0))
        assertEquals(2L, index.put("a", 0, 1, "same", 2))
        assertEquals(3L, index.statistics().retainedIntervalReferences)
    }

    @Test
    fun `empty snapshots cost a revision but no interval references and free active entities`() {
        val index = RevisionValidityIndex(Limits(maxEntities = 1, maxRetainedIntervalReferences = 2))
        index.put("a", 0, 1, "a", 0)
        val failure = assertThrows(HistoryLimit::class.java) { index.put("b", 0, 1, "b", 1) }
        assertEquals(LimitResource.ENTITIES, failure.resource)
        index.erase("a", 0, 1, 1)
        assertEquals(0, index.statistics().currentEntities)
        assertEquals(1L, index.statistics().retainedIntervalReferences)
        index.put("b", 0, 1, "b", 2)
        assertEquals(3, index.statistics().retainedRevisions)
        assertEquals(2L, index.statistics().retainedIntervalReferences)
        assertEquals(FactValue.Present("a"), index.query(index.snapshot(1), "a", 0))
        assertFalse(index.snapshot(3).entities.containsKey("a"))
    }

    @Test
    fun `zero caps allow no-ops but not state changes and Long max reference cap does not overflow`() {
        val index = RevisionValidityIndex(Limits(0, 0, 0, 0))
        assertEquals(0L, index.erase("x", 0, 1, 0))
        val zeroFailure = assertThrows(HistoryLimit::class.java) { index.put("x", 0, 1, "a", 0) }
        assertEquals(LimitResource.RETAINED_REVISIONS, zeroFailure.resource)
        assertEquals(IndexStatistics(0, 0, 0, 0, 0), index.statistics())

        val maxRefIndex = RevisionValidityIndex(Limits(maxRetainedIntervalReferences = Long.MAX_VALUE))
        assertEquals(1L, maxRefIndex.put("x", 0, 1, "a", 0))
        assertEquals(1L, maxRefIndex.statistics().retainedIntervalReferences)

        assertThrows(IllegalArgumentException::class.java) { Limits(maxRetainedRevisions = -1) }
        assertThrows(IllegalArgumentException::class.java) { Limits(maxRetainedRevisions = Int.MAX_VALUE) }
        assertThrows(IllegalArgumentException::class.java) { Limits(maxEntities = -1) }
        assertThrows(IllegalArgumentException::class.java) { Limits(maxIntervalsPerEntity = -1) }
        assertThrows(IllegalArgumentException::class.java) { Limits(maxRetainedIntervalReferences = -1) }
    }
}
