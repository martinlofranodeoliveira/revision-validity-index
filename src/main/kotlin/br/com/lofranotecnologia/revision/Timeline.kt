// Copyright 2026 Lofrano Tecnologia. MIT licensed.
package br.com.lofranotecnologia.revision

internal object Timeline {
    /** Input is sorted, disjoint and coalesced. Null replacement denotes erasure only here. */
    fun rewrite(
        previous: List<Interval>,
        from: Long,
        until: Long,
        replacement: String?
    ): List<Interval> {
        val result = ArrayList<Interval>()

        fun append(interval: Interval) {
            val tail = result.lastOrNull()
            if (tail != null && tail.until == interval.from && tail.value == interval.value) {
                result[result.lastIndex] = Interval(tail.from, interval.until, tail.value)
            } else {
                result.add(interval)
            }
        }

        var inserted = false
        fun insertReplacement() {
            if (!inserted) {
                if (replacement != null) append(Interval(from, until, replacement))
                inserted = true
            }
        }

        for (interval in previous) {
            when {
                interval.until <= from -> append(interval)
                interval.from >= until -> {
                    insertReplacement()
                    append(interval)
                }
                else -> {
                    if (interval.from < from) {
                        append(Interval(interval.from, from, interval.value))
                    }
                    insertReplacement()
                    if (interval.until > until) {
                        append(Interval(until, interval.until, interval.value))
                    }
                }
            }
        }
        insertReplacement()
        return result
    }
}
