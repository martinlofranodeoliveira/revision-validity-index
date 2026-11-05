# Public contract — development 0.1.0

Package: `br.com.lofranotecnologia.revision`. Author organization: Lofrano Tecnologia. This is a memory-only library, not durable audit storage.

## Types and calls

```kotlin
RevisionValidityIndex(limits: Limits = Limits())
put(entity: String, from: Long, until: Long, value: String, expectedRevision: Long): Long
erase(entity: String, from: Long, until: Long, expectedRevision: Long): Long
snapshot(revision: Long): Snapshot
query(snapshot: Snapshot, entity: String, validAt: Long): FactValue
```

`currentRevision: Long` and `statistics(): IndexStatistics` expose read-only state. Each statistics result comes from one published history state. Separate read calls need not describe the same instant.

`Snapshot` exposes `revision: Long`, `entities: Map<String, List<Interval>>` and `intervalCount: Long`. Entity keys use natural Java/Kotlin String ordering; intervals use increasing `from`. The exported map, its entries/views and all nested lists cannot change snapshot contents. `Interval` has immutable `from`, `until` and `value` properties. A copied interval is detached data, not an update handle.

`FactValue.Present(value)` and `FactValue.Missing` distinguish a fact, including an empty string, from absence. There is no null-valued fact. All public String and Snapshot parameters are non-null Kotlin types; Java callers must also supply non-null arguments.

Snapshots are self-contained, not owner-bound. Passing one index's snapshot to another index's `query` is allowed and queries that snapshot alone. Snapshot construction is internal, not a persistence/import format.

## Effective coordinates

`from`, `until` and `validAt` are signed 64-bit integers supplied directly as JVM `Long` values. There is no text parser or narrowing conversion. Mutations require `from < until`. Intervals include `from` and exclude `until`.

No endpoint subtraction or duration arithmetic is performed. Both signed extremes are accepted. `[Long.MIN_VALUE, Long.MAX_VALUE)` excludes `Long.MAX_VALUE`; no representable half-open interval contains that final coordinate. Queries there consequently return Missing.

## Text

- Entity: nonempty well-formed Unicode, at most 128 bytes in UTF-8.
- Value: well-formed Unicode, at most 4096 bytes in UTF-8; empty is permitted.
- Unpaired UTF-16 surrogates are rejected rather than encoded with replacement.
- No trimming, case folding or Unicode normalization. Control characters, including NUL, are permitted opaque content.
- For well-formed strings, equality is exact UTF-8 byte equality, equivalently exact JVM String content equality.
- Inputs are size-checked before any copying/encoding. Oversized input is rejected using bounded inspection. Strings are immutable; mutable caller buffers are not accepted or retained.

Entity validation also applies to query. The library's error messages do not echo entity/value payloads.

## Revisions and mutation

Revision 0 exists initially, is empty and remains queryable. Every effective global state change allocates exactly the next revision, including a change that makes the current snapshot empty. There are no skipped, per-entity or wall-clock revisions.

Put overwrites the selected range, splitting intersected intervals as needed and coalescing touching equal values. Erase removes facts only within the selected range and creates gaps. Empty entities are removed from the candidate snapshot.

After valid input, a writer atomically checks `expectedRevision` against the current global revision. Any mismatch, including a negative expectation, throws `StaleRevision`. This check precedes no-op detection. An identical resulting state returns the current revision without consuming a revision, snapshot or reference budget, even when a cap is full. Erasing an absent range is such a no-op.

Changing writes with the same expectation cannot both commit. This does not exclude two no-ops both returning the same revision. Writers are serialized by one `ReentrantLock`; there are no caller callbacks under it. Queries on held immutable snapshots acquire no writer lock. Snapshot retrieval uses safe volatile publication and no writer lock.

## Caps and atomic rejection

Defaults:

| Limit | Default | Meaning |
| --- | ---: | --- |
| maxRetainedRevisions | 128 | Changing revisions 1..N, excluding zero |
| maxEntities | 1024 | Nonempty entity keys in each snapshot, not union over history |
| maxIntervalsPerEntity | 256 | Canonical interval entries for one entity in each snapshot |
| maxRetainedIntervalReferences | 65536 | Sum of interval entries over every retained changing snapshot |

Zero caps are supported. Configuration requires nonnegative values; revision cap must also be less than `Int.MAX_VALUE` so zero plus all retained snapshots fit the indexing representation. Reference cap is a nonnegative `Long`. Other two caps are nonnegative `Int` values. Invalid configuration throws `IllegalArgumentException`.

The current snapshot is included exactly once in retained reference accounting. Revision zero contributes zero. Shared interval object identities do not reduce the count. Each accepted empty snapshot costs one revision and zero new references. No-op and rejected calls cost neither. Counters are checked without overflowing `Long` addition. See [limits.md](limits.md) for examples.

After canonicalization, every candidate cap is checked before publication. On `HistoryLimit`, the revision, all snapshots, counters and historical query answers are unchanged. There is no automatic pruning, eviction or reset operation. Even erasure can fail a retention/reference cap. Logical caps do not promise an exact process-memory ceiling or recovery from JVM fatal errors.

## Errors and ordering

All explicit operation errors extend `IndexError`:

| Error | Meaning |
| --- | --- |
| InvalidInterval(from, until) | Mutation has from >= until |
| InvalidEntity | Empty, oversized or malformed entity |
| InvalidValue | Oversized or malformed value |
| StaleRevision(expected, actual) | Atomic expected-revision comparison failed |
| UnknownRevision(requested, current) | Snapshot revision is negative or not yet created |
| HistoryLimit(resource, limit) | A changing candidate exceeds a configured cap |

Mutation validation order is interval, entity, then value for put; erase has no value. Validation precedes stale checking. With valid input, stale checking precedes no-op comparison; no-op comparison precedes capacity checks. If multiple caps fail, the implementation reports revision, interval, entity, then reference cap. Applications should not depend on which cap wins when several fail.

## Contrato resumido em português

A revisão 0 é vazia e permanente. Apenas revisões com alteração, de 1 até N, contam para retenção. Uma operação válida sem mudança verifica primeiro a revisão esperada e não gasta histórico nem referências, inclusive quando os limites estão cheios.

As faixas são `[from, until)` sobre `Long`, com `from < until`. Não há subtração de extremos, incremento de endpoint nem inferência de data. `Long.MAX_VALUE` não pertence a nenhuma faixa semiaberta representável. `put` substitui, divide e une vizinhos iguais; `erase` cria lacuna. Entidades vazias desaparecem do snapshot atual, sem apagar o histórico.

IDs são não vazios e limitados a 128 bytes UTF-8; valores podem ser vazios e têm limite de 4096 bytes. Surrogates isolados são inválidos. Strings não são normalizadas. Ausência é `FactValue.Missing`, separada de `Present("")`.

O escritor verifica a expectativa e publica sob um único lock. Consultas com snapshots imutáveis não precisam desse lock. Mapas e listas exportados têm cópias defensivas e bloqueiam alterações reais. A contagem de referências soma as entradas de cada snapshot retido, incluindo o atual uma vez, sem deduplicação por identidade de objeto. Toda rejeição por limite preserva revisões, contadores e respostas históricas. Não há poda automática nem persistência.
