% WIZARD STRATEGY -> Modular contextual bidding & position/table-aware card play
%
% Context terms:
%   bid_ctx(Hand, TrumpColor, TotalPlayers, BidsPlaced, OtherBidsSum)
%   play_ctx(Hand, TableCards, WinningCard, FollowingColor, TrumpColor, Bids, Tricks, RemainingPlayers, TableOverbid)

% ============================================================================
% 1. Hand & Table Queries (O(N) pure helpers)
% ============================================================================

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

has_card(Cards, Card) :-
    member(Card, Cards), !.

is_suit_boss(card(13, _), _, _) :- !.
is_suit_boss(card(12, Color), Hand, TableCards) :-
    (has_card(Hand, card(13, Color)) ; has_card(TableCards, card(13, Color))), !.

is_trump_card(card(_, TrumpColor), TrumpColor) :-
    valid_color(TrumpColor).

is_ruffing(Card, FollowingColor, TrumpColor) :-
    is_trump_card(Card, TrumpColor),
    FollowingColor \= TrumpColor.

has_trump_on_table(TableCards, TrumpColor) :-
    valid_color(TrumpColor),
    member(card(_, TrumpColor), TableCards), !.

% ============================================================================
% 2. Weighted Trump Selection
% ============================================================================

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

% ============================================================================
% 3. Contextual Bidding & Hook-Rule Adjustment
% ============================================================================

strategy_place_bid(Hand, TrumpColor, TotalPlayers, BidsPlaced, OtherBidsSum, Bid) :-
    length(Hand, HandSize),
    Ctx = bid_ctx(Hand, TrumpColor, TotalPlayers, BidsPlaced, OtherBidsSum),
    bid_estimate_x100(Ctx, TotalX100),
    RawBid is (TotalX100 + 50) // 100,
    clamp(0, HandSize, RawBid, Bid).

bid_estimate_x100(bid_ctx(Hand, TrumpColor, TotalPlayers, BidsPlaced, OtherBidsSum), TotalX100) :-
    length(Hand, HandSize),
    findall(Val, (member(Card, Hand), card_bid_value(Card, Hand, TrumpColor, HandSize, Val)), Values),
    sum_list(Values, CardSum),
    trump_synergy_bonus(Hand, TrumpColor, HandSize, SynergyBonus),
    player_count_adj_x100(TotalPlayers, CardSum, CountAdj),
    table_pressure_adj_x100(HandSize, TotalPlayers, BidsPlaced, OtherBidsSum, PressureAdj),
    lead_seat_adj_x100(HandSize, BidsPlaced, Hand, TrumpColor, LeadAdj),
    TotalX100 is CardSum + SynergyBonus + CountAdj + PressureAdj + LeadAdj.

% --- Card values (x100) ---

card_bid_value(wizard, _, _, _, 95) :- !.
card_bid_value(jester, _, _, _, 0) :- !.
card_bid_value(card(12, TrumpColor), Hand, TrumpColor, _, 93) :-
    valid_color(TrumpColor),
    has_card(Hand, card(13, TrumpColor)), !.
card_bid_value(card(Rank, TrumpColor), _, TrumpColor, HandSize, Value) :-
    valid_color(TrumpColor), !,
    trump_card_bid_value(Rank, HandSize, Value).
card_bid_value(card(Rank, Color), Hand, TrumpColor, HandSize, Value) :-
    valid_color(TrumpColor), !,
    count_color(Hand, Color, SuitLen),
    nontrump_card_bid_value(Rank, Color, Hand, SuitLen, HandSize, Value).
card_bid_value(card(Rank, Color), Hand, none, HandSize, Value) :-
    notrump_card_bid_value(Rank, Color, Hand, HandSize, Value).

trump_card_bid_value(13, _, 95) :- !.
trump_card_bid_value(12, _, 88) :- !.
trump_card_bid_value(11, HandSize, 85) :- HandSize =< 3, !.
trump_card_bid_value(11, _, 80) :- !.
trump_card_bid_value(10, HandSize, 78) :- HandSize =< 3, !.
trump_card_bid_value(10, _, 70) :- !.
trump_card_bid_value(Rank, HandSize, 70) :- in_range(8, 9, Rank), HandSize =< 3, !.
trump_card_bid_value(Rank, HandSize, 55) :- in_range(8, 9, Rank), HandSize > 3, !.
trump_card_bid_value(Rank, HandSize, 60) :- in_range(5, 7, Rank), HandSize =< 3, !.
trump_card_bid_value(Rank, _, 35) :- in_range(5, 7, Rank), !.
trump_card_bid_value(Rank, HandSize, 48) :- in_range(1, 4, Rank), HandSize =< 3, !.
trump_card_bid_value(_, _, 18).

nontrump_card_bid_value(13, _, _, _, HandSize, 65) :- HandSize =< 3, !.
nontrump_card_bid_value(13, _, _, SuitLen, _, 72) :- SuitLen =< 2, !.
nontrump_card_bid_value(13, _, _, 3, _, 62) :- !.
nontrump_card_bid_value(13, _, _, _, _, 48) :- !.
nontrump_card_bid_value(12, Color, Hand, SuitLen, _, Value) :-
    (SuitLen =< 2 -> Base = 48 ; (SuitLen == 3 -> Base = 38 ; Base = 22)),
    (has_card(Hand, card(13, Color)) -> Value is Base + 15 ; Value = Base), !.
nontrump_card_bid_value(11, _, _, SuitLen, _, 25) :- SuitLen =< 2, !.
nontrump_card_bid_value(11, _, _, 3, _, 15) :- !.
nontrump_card_bid_value(11, _, _, _, _, 5) :- !.
nontrump_card_bid_value(10, _, _, _, HandSize, 15) :- HandSize =< 4, !.
nontrump_card_bid_value(_, _, _, _, _, 0).

notrump_card_bid_value(13, _, _, _, 90) :- !.
notrump_card_bid_value(12, Color, Hand, _, 85) :- has_card(Hand, card(13, Color)), !.
notrump_card_bid_value(12, _, _, _, 68) :- !.
notrump_card_bid_value(11, Color, Hand, _, 75) :-
    has_card(Hand, card(13, Color)),
    has_card(Hand, card(12, Color)), !.
notrump_card_bid_value(11, _, _, _, 42) :- !.
notrump_card_bid_value(10, _, _, _, 22) :- !.
notrump_card_bid_value(9, _, _, HandSize, 10) :- HandSize =< 6, !.
notrump_card_bid_value(_, _, _, _, 0).

% --- Synergy & Context Adjustments ---

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

player_count_adj_x100(3, CardSum, Adj) :- !, Adj is (CardSum * 28) // 100.
player_count_adj_x100(4, _, 0) :- !.
player_count_adj_x100(5, CardSum, Adj) :- !, Adj is -((CardSum * 10) // 100).
player_count_adj_x100(TotalPlayers, CardSum, Adj) :-
    TotalPlayers >= 6, !,
    Adj is -((CardSum * 18) // 100).
player_count_adj_x100(_, _, 0).

table_pressure_adj_x100(_, TotalPlayers, BidsPlaced, _, 0) :-
    (BidsPlaced =< 0 ; TotalPlayers =< BidsPlaced), !.
table_pressure_adj_x100(HandSize, TotalPlayers, BidsPlaced, OtherBidsSum, Adj) :-
    RemainingBidders is TotalPlayers - BidsPlaced,
    FairPerBidderX100 is (HandSize * 100) // TotalPlayers,
    RemainingShareX100 is ((HandSize - OtherBidsSum) * 100) // RemainingBidders,
    DeltaX100 is RemainingShareX100 - FairPerBidderX100,
    RawAdj is (DeltaX100 * 35) // 100,
    clamp(-100, 100, RawAdj, Adj).

lead_seat_adj_x100(1, 0, [card(Rank, _)], none, 20) :- Rank >= 9, !.
lead_seat_adj_x100(1, 0, [card(Rank, _)], _, 20) :- Rank >= 10, !.
lead_seat_adj_x100(_, _, _, _, 0).

% --- Hook-Rule Bid Adjustment ---

strategy_adjust_bid([], _, _, _, _, _, 0) :- !.
strategy_adjust_bid(_, _, _, _, _, RejectedBid, 1) :-
    RejectedBid =< 0, !.
strategy_adjust_bid(Hand, _, _, _, _, RejectedBid, FinalBid) :-
    length(Hand, MaxSize),
    RejectedBid >= MaxSize, !,
    FinalBid is MaxSize - 1.
strategy_adjust_bid(Hand, TrumpColor, TotalPlayers, BidsPlaced, OtherBidsSum, RejectedBid, FinalBid) :-
    length(Hand, MaxSize),
    count_atom(Hand, jester, Jesters),
    MaxWinnable is MaxSize - Jesters,
    count_atom(Hand, wizard, Wizards),
    Ctx = bid_ctx(Hand, TrumpColor, TotalPlayers, BidsPlaced, OtherBidsSum),
    adjust_bid_direction(Ctx, RejectedBid, Wizards, MaxWinnable, FinalBid).

adjust_bid_direction(_, RejectedBid, _, MaxWinnable, FinalBid) :-
    RejectedBid + 1 > MaxWinnable, !,
    FinalBid is RejectedBid - 1.
adjust_bid_direction(_, RejectedBid, Wizards, _, FinalBid) :-
    RejectedBid =< Wizards, !,
    FinalBid is RejectedBid + 1.
adjust_bid_direction(Ctx, RejectedBid, _, _, FinalBid) :-
    bid_estimate_x100(Ctx, Strength),
    Threshold is RejectedBid * 100,
    (Strength >= Threshold -> FinalBid is RejectedBid + 1 ; FinalBid is RejectedBid - 1).

% ============================================================================
% 4. Intent Classification & Opening Card Play
% ============================================================================

classify_intent(Hand, Bids, Tricks, must_win_all) :-
    length(Hand, Remaining),
    Needed is Bids - Tricks,
    Needed >= Remaining, !.
classify_intent(Hand, Bids, Tricks, want_win_urgent) :-
    length(Hand, Remaining),
    Needed is Bids - Tricks,
    Needed > 0,
    Needed * 2 >= Remaining, !.
classify_intent(_, Bids, Tricks, want_win) :-
    Bids > Tricks, !.
classify_intent(_, _, _, want_lose).

strategy_best_card(Hand, _, none, none, TrumpColor, Bids, Tricks, _, TableOverbid, BestCard) :- !,
    classify_intent(Hand, Bids, Tricks, Intent),
    Ctx = play_ctx(Hand, [], none, none, TrumpColor, Bids, Tricks, 0, TableOverbid),
    findall(pair(Score, Card), (member(Card, Hand), opening_score(Intent, Card, Ctx, Score)), Pairs),
    max_scored(Pairs, BestCard).

strategy_best_card(Hand, TableCards, WinningCard, FollowingColor, TrumpColor, Bids, Tricks, RemainingPlayers, TableOverbid, BestCard) :-
    valid_card(WinningCard),
    classify_intent(Hand, Bids, Tricks, Intent),
    playable_cards(Hand, FollowingColor, Playable),
    partition_options(Playable, WinningCard, TrumpColor, Winners, Losers),
    Ctx = play_ctx(Hand, TableCards, WinningCard, FollowingColor, TrumpColor, Bids, Tricks, RemainingPlayers, TableOverbid),
    choose_follow(Intent, Ctx, Playable, Winners, Losers, BestCard).

% --- Opening Scores ---

opening_score(must_win_all, wizard, _, 1000) :- !.
opening_score(must_win_all, jester, _, 0) :- !.
opening_score(must_win_all, card(Rank, TrumpColor), play_ctx(_, _, _, _, TrumpColor, _, _, _, _), Score) :-
    valid_color(TrumpColor), !,
    Score is 500 + Rank * 10.
opening_score(must_win_all, card(Rank, _), _, Score) :-
    Score is 200 + Rank * 10.

opening_score(want_win_urgent, wizard, _, 460) :- !.
opening_score(want_win_urgent, Card, Ctx, Score) :- !,
    opening_score(want_win, Card, Ctx, Score).

opening_score(want_win, wizard, play_ctx(_, _, _, _, _, _, _, _, TableOverbid), Score) :- !,
    (TableOverbid > 0 -> Score = 430 ; Score = 350).
opening_score(want_win, jester, _, 10) :- !.
opening_score(want_win, card(Rank, TrumpColor), play_ctx(_, _, _, _, TrumpColor, _, _, _, TableOverbid), Score) :-
    valid_color(TrumpColor),
    Rank >= 11, !,
    (TableOverbid > 0 -> Bonus = 40 ; Bonus = 0),
    Score is 380 + Rank + Bonus.
opening_score(want_win, card(Rank, TrumpColor), play_ctx(_, _, _, _, TrumpColor, _, _, _, _), Score) :-
    valid_color(TrumpColor),
    Rank >= 7, !,
    Score is 200 + Rank.
opening_score(want_win, card(Rank, TrumpColor), play_ctx(_, _, _, _, TrumpColor, _, _, _, _), Score) :-
    valid_color(TrumpColor), !,
    Score is 80 + Rank.
opening_score(want_win, card(13, Color), play_ctx(Hand, _, _, _, _, _, _, _, TableOverbid), Score) :- !,
    count_color(Hand, Color, SuitLen),
    (TableOverbid > 0 -> Bonus = 30 ; Bonus = 0),
    (SuitLen =< 3 -> Score is 420 + Bonus ; Score is 360 + Bonus).
opening_score(want_win, card(12, Color), play_ctx(Hand, _, _, _, _, _, _, _, _), Score) :- !,
    count_color(Hand, Color, SuitLen),
    (SuitLen =< 3 -> Score = 300 ; Score = 220).
opening_score(want_win, card(Rank, _), _, Score) :-
    Score is 100 + Rank.

opening_score(want_lose, wizard, _, -100) :- !.
opening_score(want_lose, jester, play_ctx(_, _, _, _, _, _, _, _, TableOverbid), 450) :-
    TableOverbid < 0, !.
opening_score(want_lose, jester, _, 310) :- !.
opening_score(want_lose, card(Rank, TrumpColor), play_ctx(_, _, _, _, TrumpColor, _, _, _, _), Score) :-
    valid_color(TrumpColor), !,
    Score is 100 - Rank * 6.
opening_score(want_lose, card(Rank, _), _, Score) :-
    Rank =< 5, !,
    Score is 400 - Rank * 15.
opening_score(want_lose, card(Rank, _), _, Score) :-
    Score is 300 - Rank * 15.

% ============================================================================
% 5. Context- & Position-Aware Following Card Selection
% ============================================================================

% --- Want Lose ---

choose_follow(want_lose, play_ctx(Hand, _, _, _, TrumpColor, _, _, _, _), _, _, Losers, BestCard) :-
    Losers \= [], !,
    findall(pair(Score, Card), (member(Card, Losers), shed_danger_score(Card, Hand, TrumpColor, Score)), Pairs),
    max_scored(Pairs, BestCard).

choose_follow(want_lose, play_ctx(_, _, _, _, TrumpColor, _, _, 0, _), Playable, _, [], BestCard) :- !,
    findall(pair(Score, Card), (member(Card, Playable), forced_win_shed_score(Card, TrumpColor, Score)), Pairs),
    max_scored(Pairs, BestCard).

choose_follow(want_lose, play_ctx(_, _, _, _, TrumpColor, _, _, _, _), Playable, _, [], BestCard) :- !,
    pick_cheapest(Playable, TrumpColor, BestCard).

% --- Must Win All ---

choose_follow(must_win_all, play_ctx(_, _, _, _, TrumpColor, _, _, 0, _), _, Winners, _, BestCard) :-
    Winners \= [], !,
    pick_cheapest(Winners, TrumpColor, BestCard).

choose_follow(must_win_all, play_ctx(_, _, _, _, TrumpColor, _, _, _, _), _, Winners, _, BestCard) :-
    Winners \= [], !,
    findall(pair(Str, Card), (member(Card, Winners), win_strength(Card, TrumpColor, Str)), Pairs),
    max_scored(Pairs, BestCard).

% --- Want Win (Last to Play / Underbid -> cheapest winner) ---

choose_follow(Intent, play_ctx(_, _, _, _, TrumpColor, _, _, 0, _), _, Winners, _, BestCard) :-
    (Intent == want_win ; Intent == want_win_urgent),
    Winners \= [], !,
    pick_cheapest(Winners, TrumpColor, BestCard).

choose_follow(Intent, play_ctx(_, _, _, _, TrumpColor, _, _, _, TableOverbid), _, Winners, _, BestCard) :-
    (Intent == want_win ; Intent == want_win_urgent),
    Winners \= [],
    TableOverbid < 0, !,
    pick_cheapest(Winners, TrumpColor, BestCard).

% --- Want Win (Early Seat + Neutral/Overbid -> secure win) ---

choose_follow(Intent, Ctx, _, Winners, _, BestCard) :-
    (Intent == want_win ; Intent == want_win_urgent),
    Winners \= [], !,
    findall(pair(Score, Card), (member(Card, Winners), secure_win_score(Card, Ctx, Score)), Pairs),
    max_scored(Pairs, BestCard).

% --- Unable to Win (Discard / Void Creation) ---

choose_follow(_, play_ctx(Hand, _, _, _, TrumpColor, _, _, _, _), Playable, [], _, BestCard) :-
    findall(pair(Score, Card), (member(Card, Playable), discard_want_win_score(Card, Hand, TrumpColor, Score)), Pairs),
    max_scored(Pairs, BestCard).

% --- Follow Scoring Helpers ---

pick_cheapest(Cards, TrumpColor, BestCard) :-
    findall(pair(Cost, Card), (member(Card, Cards), play_cost(Card, TrumpColor, Cost)), Pairs),
    min_scored(Pairs, BestCard).

has_secure_winner(Winners, Ctx) :-
    member(Card, Winners),
    is_secure_winner(Card, Ctx), !.

is_secure_winner(wizard, _) :- !.
is_secure_winner(card(_, TrumpColor), play_ctx(_, _, _, _, TrumpColor, _, _, _, _)) :-
    valid_color(TrumpColor), !.
is_secure_winner(Card, play_ctx(Hand, TableCards, _, _, _, _, _, _, _)) :-
    is_suit_boss(Card, Hand, TableCards), !.
is_secure_winner(card(Rank, _), _) :-
    Rank >= 10.

secure_win_score(card(Rank, FollowingColor), play_ctx(Hand, TableCards, _, FollowingColor, TrumpColor, _, _, _, _), 450) :-
    FollowingColor \= TrumpColor,
    is_suit_boss(card(Rank, FollowingColor), Hand, TableCards), !.
secure_win_score(card(Rank, TrumpColor), play_ctx(_, TableCards, _, FollowingColor, TrumpColor, _, _, 1, _), Score) :-
    is_ruffing(card(Rank, TrumpColor), FollowingColor, TrumpColor),
    \+ has_trump_on_table(TableCards, TrumpColor), !,
    Score is 360 - Rank * 5.
secure_win_score(card(Rank, TrumpColor), play_ctx(_, TableCards, _, FollowingColor, TrumpColor, _, _, _, _), Score) :-
    is_ruffing(card(Rank, TrumpColor), FollowingColor, TrumpColor),
    \+ has_trump_on_table(TableCards, TrumpColor), !,
    Diff is abs(Rank - 8),
    Score is 360 - Diff * 8.
secure_win_score(card(Rank, TrumpColor), play_ctx(_, _, _, _, TrumpColor, _, _, RemainingPlayers, _), Score) :-
    valid_color(TrumpColor),
    RemainingPlayers >= 2, !,
    Score is 300 + Rank * 8.
secure_win_score(card(Rank, TrumpColor), play_ctx(_, _, _, _, TrumpColor, _, _, _, _), Score) :-
    valid_color(TrumpColor), !,
    Score is 340 - Rank * 4.
secure_win_score(wizard, _, 240) :- !.
secure_win_score(card(Rank, _), _, Score) :-
    Score is 150 + Rank * 12.

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

shed_danger_score(wizard, _, _, 1000) :- !.
shed_danger_score(jester, _, _, 0) :- !.
shed_danger_score(card(Rank, TrumpColor), _, TrumpColor, Score) :-
    valid_color(TrumpColor), !,
    Score is 400 + Rank * 20.
shed_danger_score(card(Rank, Color), Hand, _, Score) :-
    Rank >= 10,
    count_color(Hand, Color, SuitLen),
    SuitLen =< 2, !,
    Score is 140 + Rank * 20.
shed_danger_score(card(Rank, _), _, _, Score) :-
    Score is 100 + Rank * 20.

forced_win_shed_score(wizard, _, -100) :- !.
forced_win_shed_score(jester, _, 0) :- !.
forced_win_shed_score(card(Rank, TrumpColor), TrumpColor, Score) :-
    valid_color(TrumpColor), !,
    Score is 200 + Rank * 10.
forced_win_shed_score(card(Rank, _), _, Score) :-
    Score is 400 + Rank * 10.
