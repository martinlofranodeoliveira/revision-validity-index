// Copyright 2026 Lofrano Tecnologia. MIT licensed.
package br.com.lofranotecnologia.revision

import java.util.Random
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** Independent oracle: cells, not interval splitting. Every accepted matrix is retained. */
class MatrixOracleTest {
    private val names = listOf("x", "y", "z")
    private val first = -5
    private val width = 11
    private data class Edit(val entity: Int, val left: Int, val right: Int, val value: String?)

    @Test
    fun `fixed seed corrections agree with every historical cell`() {
        exercise(Limits(300, 3, 11, 20_000), 0x51a7L)
    }

    @Test
    fun `fixed seed rejected writes leave the entire historical matrix unchanged`() {
        exercise(Limits(24, 2, 3, 100), 0x1138L)
    }

    private fun runs(cells: List<String?>): Int {
        var count = 0
        var previous: String? = null
        for (value in cells) {
            if (value != null && value != previous) count += 1
            previous = value
        }
        return count
    }

    private fun exercise(limits: Limits, seed: Long) {
        val index = RevisionValidityIndex(limits)
        val matrices = mutableListOf(List(names.size) { List<String?>(width) { null } })
        var references = 0L
        var noOps = 0
        var rejected = 0
        val random = Random(seed)
        val directed = listOf(
            Edit(0, 1, 4, "a"),
            Edit(0, 4, 7, "a"),
            Edit(0, 2, 6, "a"),
            Edit(0, 3, 5, null),
            Edit(0, 3, 5, "b"),
            Edit(0, 3, 5, "a"),
            Edit(1, 0, 11, "c"),
            Edit(1, 0, 11, null)
        )
        repeat(220) { step ->
            val edit = if (step < directed.size) directed[step] else {
                val left = random.nextInt(width)
                val right = left + 1 + random.nextInt(width - left)
                val value = when (random.nextInt(4)) {
                    0 -> null
                    1 -> "a"
                    2 -> "b"
                    else -> ""
                }
                Edit(random.nextInt(names.size), left, right, value)
            }
            val current = matrices.lastIndex.toLong()
            val expected = if (step >= directed.size && step % 19 == 0) current - 1 else current
            val candidate = matrices.last().map { it.toMutableList() }
            for (cell in edit.left until edit.right) candidate[edit.entity][cell] = edit.value
            val changed = candidate != matrices.last()
            val counts = candidate.map { runs(it) }
            val candidateReferences = counts.sumOf { it.toLong() }
            val capExceeded = matrices.lastIndex >= limits.maxRetainedRevisions ||
                candidate.count { row -> row.any { it != null } } > limits.maxEntities ||
                counts.any { it > limits.maxIntervalsPerEntity } ||
                references + candidateReferences > limits.maxRetainedIntervalReferences
            val before = index.statistics()
            val mutation = {
                val from = (first + edit.left).toLong()
                val until = (first + edit.right).toLong()
                if (edit.value == null) index.erase(names[edit.entity], from, until, expected)
                else index.put(names[edit.entity], from, until, edit.value, expected)
            }
            when {
                expected != current -> {
                    assertThrows(StaleRevision::class.java) { mutation() }
                    rejected += 1
                    assertEquals(before, index.statistics())
                }
                !changed -> {
                    assertEquals(current, mutation())
                    noOps += 1
                    assertEquals(before, index.statistics())
                }
                capExceeded -> {
                    assertThrows(HistoryLimit::class.java) { mutation() }
                    rejected += 1
                    assertEquals(before, index.statistics())
                }
                else -> {
                    assertEquals(current + 1, mutation())
                    matrices.add(candidate.map { it.toList() })
                    references += candidateReferences
                }
            }
            assertEquals(matrices.lastIndex.toLong(), index.currentRevision)
            assertEquals(matrices.lastIndex, index.statistics().retainedRevisions)
            assertEquals(references, index.statistics().retainedIntervalReferences)
            for ((revision, matrix) in matrices.withIndex()) {
                val snapshot = index.snapshot(revision.toLong())
                assertEquals(matrix.sumOf { runs(it).toLong() }, snapshot.intervalCount)
                assertEquals(matrix.count { row -> row.any { it != null } }, snapshot.entities.size)
                for ((entityNumber, entity) in names.withIndex()) {
                    for (cell in -1..width) {
                        val value = if (cell in 0 until width) matrix[entityNumber][cell] else null
                        val expectedFact = if (value == null) FactValue.Missing else FactValue.Present(value)
                        assertEquals(
                            expectedFact,
                            index.query(snapshot, entity, (first + cell).toLong()),
                            "seed=$seed step=$step revision=$revision entity=$entity cell=$cell"
                        )
                    }
                    val intervals = snapshot.entities[entity].orEmpty()
                    for ((a, b) in intervals.zipWithNext()) {
                        assertTrue(a.until <= b.from)
                        assertFalse(a.until == b.from && a.value == b.value)
                    }
                    assertTrue(intervals.all { it.from < it.until })
                }
            }
        }
        assertTrue(noOps > 0, "directed and generated no-ops must be exercised")
        assertTrue(rejected > 0, "stale or capacity rejections must be exercised")
    }
}
