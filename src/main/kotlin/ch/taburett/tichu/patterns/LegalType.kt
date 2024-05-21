package ch.taburett.tichu.patterns

enum class LegalType {
    OK,

    /**
     * Basically a legal move but not possible due to a pending wish
     */
    WISH,

    /**
     * Move is not possible
     */
    ILLEGAL,

    /**
     * Player tries to play cards that are not owned by them
     */
    CHEATING
}