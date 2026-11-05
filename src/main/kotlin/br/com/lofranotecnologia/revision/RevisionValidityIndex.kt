// Copyright 2026 Lofrano Tecnologia. MIT licensed.
package br.com.lofranotecnologia.revision

import java.util.Collections
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class RevisionValidityIndex(val limits: Limits = Limits()) {
    private class History(val snapshots: List<Snapshot>, val references: Long) {
        val latest: Snapshot = snapshots.last()
    }

    private val writer = ReentrantLock()

    @Volatile
    private var history = History(Collections.singletonList(Snapshot(0L, emptyMap())), 0L)

    val currentRevision: Long
        get() = history.latest.revision

    fun statistics(): IndexStatistics {
        val observed = history
        return IndexStatistics(
            observed.latest.revision,
            observed.snapshots.size - 1,
            observed.latest.entities.size,
            observed.latest.intervalCount,
            observed.references
        )
    }

    fun snapshot(revision: Long): Snapshot {
        val observed = history
        if (revision < 0L || revision > observed.latest.revision) {
            throw UnknownRevision(revision, observed.latest.revision)
        }
        return observed.snapshots[revision.toInt()]
    }

    /** No writer lock and no access to mutable index state. Foreign snapshots are allowed. */
    fun query(snapshot: Snapshot, entity: String, validAt: Long): FactValue {
        TextRules.entity(entity)
        return snapshot.lookup(entity, validAt)
    }

    fun put(entity: String, from: Long, until: Long, value: String, expectedRevision: Long): Long {
        validateInterval(from, until)
        TextRules.entity(entity)
        TextRules.value(value)
        return mutate(entity, from, until, value, expectedRevision)
    }

    fun erase(entity: String, from: Long, until: Long, expectedRevision: Long): Long {
        validateInterval(from, until)
        TextRules.entity(entity)
        return mutate(entity, from, until, null, expectedRevision)
    }

    private fun validateInterval(from: Long, until: Long) {
        if (from >= until) throw InvalidInterval(from, until)
    }

    private fun mutate(
        entity: String,
        from: Long,
        until: Long,
        replacement: String?,
        expectedRevision: Long
    ): Long = writer.withLock {
        val before = history
        val current = before.latest
        if (expectedRevision != current.revision) {
            throw StaleRevision(expectedRevision, current.revision)
        }

        val oldIntervals = current.entities[entity] ?: emptyList()
        val candidate = Timeline.rewrite(oldIntervals, from, until, replacement)
        if (candidate == oldIntervals) return@withLock current.revision

        if (before.snapshots.size - 1 >= limits.maxRetainedRevisions) {
            throw HistoryLimit(LimitResource.RETAINED_REVISIONS, limits.maxRetainedRevisions.toLong())
        }
        if (candidate.size > limits.maxIntervalsPerEntity) {
            throw HistoryLimit(LimitResource.INTERVALS_PER_ENTITY, limits.maxIntervalsPerEntity.toLong())
        }
        val entityCount = current.entities.size.toLong() -
            (if (oldIntervals.isEmpty()) 0L else 1L) +
            (if (candidate.isEmpty()) 0L else 1L)
        if (entityCount > limits.maxEntities.toLong()) {
            throw HistoryLimit(LimitResource.ENTITIES, limits.maxEntities.toLong())
        }
        val candidateReferences = current.intervalCount - oldIntervals.size.toLong() + candidate.size.toLong()
        // Subtract a nonnegative counter from its cap BEFORE adding: cannot overflow Long.
        if (candidateReferences > limits.maxRetainedIntervalReferences - before.references) {
            throw HistoryLimit(
                LimitResource.RETAINED_INTERVAL_REFERENCES,
                limits.maxRetainedIntervalReferences
            )
        }

        val nextEntities = HashMap(current.entities)
        if (candidate.isEmpty()) nextEntities.remove(entity) else nextEntities[entity] = candidate
        val next = Snapshot(current.revision + 1L, nextEntities)
        val snapshots = ArrayList(before.snapshots)
        snapshots.add(next)
        val committed = History(
            Collections.unmodifiableList(snapshots),
            before.references + candidateReferences
        )
        history = committed
        next.revision
    }
}
