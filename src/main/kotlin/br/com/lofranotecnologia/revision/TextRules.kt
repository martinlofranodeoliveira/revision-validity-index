// Copyright 2026 Lofrano Tecnologia. MIT licensed.
package br.com.lofranotecnologia.revision

/** Counts exact UTF-8 length without allocating an encoded copy or replacing surrogates. */
internal object TextRules {
    fun entity(text: String) = validate(text, 128, allowEmpty = false, entity = true)
    fun value(text: String) = validate(text, 4096, allowEmpty = true, entity = false)

    private fun reject(entity: Boolean, reason: String): Nothing {
        if (entity) throw InvalidEntity(reason)
        throw InvalidValue(reason)
    }

    private fun validate(text: String, maximum: Int, allowEmpty: Boolean, entity: Boolean) {
        if (!allowEmpty && text.isEmpty()) reject(entity, "must not be empty")
        // UTF-8 of well-formed text is never shorter than its UTF-16 code-unit count.
        if (text.length > maximum) reject(entity, "UTF-8 exceeds $maximum bytes")
        var bytes = 0
        var i = 0
        while (i < text.length) {
            val unit = text[i].code
            when {
                unit <= 0x7f -> bytes += 1
                unit <= 0x7ff -> bytes += 2
                unit in 0xd800..0xdbff -> {
                    if (i + 1 >= text.length || text[i + 1].code !in 0xdc00..0xdfff) {
                        reject(entity, "unpaired UTF-16 surrogate")
                    }
                    bytes += 4
                    i += 1
                }
                unit in 0xdc00..0xdfff -> reject(entity, "unpaired UTF-16 surrogate")
                else -> bytes += 3
            }
            if (bytes > maximum) reject(entity, "UTF-8 exceeds $maximum bytes")
            i += 1
        }
    }
}
