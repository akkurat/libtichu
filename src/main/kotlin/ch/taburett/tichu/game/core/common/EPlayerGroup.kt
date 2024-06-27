package ch.taburett.tichu.game.core.common

enum class EPlayerGroup {

    A {
        // inherent circular dependency hence must be function or at least lazily evaluated/**/
        override val players: List<EPlayer> by lazy { listOf(EPlayer.A1, EPlayer.A2) }

        override fun other(): EPlayerGroup = B
    },
    B {
        override val players: List<EPlayer> by lazy { listOf(EPlayer.B1, EPlayer.B2) }

        override fun other(): EPlayerGroup = A
    };

    abstract val players: List<EPlayer>
    abstract fun other(): EPlayerGroup
}