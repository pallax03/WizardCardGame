% WIZARD STRATEGY -> Weighted trump, continuous integer-scaled bidding, and declarative Generate & Score play

% --- Counting & Suit Helpers ---

count_color(Cards, Color, Count) :-
    count_color_acc(Cards, Color, 0, Count).

count_color_acc([], _, Acc, Acc).
count_color_acc([card(_, Color) | Rest], Color, Acc, Count) :- !,
    NextAcc is Acc + 1,
    count_color_acc(Rest, Color, NextAcc, Count).
count_color_acc([_ | Rest], Color, Acc, Count) :-
    count_color_acc(Rest, Color, Acc, Count).

count_atom(Cards, Atom, Count) :-
    count_atom_acc(Cards, Atom, 0, Count).

count_atom_acc([], _, Acc, Acc).
count_atom_acc([Atom | Rest], Atom, Acc, Count) :- !,
    NextAcc is Acc + 1,
    count_atom_acc(Rest, Atom, NextAcc, Count).
count_atom_acc([_ | Rest], Atom, Acc, Count) :-
    count_atom_acc(Rest, Atom, Acc, Count).

count_voids(Hand, TrumpColor, Voids) :-
    findall(C, (valid_color(C), C \= TrumpColor, count_color(Hand, C, 0)), VoidColors),
    length(VoidColors, Voids).

suit_stats(Hand, Color, Count, RankSum, HighCount) :-
    suit_stats_acc(Hand, Color, 0, 0, 0, Count, RankSum, HighCount).

suit_stats_acc([], _, Count, RankSum, HighCount, Count, RankSum, HighCount).
suit_stats_acc([card(Rank, Color) | Rest], Color, CAcc, RAcc, HAcc, Count, RankSum, HighCount) :- !,
    NextC is CAcc + 1,
    NextR is RAcc + Rank,
    (Rank >= 10 -> NextH is HAcc + 1 ; NextH = HAcc),
    suit_stats_acc(Rest, Color, NextC, NextR, NextH, Count, RankSum, HighCount).
suit_stats_acc([_ | Rest], Color, CAcc, RAcc, HAcc, Count, RankSum, HighCount) :-
    suit_stats_acc(Rest, Color, CAcc, RAcc, HAcc, Count, RankSum, HighCount).

% --- 1. Weighted Trump Selection ---

strategy_choose_trump(Hand, TrumpColor) :-
    findall(
        pair(Score, Color),
        (
            valid_color(Color),
            suit_stats(Hand, Color, Count, RankSum, HighCount),
            Count > 0,
            Score is Count * 12 + RankSum + HighCount * 8
        ),
        Pairs
    ),
    Pairs \= [], !,
    max_scored(Pairs, TrumpColor).
strategy_choose_trump(_, red).

% --- 2. Continuous Integer-Scaled Bidding (x100) ---

card_bid_value(wizard, _, _, _, 95) :- !.
card_bid_value(jester, _, _, _, 0) :- !.
card_bid_value(card(Rank, TrumpColor), _, TrumpColor, HandSize, Value) :-
    valid_color(TrumpColor), !,
    trump_card_bid_value(Rank, HandSize, Value).
card_bid_value(card(Rank, Color), Hand, TrumpColor, HandSize, Value) :-
    valid_color(TrumpColor), !,
    count_color(Hand, Color, SuitLen),
    nontrump_card_bid_value(Rank, SuitLen, HandSize, Value).
card_bid_value(card(Rank, _), _, none, HandSize, Value) :-
    notrump_card_bid_value(Rank, HandSize, Value).

trump_card_bid_value(13, _, 95) :- !.
trump_card_bid_value(12, _, 88) :- !.
trump_card_bid_value(11, _, 80) :- !.
trump_card_bid_value(10, _, 70) :- !.
trump_card_bid_value(Rank, HandSize, 60) :- in_range(8, 9, Rank), HandSize =< 5, !.
trump_card_bid_value(Rank, _, 55) :- in_range(8, 9, Rank), !.
trump_card_bid_value(Rank, HandSize, 45) :- in_range(5, 7, Rank), HandSize =< 5, !.
trump_card_bid_value(Rank, _, 35) :- in_range(5, 7, Rank), !.
trump_card_bid_value(Rank, HandSize, 30) :- in_range(1, 4, Rank), HandSize =< 4, !.
trump_card_bid_value(_, _, 15).

nontrump_card_bid_value(13, SuitLen, _, 75) :- SuitLen =< 3, !.
nontrump_card_bid_value(13, _, _, 60) :- !.
nontrump_card_bid_value(12, SuitLen, _, 50) :- SuitLen =< 3, !.
nontrump_card_bid_value(12, _, _, 30) :- !.
nontrump_card_bid_value(11, SuitLen, _, 25) :- SuitLen =< 2, !.
nontrump_card_bid_value(11, _, _, 15) :- !.
nontrump_card_bid_value(10, _, HandSize, 10) :- HandSize =< 5, !.
nontrump_card_bid_value(_, _, _, 0).

notrump_card_bid_value(13, _, 90) :- !.
notrump_card_bid_value(12, _, 70) :- !.
notrump_card_bid_value(11, _, 45) :- !.
notrump_card_bid_value(10, _, 25) :- !.
notrump_card_bid_value(9, HandSize, 10) :- HandSize =< 6, !.
notrump_card_bid_value(_, _, 0).

trump_len_bonus(TrumpCount, Bonus) :-
    TrumpCount >= 3, !,
    Bonus is (TrumpCount - 2) * 20.
trump_len_bonus(_, 0).

void_ruff_bonus(Hand, TrumpColor, HandSize, TrumpCount, Bonus) :-
    HandSize >= 5,
    TrumpCount >= 2, !,
    count_voids(Hand, TrumpColor, Voids),
    MaxUsable is TrumpCount - 1,
    (Voids < MaxUsable -> UsableVoids = Voids ; UsableVoids = MaxUsable),
    Bonus is UsableVoids * 25.
void_ruff_bonus(_, _, _, _, 0).

trump_synergy_bonus(Hand, TrumpColor, HandSize, Bonus) :-
    valid_color(TrumpColor),
    HandSize >= 4, !,
    count_color(Hand, TrumpColor, TrumpCount),
    trump_len_bonus(TrumpCount, LenBonus),
    void_ruff_bonus(Hand, TrumpColor, HandSize, TrumpCount, VoidBonus),
    Bonus is LenBonus + VoidBonus.
trump_synergy_bonus(_, _, _, 0).

clamp_bid(RawBid, HandSize, HandSize) :- RawBid > HandSize, !.
clamp_bid(RawBid, _, 0) :- RawBid < 0, !.
clamp_bid(RawBid, _, RawBid).

strategy_place_bid(Hand, TrumpColor, Bid) :-
    length(Hand, HandSize),
    findall(Val, (member(Card, Hand), card_bid_value(Card, Hand, TrumpColor, HandSize, Val)), Values),
    sum_list(Values, CardSum),
    trump_synergy_bonus(Hand, TrumpColor, HandSize, Bonus),
    TotalX100 is CardSum + Bonus,
    RawBid is (TotalX100 + 50) // 100,
    clamp_bid(RawBid, HandSize, Bid).

% --- 3. Consistent Hook-Rule Bid Adjustment ---

hand_strength_x100(Hand, Strength) :-
    findall(Val, (member(Card, Hand), agnostic_card_strength(Card, Val)), Values),
    sum_list(Values, Strength).

agnostic_card_strength(wizard, 95) :- !.
agnostic_card_strength(card(13, _), 80) :- !.
agnostic_card_strength(card(12, _), 55) :- !.
agnostic_card_strength(card(11, _), 30) :- !.
agnostic_card_strength(card(10, _), 15) :- !.
agnostic_card_strength(_, 0).

adjust_bid_direction(_, RejectedBid, _, MaxWinnable, FinalBid) :-
    RejectedBid + 1 > MaxWinnable, !,
    FinalBid is RejectedBid - 1.
adjust_bid_direction(_, RejectedBid, Wizards, _, FinalBid) :-
    RejectedBid =< Wizards, !,
    FinalBid is RejectedBid + 1.
adjust_bid_direction(Hand, RejectedBid, _, _, FinalBid) :-
    hand_strength_x100(Hand, Strength),
    Threshold is RejectedBid * 100,
    (Strength >= Threshold -> FinalBid is RejectedBid + 1 ; FinalBid is RejectedBid - 1).

strategy_adjust_bid([], _, 0) :- !.
strategy_adjust_bid(_, RejectedBid, 1) :-
    RejectedBid =< 0, !.
strategy_adjust_bid(Hand, RejectedBid, FinalBid) :-
    length(Hand, MaxSize),
    RejectedBid >= MaxSize, !,
    FinalBid is MaxSize - 1.
strategy_adjust_bid(Hand, RejectedBid, FinalBid) :-
    length(Hand, MaxSize),
    count_atom(Hand, jester, Jesters),
    MaxWinnable is MaxSize - Jesters,
    count_atom(Hand, wizard, Wizards),
    adjust_bid_direction(Hand, RejectedBid, Wizards, MaxWinnable, FinalBid).

% --- 4. Declarative Generate & Score Card Play ---

play_cost(jester, _, 0) :- !.
play_cost(wizard, _, 100) :- !.
play_cost(card(Rank, TrumpColor), TrumpColor, Cost) :-
    valid_color(TrumpColor), !,
    Cost is Rank + 13.
play_cost(card(Rank, _), _, Rank).

win_strength(wizard, _, 1000) :- !.
win_strength(jester, _, 0) :- !.
win_strength(card(Rank, TrumpColor), TrumpColor, Strength) :-
    valid_color(TrumpColor), !,
    Strength is 500 + Rank.
win_strength(card(Rank, _), _, Strength) :-
    Strength is 100 + Rank.

classify_intent(Hand, Bids, Tricks, must_win_all) :-
    length(Hand, Remaining),
    Needed is Bids - Tricks,
    Needed >= Remaining, !.
classify_intent(_, Bids, Tricks, want_win) :-
    Bids > Tricks, !.
classify_intent(_, _, _, want_lose).

strategy_best_card(Hand, none, none, TrumpColor, Bids, Tricks, BestCard) :- !,
    classify_intent(Hand, Bids, Tricks, Intent),
    findall(pair(Score, Card), (member(Card, Hand), opening_score(Intent, Card, Hand, TrumpColor, Score)), Pairs),
    max_scored(Pairs, BestCard).

strategy_best_card(Hand, WinningCard, FollowingColor, TrumpColor, Bids, Tricks, BestCard) :-
    valid_card(WinningCard),
    classify_intent(Hand, Bids, Tricks, Intent),
    playable_cards(Hand, FollowingColor, PlayableCards),
    partition_options(PlayableCards, WinningCard, TrumpColor, WinningCards, LosingCards),
    follow_card(Intent, PlayableCards, WinningCards, LosingCards, Hand, TrumpColor, BestCard).

% Opening scores
opening_score(must_win_all, wizard, _, _, 1000) :- !.
opening_score(must_win_all, jester, _, _, 0) :- !.
opening_score(must_win_all, card(Rank, TrumpColor), _, TrumpColor, Score) :-
    valid_color(TrumpColor), !,
    Score is 500 + Rank * 10.
opening_score(must_win_all, card(Rank, _), _, _, Score) :-
    Score is 200 + Rank * 10.

opening_score(want_win, wizard, _, _, 350) :- !.
opening_score(want_win, jester, _, _, 10) :- !.
opening_score(want_win, card(Rank, TrumpColor), _, TrumpColor, Score) :-
    valid_color(TrumpColor),
    Rank >= 11, !,
    Score is 380 + Rank.
opening_score(want_win, card(Rank, TrumpColor), _, TrumpColor, Score) :-
    valid_color(TrumpColor),
    Rank >= 7, !,
    Score is 200 + Rank.
opening_score(want_win, card(Rank, TrumpColor), _, TrumpColor, Score) :-
    valid_color(TrumpColor), !,
    Score is 80 + Rank.
opening_score(want_win, card(13, Color), Hand, _, Score) :- !,
    count_color(Hand, Color, SuitLen),
    (SuitLen =< 3 -> Score = 420 ; Score = 360).
opening_score(want_win, card(12, Color), Hand, _, Score) :- !,
    count_color(Hand, Color, SuitLen),
    (SuitLen =< 3 -> Score = 300 ; Score = 220).
opening_score(want_win, card(Rank, _), _, _, Score) :-
    Score is 100 + Rank.

opening_score(want_lose, wizard, _, _, -100) :- !.
opening_score(want_lose, jester, _, _, 310) :- !.
opening_score(want_lose, card(Rank, TrumpColor), _, TrumpColor, Score) :-
    valid_color(TrumpColor), !,
    Score is 100 - Rank * 6.
opening_score(want_lose, card(Rank, _), _, _, Score) :-
    Rank =< 5, !,
    Score is 400 - Rank * 15.
opening_score(want_lose, card(Rank, _), _, _, Score) :-
    Score is 300 - Rank * 15.

% Following card selection (want_lose MUST precede catch-all discard)
follow_card(want_lose, _, _, LosingCards, _, TrumpColor, BestCard) :-
    LosingCards \= [], !,
    findall(pair(Score, Card), (member(Card, LosingCards), shed_danger_score(Card, TrumpColor, Score)), Pairs),
    max_scored(Pairs, BestCard).

follow_card(want_lose, PlayableCards, _, [], _, TrumpColor, BestCard) :- !,
    findall(pair(Cost, Card), (member(Card, PlayableCards), play_cost(Card, TrumpColor, Cost)), Pairs),
    min_scored(Pairs, BestCard).

follow_card(must_win_all, _, WinningCards, _, _, TrumpColor, BestCard) :-
    WinningCards \= [], !,
    findall(pair(Str, Card), (member(Card, WinningCards), win_strength(Card, TrumpColor, Str)), Pairs),
    max_scored(Pairs, BestCard).

follow_card(want_win, _, WinningCards, _, _, TrumpColor, BestCard) :-
    WinningCards \= [], !,
    findall(pair(Cost, Card), (member(Card, WinningCards), play_cost(Card, TrumpColor, Cost)), Pairs),
    min_scored(Pairs, BestCard).

follow_card(_, PlayableCards, [], _, Hand, TrumpColor, BestCard) :-
    findall(pair(Score, Card), (member(Card, PlayableCards), discard_want_win_score(Card, Hand, TrumpColor, Score)), Pairs),
    max_scored(Pairs, BestCard).

discard_want_win_score(wizard, _, _, -100) :- !.
discard_want_win_score(jester, _, _, 300) :- !.
discard_want_win_score(card(Rank, TrumpColor), _, TrumpColor, Score) :-
    valid_color(TrumpColor), !,
    Score is 150 - Rank * 10.
discard_want_win_score(card(Rank, Color), Hand, _, Score) :-
    Rank =< 8, !,
    count_color(Hand, Color, SuitLen),
    (SuitLen == 1 -> VoidBonus = 40 ; VoidBonus = 0),
    Score is 500 - Rank * 20 + VoidBonus.
discard_want_win_score(card(Rank, _), _, _, Score) :-
    Score is 380 - Rank * 20.

shed_danger_score(wizard, _, 1000) :- !.
shed_danger_score(jester, _, 0) :- !.
shed_danger_score(card(Rank, TrumpColor), TrumpColor, Score) :-
    valid_color(TrumpColor), !,
    Score is 400 + Rank * 20.
shed_danger_score(card(Rank, _), _, Score) :-
    Score is 100 + Rank * 20.
