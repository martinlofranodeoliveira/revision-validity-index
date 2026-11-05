// Copyright 2026 Lofrano Tecnologia. MIT licensed.
package br.com.lofranotecnologia.revision

import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

class ConcurrentWritersTest {
    @Test
    @Timeout(15)
    fun `barrier released changing writers cannot both commit the same expectation`() {
        repeat(12) {
            val index = RevisionValidityIndex()
            val barrier = CyclicBarrier(3)
            val pool = Executors.newFixedThreadPool(2)
            try {
                val futures = listOf("a", "b").map { label ->
                    pool.submit(Callable {
                        barrier.await(5, TimeUnit.SECONDS)
                        try {
                            "ok:${index.put(label, 0, 1, label, 0)}"
                        } catch (error: StaleRevision) {
                            "stale:${error.actual}"
                        }
                    })
                }
                barrier.await(5, TimeUnit.SECONDS)
                assertEquals(listOf("ok:1", "stale:1"), futures.map { it.get(5, TimeUnit.SECONDS) }.sorted())
                val snapshot = index.snapshot(1)
                assertEquals(1, snapshot.entities.size)
                assertEquals(1L, index.statistics().retainedIntervalReferences)
                assertTrue(index.snapshot(0).entities.isEmpty())
            } finally {
                pool.shutdownNow()
                assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS))
            }
        }
    }

    @Test
    @Timeout(15)
    fun `barrier released put and erase competing on the same revision preserve history`() {
        repeat(12) {
            val index = RevisionValidityIndex()
            assertEquals(1L, index.put("x", 0, 10, "blue", 0))
            val rev1 = index.snapshot(1)
            val barrier = CyclicBarrier(3)
            val pool = Executors.newFixedThreadPool(2)
            try {
                val putFuture = pool.submit(Callable {
                    barrier.await(5, TimeUnit.SECONDS)
                    try {
                        "put:${index.put("x", 4, 7, "green", 1)}"
                    } catch (error: StaleRevision) {
                        "stale:${error.expected}:${error.actual}"
                    }
                })
                val eraseFuture = pool.submit(Callable {
                    barrier.await(5, TimeUnit.SECONDS)
                    try {
                        "erase:${index.erase("x", 2, 8, 1)}"
                    } catch (error: StaleRevision) {
                        "stale:${error.expected}:${error.actual}"
                    }
                })
                barrier.await(5, TimeUnit.SECONDS)
                val outcomes = listOf(putFuture.get(5, TimeUnit.SECONDS), eraseFuture.get(5, TimeUnit.SECONDS)).sorted()
                assertTrue(
                    outcomes == listOf("put:2", "stale:1:2") || outcomes == listOf("erase:2", "stale:1:2"),
                    "Unexpected race outcomes: $outcomes"
                )
                assertEquals(2L, index.currentRevision)
                assertEquals(FactValue.Present("blue"), index.query(rev1, "x", 5))
                val rev2 = index.snapshot(2)
                if (outcomes.contains("put:2")) {
                    assertEquals(FactValue.Present("green"), index.query(rev2, "x", 5))
                } else {
                    assertEquals(FactValue.Missing, index.query(rev2, "x", 5))
                }
            } finally {
                pool.shutdownNow()
                assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS))
            }
        }
    }

    @Test
    @Timeout(15)
    fun `held snapshots and concurrently retrieved histories stay consistent`() {
        val index = RevisionValidityIndex()
        index.put("x", 0, 1, "1", 0)
        val held = index.snapshot(1)
        val start = CyclicBarrier(2)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val writer = pool.submit(Callable {
                start.await(5, TimeUnit.SECONDS)
                for (revision in 2L..40L) {
                    assertEquals(revision, index.put("x", 0, 1, revision.toString(), revision - 1))
                }
            })
            val reader = pool.submit(Callable {
                start.await(5, TimeUnit.SECONDS)
                repeat(2000) {
                    assertEquals(FactValue.Present("1"), index.query(held, "x", 0))
                    val revision = index.currentRevision
                    val observed = index.snapshot(revision)
                    assertEquals(revision, observed.revision)
                    assertEquals(FactValue.Present(revision.toString()), index.query(observed, "x", 0))
                }
            })
            writer.get(5, TimeUnit.SECONDS)
            reader.get(5, TimeUnit.SECONDS)
            assertEquals(40L, index.currentRevision)
        } finally {
            pool.shutdownNow()
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS))
        }
    }
}
