// Copyright 2026 Lofrano Tecnologia. MIT licensed.
package br.com.lofranotecnologia.revision

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ImmutabilityTest {
    @Test
    fun `exported map lists entries keys and values cannot mutate history`() {
        val index = RevisionValidityIndex()
        index.put("x", 0, 10, "a", 0)
        val old = index.snapshot(1)
        val map = old.entities as MutableMap<String, List<Interval>>
        val list = old.entities.getValue("x") as MutableList<Interval>
        assertThrows(UnsupportedOperationException::class.java) { map.clear() }
        assertThrows(UnsupportedOperationException::class.java) { map["other"] = emptyList() }
        assertThrows(UnsupportedOperationException::class.java) { list.clear() }
        assertThrows(UnsupportedOperationException::class.java) { list[0] = list[0].copy(value = "bad") }
        assertThrows(UnsupportedOperationException::class.java) {
            (old.entities.keys as MutableSet<String>).remove("x")
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (old.entities.values as MutableCollection<List<Interval>>).clear()
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (old.entities.entries.first() as MutableMap.MutableEntry<String, List<Interval>>)
                .setValue(emptyList())
        }
        val detached = list[0].copy(value = "detached")
        assertEquals("detached", detached.value)
        index.put("x", 3, 7, "b", 1)
        assertEquals(FactValue.Present("a"), index.query(old, "x", 5))
        assertEquals(FactValue.Present("b"), index.query(index.snapshot(2), "x", 5))
        assertEquals(1L, old.intervalCount)
    }

    @Test
    fun `query is self-contained even when invoked through a different index`() {
        val origin = RevisionValidityIndex()
        origin.put("x", 0, 1, "a", 0)
        val snapshot = origin.snapshot(1)
        val other = RevisionValidityIndex()
        assertEquals(FactValue.Present("a"), other.query(snapshot, "x", 0))
        assertEquals(0L, other.currentRevision)
    }
}
