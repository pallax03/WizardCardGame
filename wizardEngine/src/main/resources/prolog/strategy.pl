% WIZARD STRATEGY -> Pure O(N) heuristics and decision logic

% count_color(+Cards, +Color, -Count) -> tail-recursive O(N) color counter
count_color(Cards, Color, Count) :-
    count_color_acc(Cards, Color, 0, Count).

count_color_acc([], _, Acc, Acc).
count_color_acc([card(_, Color) | Rest], Color, Acc, Count) :- !,
    NextAcc is Acc + 1,
    count_color_acc(Rest, Color, NextAcc, Count).
count_color_acc([_ | Rest], Color, Acc, Count) :-
    count_color_acc(Rest, Color, Acc, Count).

% dominant_color(+Cards, -Color) -> color with highest count in Cards
dominant_color(Cards, BestColor) :-
    findall(
        pair(Count, Color),
        (valid_color(Color), count_color(Cards, Color, Count), Count > 0),
        Pairs
    ),
    Pairs \= [],
    max_scored(Pairs, BestColor).

% strategy_choose_trump(+Hand, -TrumpColor)
strategy_choose_trump(Hand, TrumpColor) :-
    dominant_color(Hand, TrumpColor), !.
strategy_choose_trump(_, red).

% safe_trick(+Card, +Hand, +TrumpColor)
safe_trick(wizard, _, _).
safe_trick(card(Rank, TrumpColor), _, TrumpColor) :-
    valid_color(TrumpColor),
    in_range(10, 13, Rank).
safe_trick(card(13, Color), _, TrumpColor) :-
    Color \= TrumpColor.

% risky_trick(+Card, +Hand, +TrumpColor)
risky_trick(card(Rank, Color), Hand, TrumpColor) :-
    in_range(10, 12, Rank),
    Color \= TrumpColor,
    count_color(Hand, Color, Count),
    Count >= 5.
risky_trick(card(Rank, Color), Hand, TrumpColor) :-
    in_range(11, 12, Rank),
    Color \= TrumpColor,
    count_color(Hand, Color, Count),
    Count =< 2.

% is_trick_card(+Card, +Hand, +TrumpColor) -> deterministic classification preventing double counting
is_trick_card(Card, Hand, TrumpColor) :-
    (safe_trick(Card, Hand, TrumpColor) ; risky_trick(Card, Hand, TrumpColor)), !.

% strategy_place_bid(+Hand, +TrumpColor, -Bid)
strategy_place_bid(Hand, TrumpColor, Bid) :-
    findall(Card, (member(Card, Hand), is_trick_card(Card, Hand, TrumpColor)), TrickCards),
    length(TrickCards, Bid).

% strategy_adjust_bid(+Hand, +RejectedBid, -FinalBid)
strategy_adjust_bid([], _, 0) :- !.
strategy_adjust_bid(_, 0, 1) :- !.
strategy_adjust_bid(Hand, RejectedBid, FinalBid) :-
    length(Hand, MaxSize),
    RejectedBid >= MaxSize, !,
    FinalBid is MaxSize - 1.
strategy_adjust_bid(Hand, RejectedBid, FinalBid) :-
    member(jester, Hand), !,
    FinalBid is RejectedBid - 1.
strategy_adjust_bid(_, RejectedBid, FinalBid) :-
    FinalBid is RejectedBid + 1.

% play_cost(+Card, +TrumpColor, -Cost) -> trump-aware cost for picking cheapest winning/discard card
play_cost(jester, _, 0) :- !.
play_cost(wizard, _, 100) :- !.
play_cost(card(Rank, TrumpColor), TrumpColor, Cost) :-
    valid_color(TrumpColor), !,
    Cost is Rank + 13.
play_cost(card(Rank, _), _, Rank).

% lowest_card(+Cards, +TrumpColor, -LowestCard)
lowest_card(Cards, TrumpColor, LowestCard) :-
    findall(pair(Cost, Card), (member(Card, Cards), play_cost(Card, TrumpColor, Cost)), Pairs),
    min_scored(Pairs, LowestCard).

% dangerous_value(+Card, +TrumpColor, -Value)
dangerous_value(jester, _, 0) :- !.
dangerous_value(wizard, _, 100) :- !.
dangerous_value(card(Rank, TrumpColor), TrumpColor, Value) :-
    valid_color(TrumpColor), !,
    Value is Rank + 6.
dangerous_value(card(Rank, _), _, Rank).

% most_dangerous_card(+Cards, +TrumpColor, -MostDangerousCard)
most_dangerous_card(Cards, TrumpColor, MostDangerousCard) :-
    findall(pair(Value, Card), (member(Card, Cards), dangerous_value(Card, TrumpColor, Value)), Pairs),
    max_scored(Pairs, MostDangerousCard).

% smart_discard(+PlayableCards, +TrumpColor, -BestDiscardCard)
smart_discard(PlayableCards, TrumpColor, BestDiscardCard) :-
    findall(Card, (
        member(Card, PlayableCards),
        Card \= wizard,
        Card \= jester,
        \+ card_of_color(Card, TrumpColor)
    ), JunkCards),
    JunkCards \= [], !,
    lowest_card(JunkCards, TrumpColor, BestDiscardCard).
smart_discard(PlayableCards, TrumpColor, BestDiscardCard) :-
    lowest_card(PlayableCards, TrumpColor, BestDiscardCard).

wants_to_win(Bids, Tricks) :- Bids > Tricks.

% strategy_best_card(+Hand, +WinningCard, +FollowingColor, +TrumpColor, +Bids, +Tricks, -BestCard)
strategy_best_card(Hand, none, none, TrumpColor, Bids, Tricks, BestCard) :- !,
    opening_card(Hand, TrumpColor, Bids, Tricks, BestCard).
strategy_best_card(Hand, WinningCard, FollowingColor, TrumpColor, Bids, Tricks, BestCard) :-
    valid_card(WinningCard),
    playable_cards(Hand, FollowingColor, PlayableCards),
    partition_options(PlayableCards, WinningCard, TrumpColor, WinningCards, LosingCards),
    (wants_to_win(Bids, Tricks) ->
        follow_win(PlayableCards, WinningCards, TrumpColor, BestCard)
    ;
        follow_lose(PlayableCards, LosingCards, TrumpColor, BestCard)
    ).

opening_card(Hand, TrumpColor, Bids, Tricks, BestCard) :-
    wants_to_win(Bids, Tricks), !,
    findall(Card, (member(Card, Hand), is_trick_card(Card, Hand, TrumpColor)), TrickCards),
    (TrickCards \= [] ->
        lowest_card(TrickCards, TrumpColor, BestCard)
    ;
        most_dangerous_card(Hand, TrumpColor, BestCard)
    ).
opening_card(Hand, TrumpColor, _, _, BestCard) :-
    smart_discard(Hand, TrumpColor, BestCard).

follow_win(_, WinningCards, TrumpColor, BestCard) :-
    WinningCards \= [], !,
    lowest_card(WinningCards, TrumpColor, BestCard).
follow_win(PlayableCards, _, _, jester) :-
    member(jester, PlayableCards), !.
follow_win(PlayableCards, _, TrumpColor, BestCard) :-
    smart_discard(PlayableCards, TrumpColor, BestCard).

follow_lose(_, LosingCards, TrumpColor, BestCard) :-
    LosingCards \= [], !,
    most_dangerous_card(LosingCards, TrumpColor, BestCard).
follow_lose(PlayableCards, _, TrumpColor, BestCard) :-
    most_dangerous_card(PlayableCards, TrumpColor, BestCard).
