# Changelog

## 0.1.0 — Unreleased

Development version; no release or deployment claim.

### Added

- Memory-only global logical revisions with permanently queryable empty revision zero.
- Half-open signed-Long effective intervals, overwrite splitting, coalescing and gap-producing erasure.
- Atomic expected-revision checks, serialized writers and safely published immutable snapshots.
- Explicit retention, entity, interval and structural retained-reference caps; no automatic pruning.
- Bounded UTF-8 validation that rejects unpaired surrogates without replacement encoding.
- Historical regression cases, adversarial input tests, exported-view mutation tests, barrier-based writer races and an independent fixed-seed historical cell oracle.
- Deterministic synthetic label demo with an exact success line.
- English and Brazilian Portuguese usage, interval and concurrency documentation.
- Strict dependency locking/verification policy and separate offline test/demo CI gates.
