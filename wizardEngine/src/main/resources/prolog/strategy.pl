% WIZARD STRATEGY.HELPER -> Feature extracted from Rules and Basic, for Strategies

% extract_colors(+Cards, -Colors) -> return the list of colors in a List of cards (not distincts)
extract_colors(Cards, Colors) :- findall(Color, member(card(_, Color), Cards), Colors).

% extract_colors([card(1, red), card(5, green), card(8, yellow), card(13, red)], Colors) -> Colors / [red,green,yellow,red]

% color_frequencies(+Cards, -Frequencies) -> return a mapped list from a List of Cards with DistinctColors and his frequencies
color_frequencies(Cards, Frequencies) :-
	extract_colors(Cards, Colors),
	distinct(Colors, DistinctColors),
	findall(
		freq(Color, N),
		(member(Color, DistinctColors), count(Colors, Color, N)),
		Frequencies
	).

% color_frequencies([card(1, red), card(4, red), card(1, yellow), card(13, blue)], Frequencies) -> Frequencies / [freq(red,2),freq(yellow,1),freq(blue,1)]


cards_ranks_of_color(Cards, Color, Ranks) :- findall(Rank, member(card(Rank, Color), Cards), Ranks).
count_color(Cards, Color, Count) :- cards_ranks_of_color(Cards, Color, Ranks), length(Ranks, Count).


% wants_to_win/lose(+Bids, +Tricks) -> checkers.
wants_to_win(Bids, Tricks) :- Bids > Tricks.
wants_to_lose(Bids, Tricks) :- Bids =< Tricks.

% lowest_card(+Cards, -LowestCard) -> lowest based on card_value.
lowest_card(Cards, LowestCard) :-
	findall(Rank, (member(Card, Cards), card_value(Card, Rank)), Ranks),
	min_max(Ranks, _, MinRank),
	member(LowestCard, Cards),
	card_value(LowestCard, MinRank), !.



% WIZARD STRATEGY -> Strategies for API


% dominant_color(+Cards, -Color) -> return the max color present in a List of card (2 red and 2 yellow -> given in next paths)
dominant_color(Cards, Color) :-
	color_frequencies(Cards, Frequencies),
	findall(N, member(freq(_, N), Frequencies), Counts),
	min_max(Counts, Max, _),
	member(freq(Color, Max), Frequencies).

% dominant_color([card(1, red), card(4, red), card(1, yellow), card(13, blue)], Color) -> Color / red
% dominant_color([card(1, red), card(4, red), card(1, yellow), card(13, yellow)], Color) -> Color / red

% safe_trick(+Card, +Hand, +TrumpColor) -> evaluate if card can be a secure trick
% 	- wizards
safe_trick(wizard, _, _).
% 	- Trump Cards, Rank in range 10 - 13.
safe_trick(card(Rank, TrumpColor), _, TrumpColor) :- range(10, 13, Rank).
% 	- no Trump Cards, 13 Rank (highest)
safe_trick(card(13, Color), _, TrumpColor) :- Color \= TrumpColor.

% ricky_trick(+Card, +Hand, +TrumpColor) -> evaluate if card can be a risky trick (exclude safe_trick: (Rank 13 is already included in safe_trick))
% 	- no Trump Cards, Hand contains >= 5 of the same color, Rank in range 10 - 12.
risky_trick(card(Rank, Color), Hand, TrumpColor) :-
	range(10, 12, Rank),
	Color \= TrumpColor,
	count_color(Hand, Color, Count),
	Count >= 5.
% 	- no Trump Cards, Hand contains only 2 cards of a color, Rank in range 11 - 12.
risky_trick(card(Rank, Color), Hand, TrumpColor) :-
    range(11, 12, Rank),
    Color \= TrumpColor,
    count_color(Hand, Color, Count),
    Count =< 2.


% beats(+MyCard, +WinningCard, +TrumpColor) -> evaluate TrickWinner using MyCard
%		- any cards wins over jester, but not another jester.
beats(NotJester, jester, _) :- !, NotJester \= jester.
%		- wizard wins over all, but only the first one.
beats(wizard, NotWizard, _) :- !, NotWizard \= wizard.
%		- a Trump always beat a not Trump
beats(card(_, TrumpColor), card(_, Color), TrumpColor) :- Color \= TrumpColor.
%		- highest Rank
beats(card(MyCardRank, Color), card(WinningCardRank, Color), _) :- MyCardRank > WinningCardRank.


% winning_options(+PlayableCards, +WinningCard, +TrumpColor, -WinningCards) -> given a Legit List of PlayableCards (playable_cards doc) -> return a List of WinningCards
winning_options(PlayableCards, WinningCard, TrumpColor, WinningCards) :-
	findall(Card, (member(Card, PlayableCards), beats(Card, WinningCard, TrumpColor)), WinningCards).

% winning_options([card(1,red),card(4,yellow),wizard,jester], card(10, yellow), red, WinningCards) -> WinningCards / [card(1,red),wizard]

% losing_options(+PlayableCards, +WinningCard, +TrumpColor, -LosingCards) -> given a Legit List of PlayableCards (playable_cards doc) -> return a List of LosingCards
losing_options(PlayableCards, WinningCard, TrumpColor, LosingCards) :-
	winning_options(PlayableCards, WinningCard, TrumpColor, WinningCards),
	findall(Card, (member(Card, PlayableCards), \+ member(Card, WinningCards)), LosingCards).

% losing_options([card(1,red),card(4,yellow),wizard,jester], card(10, yellow), red, LosingCards) -> LosingCards / [card(4,yellow),jester]


% smart_discard(+PlayableCards, ?TrumpColor, -BestDiscardCard) -> strategy for save jester.
% 	- use the lowest card: not a special or a trump.
smart_discard(PlayableCards, TrumpColor, BestDiscardCard) :-
	findall(Card, (
		member(Card, PlayableCards), 
		Card \= wizard, 
		Card \= jester, 
		\+ card_of_color(Card, TrumpColor)
	), JunkCards),
	JunkCards \= [],
	lowest_card(JunkCards, BestDiscardCard), !.

%		- fallback: use the lowest.
smart_discard(PlayableCards, _, BestDiscardCard) :-
	lowest_card(PlayableCards, BestDiscardCard).


% dangerous_value(+Card, +TrumpColor, -Value) -> convert any Card to a general Value
dangerous_value(jester, _, 0).
% 	- Trump = Rank + 6: ex: 2 of Trump -> 8 of Standard: a 10 Standard is dangerous
dangerous_value(card(Rank, TrumpColor), TrumpColor, Value) :- Value is Rank + 6, !.
% 	- Standard: Rank
dangerous_value(card(Rank, _), _, Rank).
% 	- Wizard: MAX
dangerous_value(wizard, _, 100).

% most_dangerous_card(+Cards, +TrumpColor, -MostDangerous) -> Give the most dangerous card.
most_dangerous_card(Cards, TrumpColor, MostDangerousCard) :-
	findall(Value, (member(Card, Cards), dangerous_value(Card, TrumpColor, Value)), Values),
	min_max(Values, MaxValue, _),
	member(MostDangerousCard, Cards),
	dangerous_value(MostDangerousCard, TrumpColor, MaxValue), !.
