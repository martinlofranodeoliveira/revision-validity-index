# Embedding examples — development 0.1.0

These snippets use synthetic labels only. They are Kotlin snippets for an application with this library and Kotlin stdlib on its classpath.

## Optimistic correction

```kotlin
import br.com.lofranotecnologia.revision.*

val index = RevisionValidityIndex()
val observed = index.currentRevision
try {
    val accepted = index.put("synthetic-device", -20, 20, "amber", observed)
    println("accepted=$accepted")
} catch (error: StaleRevision) {
    // Re-read relevant facts and decide whether the correction is still appropriate.
    println("conflict current=${error.actual}")
} catch (error: HistoryLimit) {
    println("capacity resource=${error.resource}")
}
```

Without competing writers, this prints:

```text
accepted=1
```

Do not blindly retry a semantic correction after a conflict. Another writer may have changed the facts on which the decision depended.

## Explicit absence and empty value

```kotlin
val index = RevisionValidityIndex()
index.put("synthetic-flag", 0, 2, "", 0)
val view = index.snapshot(1)
check(index.query(view, "synthetic-flag", 0) == FactValue.Present(""))
check(index.query(view, "synthetic-flag", 2) == FactValue.Missing)
```

An empty string is an opaque value, not a tombstone. `erase` is the only removal operation.

## Inspect canonical intervals

```kotlin
val index = RevisionValidityIndex()
index.put("synthetic-label", 0, 4, "blue", 0)
index.put("synthetic-label", 4, 10, "blue", 1)
for (interval in index.snapshot(2).entities.getValue("synthetic-label")) {
    println("[${interval.from},${interval.until})=${interval.value}")
}
```

Output:

```text
[0,10)=blue
```

The returned collections reject mutation. Copy them into application-owned collections if an editable projection is needed; editing that projection cannot update the index.

## Português

Uma revisão esperada é uma condição otimista sobre o estado global, não um bloqueio mantido pelo chamador. Em caso de `StaleRevision`, releia os fatos e reavalie a decisão antes de tentar novamente. `Present("")` continua sendo um fato; ausência é `Missing`. As coleções exportadas não são um canal de edição: faça uma cópia própria para projeções mutáveis e use `put` ou `erase` para alterações no índice.
