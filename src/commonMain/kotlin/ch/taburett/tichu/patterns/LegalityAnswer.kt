@file:JsExport()
package ch.taburett.tichu.patterns

import kotlin.js.JsExport

data class LegalityAnswer(val type: LegalType, val message: String)

fun ok(): LegalityAnswer {
    return LegalityAnswer(LegalType.OK, "");
}

fun message(msg: String): LegalityAnswer {
    return LegalityAnswer(LegalType.ILLEGAL, msg)
}
