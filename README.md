# Revision Validity Index

Development version **0.1.0**, by **Lofrano Tecnologia**. MIT licensed.

An in-memory Kotlin/JVM library for corrected labels and configuration facts. Effective intervals answer **when a value applies**; logical revisions answer **what the process knew after a particular accepted change**. Revisions are not timestamps, authorship evidence or durable audit records.

- Organization: https://lofranotecnologia.com.br
- Repository: https://github.com/martinlofranodeoliveira/revision-validity-index
- [Português](README.pt-BR.md)
- [Precise contract](docs/contract.md), [design](docs/design.md), [capacity accounting](docs/limits.md)

## Install and verify

Use **Gradle 8.10.2 and JDK 21**, for example the specified `gradle:8.10.2-jdk21` development image. No wrapper binary or installer is included. Kotlin JVM 2.0.21 and JUnit Jupiter 5.11.4 are pinned in the build. JUnit is test-only; runtime needs Kotlin's standard library and JDK 21, not a service, database, model or API.

Keep caches, Java user files and compiler state inside ignored `build/`:

```sh
mkdir -p build/gradle-home build/java-home build/tmp
export GRADLE_USER_HOME="$PWD/build/gradle-home"
export JAVA_TOOL_OPTIONS="-Duser.home=$PWD/build/java-home -Djava.io.tmpdir=$PWD/build/tmp -Dfile.encoding=UTF-8"
```

`gradle.lockfile` and `gradle/verification-metadata.xml` are required source integrity records. They must contain actual resolution-derived locks and SHA-256 hashes, including metadata artifacts. Missing records fail closed. For first dependency preparation, integrity review and cold-cache verification, follow [docs/integrity.md](docs/integrity.md); do not fabricate these files or disable verification.

With reviewed records, resolve dependencies once online into that Gradle home:

```sh
gradle --dependency-verification strict resolveDependencies testClasses
```

Then run the two independent offline gates:

```sh
gradle --offline test
gradle --offline --quiet demoAssert
```

The demo's stdout is exactly one line:

```text
DEMO_OK old=blue corrected=green removed=missing
```

This stable line is human-readable and can also be checked by scripts. Java may print `JAVA_TOOL_OPTIONS` information to stderr. The demo performs real historical queries and checks an exported-view mutation attempt before printing success. CI resolves using reviewed records and then runs tests and demo offline, separately. CI never writes integrity records or publishes artifacts.

For a local library JAR:

```sh
gradle --offline jar
```

The result is `build/libs/revision-validity-index-0.1.0.jar`; it is not a fat JAR. Supply Kotlin stdlib 2.0.21 on an embedding application's runtime classpath. No artifact publication is configured.

## Small example

```kotlin
import br.com.lofranotecnologia.revision.*

val index = RevisionValidityIndex()
val r1 = index.put("synthetic-label", 0, 10, "blue", 0)
val beforeCorrection = index.snapshot(r1)
val r2 = index.put("synthetic-label", 4, 7, "green", r1)
check(index.query(beforeCorrection, "synthetic-label", 5) == FactValue.Present("blue"))
check(index.query(index.snapshot(r2), "synthetic-label", 5) == FactValue.Present("green"))
val r3 = index.erase("synthetic-label", 6, 9, r2)
check(index.query(index.snapshot(r3), "synthetic-label", 6) == FactValue.Missing)
```

## Contract at a glance

- Revision 0 is permanently queryable and empty. Only changing revisions 1..N count against retention.
- Effective time uses caller-supplied signed `Long` units and `[from, until)`. There is no date inference.
- Put overwrites, splits and coalesces adjacent equal strings. Erase creates absence, never a null fact.
- A valid no-op checks `expectedRevision` first and consumes no revision or references, even at full capacity.
- All writers use one `ReentrantLock`. The expected-revision check and publication are atomic. Competing changing writes with the same expectation cannot both commit.
- Snapshot retrieval reads one volatile immutable history. Queries on held snapshots take no writer lock.
- Exported maps and lists have defensive copies and Java unmodifiable wrappers, not merely Kotlin read-only types.
- Default caps: 128 changing revisions, 1024 active entities per snapshot, 256 intervals per entity and 65536 retained interval references. No automatic pruning.
- IDs are nonempty well-formed Unicode, at most 128 UTF-8 bytes. Values are well-formed Unicode, at most 4096 UTF-8 bytes; the empty value is a fact. Strings are immutable and not normalized.

## Why this design?

A bounded in-memory index is useful when an application needs historical as-known queries without introducing storage infrastructure. Canonical intervals make gaps explicit and compress adjacent equal facts. Whole immutable snapshots and explicit optimistic concurrency favor a small, inspectable contract over clever mutation or lock-free writers. The [design notes](docs/design.md) discuss copying costs and linearization points.

Regression tests cover historical boundaries, no-ops, cap equality and rollback, deep immutability, Unicode and extreme `Long` values. Barrier-based races do not use sleeps as an oracle. A fixed-seed independent per-cell/per-revision matrix checks generated edits and rejections.

## Limits and exclusions

All state disappears on process exit. Reaching a retention/reference cap blocks further changing writes, including erasure when it would require another retained snapshot. Logical caps are not exact heap limits. Copying occurs under the writer lock; no throughput or production-readiness claim is made.

This project does not implement authentication, authorization, durable audit storage, financial or clinical record handling, distributed replication, timezone rules, automatic pruning, a parser or a network service. It claims only the documented interval contract, not broad temporal-database standards compliance.

For security reports use private reporting when available as described in [SECURITY.md](SECURITY.md). For general contact use the [official homepage](https://lofranotecnologia.com.br).
