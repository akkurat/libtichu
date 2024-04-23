package ch.taburett.tichu.game

class MutableRound {

    private var state: RoundState = RoundState.SCHUPF;
    val tricks: List<Trick> = ArrayList()

    enum class RoundState {
        SCHUPF, PLAY, END
    }

    fun getState() :RoundState {
       return state;
    }

}


