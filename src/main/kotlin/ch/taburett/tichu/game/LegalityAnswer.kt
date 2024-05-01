package ch.taburett.tichu.game

data class LegalityAnswer(val type: LegalType, val message: String)

fun ok(): LegalityAnswer {
    return LegalityAnswer(LegalType.OK, "");
}

fun message(msg: String): LegalityAnswer {
    return LegalityAnswer(LegalType.ILLEGAL, msg)
}
