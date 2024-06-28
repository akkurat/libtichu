## Cards
### Suits
* JADE J
* SWORDS D (for sworD or Dagger)
* PAGODAS P
* STARS S
### Ranks
* MAH = 1, DRG=15, PHX = *
* 2 - 10
* 11 - 14 Joker, Queen, King, Ace

Static Values for each Card of the Tichu Deck are provided. This is usefull for testing or writing bots.

## Patterns

Each pattern has a factory companion
* pattern: Checks a given set of cards assuming there is exactly one instance of the pattern (e.g. ```Triple.pattern(listOf(S3,D3,J3))``` )
* allPatterns: Checks a given set of cards for all possible instances of this pattern type (e.g. ```Pair.pattern(listOf(S2,P2,J2,D5,P5))```)

There is a factory, to generate all patterns for a given set

## Game
Logic to play a Tichu Game, split into:
* Preparation
* Game Play
* Tricks
* Current Table
* Logic to validate moves

## Botplayer

* Battle: let play bots against each other
* Probabilities: Help to to predict the strength of a pattern
* Simulation: Emulate a round played
* Stupid Player: More or less just confirming to tichu rules
* Strategic Player: Trying to play a somewhat sensisable game
