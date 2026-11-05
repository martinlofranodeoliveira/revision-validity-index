// Copyright 2026 Lofrano Tecnologia. MIT licensed.
package br.com.lofranotecnologia.revision

import java.util.Collections
import java.util.TreeMap

/** Self-contained immutable view. Its exported map AND every nested list are unmodifiable. */
class Snapshot internal constructor(
    val revision: Long,
    source: Map<String, List<Interval>>
) {
    val entities: Map<String, List<Interval>>
    val intervalCount: Long

    init {
        val copy = TreeMap<String, List<Interval>>()
        var count = 0L
        for ((entity, intervals) in source) {
            if (intervals.isNotEmpty()) {
                copy[entity] = Collections.unmodifiableList(ArrayList(intervals))
                count += intervals.size.toLong()
            }
        }
        entities = Collections.unmodifiableMap(copy)
        intervalCount = count
    }

    internal fun lookup(entity: String, validAt: Long): FactValue {
        val intervals = entities[entity] ?: return FactValue.Missing
        var low = 0
        var high = intervals.lastIndex
        while (low <= high) {
            val middle = low + (high - low) / 2
            val interval = intervals[middle]
            when {
                validAt < interval.from -> high = middle - 1
                validAt >= interval.until -> low = middle + 1
                else -> return FactValue.Present(interval.value)
            }
        }
        return FactValue.Missing
    }
}
