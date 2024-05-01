package ch.taburett.tichu.game

class MutableRound {

    private var state: RoundState = RoundState.BIG_TICHU
    val tricks: List<Trick> = ArrayList()

    enum class RoundState {
        BIG_TICHU, TICHU_PRE_SCHUPF, SCHUPF, TICHU_POST_SCHUPF, PLAY, END
    }

    fun getState() :RoundState {
       return state;
    }



}


