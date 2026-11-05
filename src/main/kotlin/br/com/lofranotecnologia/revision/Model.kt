// Copyright 2026 Lofrano Tecnologia. MIT licensed.
package br.com.lofranotecnologia.revision

/** Configured logical caps, not a measurement of JVM heap consumption. */
data class Limits(
    val maxRetainedRevisions: Int = 128,
    val maxEntities: Int = 1024,
    val maxIntervalsPerEntity: Int = 256,
    val maxRetainedIntervalReferences: Long = 65_536L
) {
    init {
        require(maxRetainedRevisions >= 0 && maxRetainedRevisions < Int.MAX_VALUE)
        require(maxEntities >= 0)
        require(maxIntervalsPerEntity >= 0)
        require(maxRetainedIntervalReferences >= 0)
    }
}

/** A canonical, half-open interval exported by a snapshot. */
@ConsistentCopyVisibility
data class Interval internal constructor(
    val from: Long,
    val until: Long,
    val value: String
)

sealed interface FactValue {
    data class Present(val value: String) : FactValue
    data object Missing : FactValue
}

/** All fields come from one published history state. */
data class IndexStatistics(
    val currentRevision: Long,
    val retainedRevisions: Int,
    val currentEntities: Int,
    val currentIntervals: Long,
    val retainedIntervalReferences: Long
)
