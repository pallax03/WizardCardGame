% WIZARD RULES -> Core game legality and trick-resolution rules

% following_standard_cards(+Hand, +FollowingColor, -LegalStandardCards)
following_standard_cards(_, none, []) :- !.
following_standard_cards(Hand, FollowingColor, LegalStandardCards) :-
    findall(Card, (member(Card, Hand), card_of_color(Card, FollowingColor)), LegalStandardCards).

% special_cards(+Hand, -Specials)
special_cards(Hand, Specials) :-
    findall(Card, (member(Card, Hand), (Card == wizard ; Card == jester)), Specials).

% playable_cards(+Hand, +FollowingColor, -PlayableCards) -> single-pass legal card filtering
playable_cards(Hand, none, Hand) :- !.
playable_cards(Hand, FollowingColor, PlayableCards) :-
    following_standard_cards(Hand, FollowingColor, FollowingStandardCards),
    (FollowingStandardCards == [] ->
        PlayableCards = Hand
    ;
        special_cards(Hand, SpecialCards),
        append(FollowingStandardCards, SpecialCards, PlayableCards)
    ).

% beats(+MyCard, +WinningCard, +TrumpColor) -> pure trick-winner relation (safe against invalid atoms)
beats(wizard, jester, _).
beats(card(_, _), jester, _).
beats(wizard, card(_, _), _).
beats(card(_, TrumpColor), card(_, Color), TrumpColor) :-
    valid_color(TrumpColor),
    Color \= TrumpColor.
beats(card(MyRank, Color), card(WinRank, Color), _) :-
    MyRank > WinRank.

% partition_options(+PlayableCards, +WinningCard, +TrumpColor, -WinningCards, -LosingCards) -> single-pass O(N)
partition_options([], _, _, [], []).
partition_options([Card | Rest], WinningCard, TrumpColor, [Card | WinRest], LoseRest) :-
    beats(Card, WinningCard, TrumpColor), !,
    partition_options(Rest, WinningCard, TrumpColor, WinRest, LoseRest).
partition_options([Card | Rest], WinningCard, TrumpColor, WinRest, [Card | LoseRest]) :-
    partition_options(Rest, WinningCard, TrumpColor, WinRest, LoseRest).
