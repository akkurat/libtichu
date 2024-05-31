package ch.taburett.tichu.game;

import ch.taburett.tichu.cards.HandCard;
import ch.taburett.tichu.cards.NumberCard;
import ch.taburett.tichu.cards.Phoenix;
import ch.taburett.tichu.cards.PlayCard;
import ch.taburett.tichu.game.protocol.*;
import ch.taburett.tichu.patterns.LegalType;
import ch.taburett.tichu.patterns.Single;
import ch.taburett.tichu.patterns.TichuPattern;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ch.taburett.tichu.cards.CardUtilsKt.pattern;
import static ch.taburett.tichu.cards.CardsKt.*;
import static ch.taburett.tichu.cards.CardsKt.PHX;

public class StupidPlayer {

public static void extracted(ServerMessage message, Consumer<PlayerMessage> listener, Player player) {
        switch (message) {
            case AckGameStage ack -> {
                switch (ack.getStage()) {
                    case EIGHT_CARDS -> listener.accept(new Ack.BigTichu());
                    case PRE_SCHUPF -> listener.accept(new Ack.TichuBeforeSchupf());
                    case SCHUPF -> {
                        var cards = ack.getCards();
                        listener.accept(new Schupf(cards.get(0), cards.get(1), cards.get(2)));
                    }
                    case POST_SCHUPF -> listener.accept(new Ack.TichuBeforePlay());
                }
            }
            case Schupf schupf -> listener.accept(new Ack.SchupfcardReceived());
            case WhosMove mm -> {
                if (mm.getStage() == Stage.GIFT_DRAGON) {
                    listener.accept(new GiftDragon(GiftDragon.ReLi.LI));
                } else if (mm.getStage() == Stage.YOURTURN) {
                    var table = mm.getTable();
                    List<HandCard> handcards = mm.getHandcards();
                    if (table.isEmpty()) {
                        if (mm.mightFullFillWish()) {
                            var numberCards = mm.getHandcards().stream()
                                    .filter(hc -> hc instanceof NumberCard)
                                    .map(hc -> (NumberCard) hc)
                                    .filter(nc -> Objects.equals(mm.getWish(), ((NumberCard) nc).getValue()))
                                    .findAny().map(List::of)
                                    .orElse(List.of());
                            listener.accept(new Move(numberCards));
                            return;
                        }
                        // play smallest card
                        var ocard = handcards.stream()
                                .min(Comparator.comparingDouble(HandCard::getSort))
                                .orElse(null);

                        var values = handcards.stream()
                                .filter(h -> h instanceof NumberCard)
                                .map(h -> ((NumberCard) h).getValue())
                                .collect(Collectors.toSet());

                        var rw = IntStream.range(2, 15).filter(n -> !values.contains(n)).toArray();
                        Integer randomWish = rw.length > 0 ? rw[ThreadLocalRandom.current().nextInt(rw.length)] : null;

                        Move move;
                        if (ocard instanceof Phoenix sc) {
                            move = new Move(List.of(sc.asPlayCard(1)));
                        } else if (ocard == MAH) {
                            move = new Move(List.of(MAH), randomWish);
                        } else if (ocard instanceof PlayCard pc) {
                            move = new Move(List.of(pc));
                        } else {
                            move = new Move(List.of());
                        }

                        listener.accept(move);

                    } else {
                        PlayLogEntry toBeat = table.toBeat();
                        var pat = pattern(toBeat.getCards());
                        var all = pat.findBeatingPatterns(handcards);

                        if (mm.getWish() != null) {
                            var allWithWish = all.stream()
                                    .filter(p -> p.getCards().stream().anyMatch(c -> c.getValue() - mm.getWish() == 0))
                                    .toList();
                            if( !allWithWish.isEmpty() ) {
                                all = allWithWish;
                            }
                        }

                        if (pat instanceof Single si ) {
                            if (handcards.contains(DRG)) {
                                all.add(new Single(DRG));
                            }
                            if (handcards.contains(PHX)) {
                                all.add(new Single(PHX.asPlayCard(si.getCard().getValue() + 1)));
                            }
                        }
                        if (all.isEmpty()) {
                            listener.accept(new Move(List.of()));
                        } else {
                            var mypat = all.stream()
                                    .filter(p -> p.beats(pat).getType() == LegalType.OK)
                                    .min(Comparator.comparingDouble(TichuPattern::rank));
                            if (mypat.isPresent()) {
                                var prPat = mypat.get();
                                if (
                                        toBeat.getPlayer().getPlayerGroup() != player.getPlayerGroup() ||
                                                pat.rank() < 10
                                ) {
                                    listener.accept(new Move(mypat.get().getCards()));
                                } else {
                                    listener.accept(new Move(List.of()));
                                }
                            } else {
                                listener.accept(new Move(List.of()));
                            }
                        }

                        // pattern
                    }
                }
            }
            case null, default -> {
            }
        }
    }
}
