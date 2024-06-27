package ch.taburett.tichu.game.core.common

enum class EPlayer(val value: String, val playerGroup: EPlayerGroup) {
    A1("A1", EPlayerGroup.A),
    B1("B1", EPlayerGroup.B),
    A2("A2", EPlayerGroup.A),
    B2("B2", EPlayerGroup.B);

    val next: EPlayer
        get() {
            return EPlayer.entries[(this.ordinal + 3) % 4]
        }

    val li: EPlayer
        get() {
            return EPlayer.entries[(this.ordinal + 3) % 4]
        }

    val partner: EPlayer
        get() {
            return EPlayer.entries[(this.ordinal + 2) % 4]
        }

    val re: EPlayer
        get() {
            return EPlayer.entries[(this.ordinal + 1) % 4]
        }

}

val playerList = EPlayer.entries.toList()