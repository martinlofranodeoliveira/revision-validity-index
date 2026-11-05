// Copyright 2026 Lofrano Tecnologia. MIT licensed.
package br.com.lofranotecnologia.revision

sealed class IndexError(message: String) : RuntimeException(message)

class InvalidInterval(val from: Long, val until: Long) :
    IndexError("Effective interval must satisfy from < until")

class InvalidEntity(reason: String) : IndexError("Invalid entity: $reason")
class InvalidValue(reason: String) : IndexError("Invalid value: $reason")

class StaleRevision(val expected: Long, val actual: Long) :
    IndexError("Expected revision $expected but current revision is $actual")

class UnknownRevision(val requested: Long, val current: Long) :
    IndexError("Revision $requested does not exist; current revision is $current")

enum class LimitResource {
    RETAINED_REVISIONS,
    ENTITIES,
    INTERVALS_PER_ENTITY,
    RETAINED_INTERVAL_REFERENCES
}

class HistoryLimit(val resource: LimitResource, val limit: Long) :
    IndexError("Mutation would exceed $resource limit $limit")
