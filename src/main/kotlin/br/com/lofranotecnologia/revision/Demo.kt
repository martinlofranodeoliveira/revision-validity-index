// Copyright 2026 Lofrano Tecnologia. MIT licensed.
package br.com.lofranotecnologia.revision

object Demo {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.isEmpty()) { "The offline demo accepts no arguments" }
        val index = RevisionValidityIndex()
        check(index.put("synthetic-label", 0, 10, "blue", 0) == 1L)
        val old = index.snapshot(1)
        check(index.put("synthetic-label", 4, 7, "green", 1) == 2L)
        val corrected = index.snapshot(2)
        check(index.erase("synthetic-label", 6, 9, 2) == 3L)
        val removed = index.snapshot(3)
        check(index.query(old, "synthetic-label", 5) == FactValue.Present("blue"))
        check(index.query(corrected, "synthetic-label", 5) == FactValue.Present("green"))
        check(index.query(corrected, "synthetic-label", 6) == FactValue.Present("green"))
        check(index.query(removed, "synthetic-label", 6) == FactValue.Missing)
        check(index.query(removed, "synthetic-label", 9) == FactValue.Present("blue"))

        var blocked = false
        try {
            (old.entities as MutableMap<String, List<Interval>>).clear()
        } catch (_: UnsupportedOperationException) {
            blocked = true
        }
        check(blocked)
        check(index.query(old, "synthetic-label", 5) == FactValue.Present("blue"))
        println("DEMO_OK old=blue corrected=green removed=missing")
    }
}
