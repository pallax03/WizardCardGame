% WIZARD DOMAIN -> Card representation, validation, and pure accessors

valid_color(red).
valid_color(green).
valid_color(yellow).
valid_color(blue).

valid_rank(Rank) :- in_range(1, 13, Rank).

% valid_card(+Card) -> deterministic validation of a single Wizard card term
valid_card(wizard) :- !.
valid_card(jester) :- !.
valid_card(card(Rank, Color)) :-
    valid_rank(Rank),
    valid_color(Color), !.

% validate_cards(+Cards) -> succeeds iff every element is a valid card
validate_cards([]).
validate_cards([Card | Rest]) :-
    valid_card(Card),
    validate_cards(Rest).

% card_of_color(?Card, ?Color) -> fast structural accessor (no re-validation in hot loops)
card_of_color(card(_, Color), Color).

% card_value(+Card, -RankValue) -> base rank value (0 for jester, 1..13 for standard, 14 for wizard)
card_value(jester, 0).
card_value(card(Rank, _), Rank).
card_value(wizard, 14).
