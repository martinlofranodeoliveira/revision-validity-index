# Design / Projeto — development 0.1.0

## Two independent dimensions

Effective coordinates are signed `Long` units supplied by the caller. Knowledge revisions are a contiguous logical sequence starting at zero. Neither dimension establishes a real-world date. An overwrite at revision 2 may correct the same effective coordinates already recorded at revision 1; both answers remain available.

Each snapshot contains a sorted entity map. Each entity has a sorted, non-overlapping list of half-open intervals, with adjacent equal strings coalesced. Missing time is represented by gaps. Empty entities are omitted. Binary search answers a query without consulting the current index state.

## Mutation algorithm

1. Validate interval and bounded text before acquiring the writer lock. Exact UTF-8 length is counted over UTF-16 without allocating encoded input. Unpaired surrogates are rejected.
2. Acquire one `ReentrantLock` and capture the current immutable history.
3. Compare `expectedRevision`. A stale operation cannot qualify as a no-op.
4. Walk only the selected entity's intervals, preserving unaffected intervals and at most two overlap fragments. Insert the replacement, or insert nothing for erase. Append operations coalesce touching equal values.
5. Compare canonical lists for a no-op. If equal, return without publishing anything.
6. Check every candidate cap. Count the whole candidate snapshot, not merely its changes.
7. Defensively copy the candidate map and nested lists; build a new immutable history list. Publish one volatile history reference.

All potentially failing candidate construction occurs before publication. There are no user callbacks, I/O or external services in the critical section. JVM fatal resource exhaustion is not a recoverability guarantee.

The effective-coordinate algorithm uses comparisons only: it never subtracts endpoints, measures duration or increments an endpoint. A range from `Long.MIN_VALUE` to `Long.MAX_VALUE` is therefore valid, although its exclusive upper endpoint still excludes `Long.MAX_VALUE` itself.

## Concurrency and publication

A changing writer linearizes at volatile history publication. A no-op or stale rejection linearizes at its decision under the lock. Snapshot retrieval and statistics capture one volatile history reference. A successful retrieval returns a fully constructed immutable object; later writes never modify it. A query on a held snapshot does not read the history and does not acquire the writer lock.

This is not a multi-operation transaction API. Reading `currentRevision`, then requesting that snapshot is safe because history is never pruned. Using the revision as an optimistic write expectation may still yield `StaleRevision`; applications decide whether to recompute and retry. No fairness or latency bound is promised.

Kotlin read-only collections alone are insufficient because Java or casts can expose mutation methods. Snapshot construction copies every collection, then wraps both map and nested lists with `Collections.unmodifiable*`. Intervals contain only final primitives and immutable `String` references. Strings need no mutable-buffer copy; no caller-owned mutable character storage is retained. The private history list is also unmodifiable.

## Cost and bounds

With E entities, K intervals for the edited entity, S interval entries in the new snapshot and R retained changing revisions:

- query: O(log E + log K), plus bounded ID validation;
- rewrite/no-op comparison: O(K), with at most K+2 candidate intervals;
- changing commit: O(S + E log E + R) collection work;
- retained structure: bounded by configured revision, entity, interval and reference caps, plus collection/string overhead.

Interval objects and strings may be shared safely, but accounting deliberately does not deduplicate them. Copying all nested lists trades memory and lock duration for a straightforward ownership model. Defaults are conservative logical limits, not a heap reservation or benchmark.

## Decisões e concorrência em português

O tempo efetivo usa unidades inteiras fornecidas pelo chamador. A revisão é uma sequência lógica global, não um relógio. Um snapshot guarda intervalos ordenados e disjuntos; lacunas representam ausência. Vizinhos iguais são unidos, e entidades sem intervalos são removidas.

A validação de texto e faixa ocorre antes do lock. Dentro de um único `ReentrantLock`, o escritor verifica a revisão esperada, constrói a faixa candidata, detecta operações sem mudança e verifica todos os limites antes de publicar. A publicação troca uma referência volátil para um histórico completamente construído. Não há callbacks do usuário nem I/O na seção crítica.

Uma consulta com snapshot já obtido não usa o lock do escritor. O snapshot é independente do estado atual, e revisões anteriores nunca são alteradas. Duas escritas que mudam o estado e esperam a mesma revisão não podem ambas ser aceitas. Não há promessa de justiça do lock nem de tempo de resposta.

As coleções são copiadas e protegidas contra alterações, não apenas declaradas como interfaces Kotlin de leitura. Valores `String` são imutáveis; texto malformado é rejeitado. A contagem de referências considera cada entrada em cada revisão retida, mesmo quando objetos são compartilhados.

## References

These are design references, not copied implementations or compliance claims:

- Kotlin collections: https://kotlinlang.org/docs/collections-overview.html
- JDK 21 ReentrantLock: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/locks/ReentrantLock.html
- Gradle 8.10.2 dependency locking: https://docs.gradle.org/8.10.2/userguide/dependency_locking.html
