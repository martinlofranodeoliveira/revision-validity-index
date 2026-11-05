// Copyright 2026 Lofrano Tecnologia. MIT licensed.
package br.com.lofranotecnologia.revision

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UnicodeAndBoundaryTest {
    @Test
    fun `Long endpoints are compared without duration arithmetic`() {
        val index = RevisionValidityIndex()
        index.put("x", Long.MIN_VALUE, Long.MAX_VALUE, "wide", 0)
        val one = index.snapshot(1)
        assertEquals(FactValue.Present("wide"), index.query(one, "x", Long.MIN_VALUE))
        assertEquals(FactValue.Present("wide"), index.query(one, "x", Long.MAX_VALUE - 1))
        assertEquals(FactValue.Missing, index.query(one, "x", Long.MAX_VALUE))
        index.put("x", -1, 1, "center", 1)
        index.erase("x", Long.MIN_VALUE, 0, 2)
        val three = index.snapshot(3)
        assertEquals(FactValue.Missing, index.query(three, "x", Long.MIN_VALUE))
        assertEquals(FactValue.Missing, index.query(three, "x", -1))
        assertEquals(FactValue.Present("center"), index.query(three, "x", 0))
        assertEquals(FactValue.Present("wide"), index.query(three, "x", 1))
        assertEquals(FactValue.Present("wide"), index.query(one, "x", 0))
        index.erase("x", 0, Long.MAX_VALUE, 3)
        assertTrue(index.snapshot(4).entities.isEmpty())
    }

    @Test
    fun `validation precedes stale checks and follows documented interval entity value order`() {
        val index = RevisionValidityIndex()
        for ((from, until) in listOf(0L to 0L, 3L to -3L, Long.MAX_VALUE to Long.MIN_VALUE)) {
            assertThrows(InvalidInterval::class.java) {
                index.put("", from, until, "a".repeat(4097), -1)
            }
            assertThrows(InvalidInterval::class.java) {
                index.erase("", from, until, -1)
            }
        }
        assertThrows(InvalidEntity::class.java) {
            index.put("", 0, 1, "a".repeat(4097), -1)
        }
        assertThrows(InvalidEntity::class.java) {
            index.erase("", 0, 1, -1)
        }
        assertThrows(InvalidValue::class.java) {
            index.put("x", 0, 1, "a".repeat(4097), -1)
        }
        assertEquals(0L, index.currentRevision)
    }

    @Test
    fun `entity byte limits distinguish 1 2 3 and 4 byte UTF-8 sequences from characters`() {
        val index = RevisionValidityIndex()
        val ascii128 = "a".repeat(128)
        val twoByte128 = "é".repeat(64)
        val threeByte128 = "€".repeat(42) + "é"
        val fourByte128 = "🟦".repeat(32)
        assertEquals(1L, index.put(ascii128, 0, 1, "ok", 0))
        assertEquals(2L, index.put(twoByte128, 0, 1, "ok", 1))
        assertEquals(3L, index.put(threeByte128, 0, 1, "ok", 2))
        assertEquals(4L, index.put(fourByte128, 0, 1, "ok", 3))
        assertThrows(InvalidEntity::class.java) { index.put("a".repeat(129), 0, 1, "x", 4) }
        assertThrows(InvalidEntity::class.java) { index.erase("é".repeat(65), 0, 1, 4) }
        assertThrows(InvalidEntity::class.java) { index.put("€".repeat(43), 0, 1, "x", 4) }
        assertThrows(InvalidEntity::class.java) { index.put("🟦".repeat(32) + "a", 0, 1, "x", 4) }
        assertThrows(InvalidEntity::class.java) { index.query(index.snapshot(4), "", 0) }
        assertEquals(4L, index.currentRevision)
    }

    @Test
    fun `values allow exactly 4096 UTF-8 bytes across BMP and supplementary characters`() {
        val index = RevisionValidityIndex()
        val exactFourByte = "🟦".repeat(1024)
        val exactThreeByte = "€".repeat(1365) + "a"
        index.put("x", 0, 1, exactFourByte, 0)
        assertEquals(FactValue.Present(exactFourByte), index.query(index.snapshot(1), "x", 0))
        index.put("x", 0, 1, exactThreeByte, 1)
        assertEquals(FactValue.Present(exactThreeByte), index.query(index.snapshot(2), "x", 0))
        assertThrows(InvalidValue::class.java) { index.put("x", 0, 1, "🟦".repeat(1025), 2) }
        assertThrows(InvalidValue::class.java) { index.put("x", 0, 1, "€".repeat(1365) + "é", 2) }
        assertThrows(InvalidValue::class.java) { index.put("x", 0, 1, "a".repeat(4097), 2) }
        assertEquals(2L, index.currentRevision)
    }

    @Test
    fun `unpaired and reversed surrogates are rejected rather than replaced`() {
        val index = RevisionValidityIndex()
        val high = 0xd800.toChar().toString()
        val low = 0xdc00.toChar().toString()
        for (bad in listOf(high, low, high + "x", "x" + low, high + high, low + high)) {
            assertThrows(InvalidEntity::class.java) { index.put(bad, 0, 1, "ok", 0) }
            assertThrows(InvalidValue::class.java) { index.put("x", 0, 1, bad, 0) }
        }
        assertEquals(IndexStatistics(0, 0, 0, 0, 0), index.statistics())
        index.put("x", 0, 1, "\u0000", 0)
        assertEquals(FactValue.Present("\u0000"), index.query(index.snapshot(1), "x", 0))
    }
}
