# Capacity accounting / Contagem de capacidade

The counters describe retained structure, not bytes allocated by the JVM.

For retained changing snapshots S1 through SN:

```text
retainedRevisions = N
intervals(S) = sum of the sizes of all nonempty entity interval lists in S
retainedIntervalReferences = intervals(S1) + ... + intervals(SN)
```

The initial S0 is empty, remains queryable and does not count as a changing revision. Current SN appears once, not twice. Sharing an immutable Interval object does not reduce the total. Every snapshot's lists have their own structural entries.

## Example: unchanged facts still count

With a reference cap of 3:

| Operation | New snapshot entries | Total retained references |
| --- | ---: | ---: |
| put a [0,1) = A, revision 1 | 1 | 1 |
| put b [0,1) = B, revision 2 | 2 | 3 |
| erase b [0,1), attempted revision 3 | 1 | would be 4: reject |

Revision 2 still contains b after that rejection. Repeating a's existing value with expectation 2 is a no-op and remains accepted without consuming references.

## Example: empty entity cleanup

With one active entity allowed:

1. Put a: revision 1 contains a.
2. Erase all of a: revision 2 is empty and adds zero references, but costs one changing revision.
3. Put b: revision 3 contains b. The active entity cap is satisfied even though a is still queryable in revision 1.

Distinct entity IDs across all history need not fit the active entity cap. Retained intervals, revision count and bounded text still bound their retained structure. The implementation never keeps an empty entity key.

## Equality and overflow

A candidate equal to a cap is accepted. The interval cap is tested after coalescing, so joining two same-valued neighbors can reduce the count. Rejections do not consume a revision number. A no-op checks the expected revision before bypassing the cap checks.

The next snapshot's interval count is calculated using nonnegative `Long` counts. Before adding it to the retained total, the implementation compares it with `cap - retainedTotal`. Since the retained total is already within its cap, that subtraction is safe and prevents overflow of the later addition. Endpoint arithmetic is not involved.

Zero caps permit valid no-ops on the empty index but reject the first change. Large custom caps can exhaust a process before reaching a logical cap; there is no exact heap accounting, string interning policy or automatic pruning. Choose limits for the embedding process. Creating a separate index starts new memory-only history; it is not a durable continuation of an old one.

## Português

A contagem soma as entradas de intervalos de cada revisão com alteração, incluindo o snapshot atual uma única vez. Não se deduplicam objetos compartilhados. A revisão 0 não conta. Um snapshot vazio custa uma revisão e zero referências novas. Entidades vazias são removidas; o limite de entidades mede as chaves ativas de cada snapshot, não a união de todo o histórico.

A igualdade com o limite é aceita. Todas as verificações ocorrem antes da publicação. Nenhuma rejeição altera revisão, histórico ou contadores. O limite não é uma medição de heap; não há poda automática. Até uma remoção pode ser bloqueada porque precisa preservar outro snapshot.
