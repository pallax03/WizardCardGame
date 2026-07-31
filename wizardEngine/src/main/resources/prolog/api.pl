% WIZARD API

% choose_trump(+Hand, -TrumpColor) -> return the best trump to choose based on dominant_color (see STRATEGY.dominant_color)
choose_trump(_, TrumpColor) :- is_valid_color(TrumpColor).
choose_trump(Hand, TrumpColor) :- dominant_color(Hand, TrumpColor), is_valid_color(TrumpColor).

% choose_trump([card(1, yellow), card(1, red), wizard], TrumpColor). -> TrumpColor / yellow.
% choose_trump([wizard, jester, wizard], TrumpColor). -> TrumpColor \ ? (just pick it in order of is_valid_color)


% place_bid(+Hand, ?TrumpColor, -Bid) -> return the best Bid based on STRATEGY: cards matching safe_trick OR risky_trick: add a Bid
place_bid(Hand, TrumpColor, Bid) :-
    findall(Card, (member(Card, Hand), (safe_trick(Card, Hand, TrumpColor) ; risky_trick(Card, Hand, TrumpColor))), Cards),
    length(Cards, Bid).

% place_bid([card(1, red), jester, card(4, red), wizard, card(12, yellow), card(13, blue), wizard, jester, wizard], yellow, Bid) -> Bid / 5


% To call when place_bid return an invalid number.
% adjust_bid(+Hand, +RejectedBid, -FinalBid) -> + 1 (fallback) or - 1 (if Hand has jesters) from RejectingBid (avoiding loop)
adjust_bid(Hand, 0, 1) :- !.
adjust_bid(Hand, RejectedBid, FinalBid) :- length(Hand, MaxSize), RejectedBid >= MaxSize, FinalBid is MaxSize - 1, !.
adjust_bid(Hand, RejectedBid, FinalBid) :- member(jester, Hand), FinalBid is RejectedBid - 1, !.
adjust_bid(Hand, RejectedBid, FinalBid) :- FinalBid is RejectedBid + 1, !.

% loop: adjust_bid([card(1, red), card(4, red), wizard, card(12, yellow), card(13, blue), wizard, wizard], 10, Bid) -> Bid / 6
% fallback: adjust_bid([card(1, red), card(4, red), wizard, card(12, yellow), card(13, blue), wizard, wizard], 7, Bid) -> Bid / 6
% jesters: adjust_bid([card(1, red), jester, card(4, red), wizard, card(12, yellow), card(13, blue), wizard, jester, wizard], 9, Bid) -> Bid / 8


% best_playable_card(+Hand, +WinningCard, +FollowingColor, +TrumpColor, +Bids, +Tricks, -BestCard) -> return the best card to play.

% - WinningCard REQUIRED
% 	- want to win with winning cards:
%			- play the lowest to win
best_playable_card(Hand, WinningCard, FollowingColor, TrumpColor, Bids, Tricks, BestCard) :-
	validate_cards([WinningCard]),
	wants_to_win(Bids, Tricks),
	playable_cards(Hand, FollowingColor, PlayableCards),
	winning_options(PlayableCards, WinningCard, TrumpColor, WinningCards),
	WinningCards \= [],
	lowest_card(WinningCards, BestCard), !.

% - WinningCard REQUIRED
% 	- want to win without winning cards: 
%			- play a jester 0% win ofor next tricks
best_playable_card(Hand, WinningCard, FollowingColor, TrumpColor, Bids, Tricks, jester) :-
	wants_to_win(Bids, Tricks),
	validate_cards([WinningCard]),
	playable_cards(Hand, FollowingColor, PlayableCards),
	winning_options(PlayableCards, WinningCard, TrumpColor, WinningCards),
	WinningCards == [],
	member(jester, PlayableCards), !.
%	fallback:	- play a smart discard to preserve high cards
best_playable_card(Hand, WinningCard, FollowingColor, TrumpColor, Bids, Tricks, BestCard) :-
	wants_to_win(Bids, Tricks),
	validate_cards([WinningCard]),
	playable_cards(Hand, FollowingColor, PlayableCards),
	winning_options(PlayableCards, WinningCard, TrumpColor, WinningCards),
	WinningCards == [],
	smart_discard(PlayableCards, TrumpColor, BestCard), !.

% - WinningCard REQUIRED
% 	- want to lose but every playable card wins:
%			- play the most dangerous card to minimize trick won
best_playable_card(Hand, WinningCard, FollowingColor, TrumpColor, Bids, Tricks, BestCard) :-
	wants_to_lose(Bids, Tricks),
	validate_cards([WinningCard]),
	playable_cards(Hand, FollowingColor, PlayableCards),
	losing_options(PlayableCards, WinningCard, TrumpColor, LosingCards),
	LosingCards == [],
	most_dangerous_card(PlayableCards, TrumpColor, BestCard), !.

% - WinningCard REQUIRED
% 	- want to lose:
%			- play the most dangerous card among losing options to maximize win on next tricks
best_playable_card(Hand, WinningCard, FollowingColor, TrumpColor, Bids, Tricks, BestCard) :-
	wants_to_lose(Bids, Tricks),
	validate_cards([WinningCard]),
	playable_cards(Hand, FollowingColor, PlayableCards),
	losing_options(PlayableCards, WinningCard, TrumpColor, LosingCards),
	LosingCards \= [],
	most_dangerous_card(LosingCards, TrumpColor, BestCard), !.

% - WinningCard NOT REQUIRED (Opening)
% 	- want to lose:
%			- play the smartest, least dangerous discard to slide under opponents
best_playable_card(Hand, _, none, TrumpColor, Bids, Tricks, BestCard) :-
	wants_to_lose(Bids, Tricks),
	smart_discard(Hand, TrumpColor, BestCard), !.

% - WinningCard NOT REQUIRED (Opening)
% 	- want to win:
%			- play the lowest among your safe/risky tricks
best_playable_card(Hand, _, none, TrumpColor, Bids, Tricks, BestCard) :-
	wants_to_win(Bids, Tricks),
	findall(Card, (member(Card, Hand), (safe_trick(Card, Hand, TrumpColor); risky_trick(Card, Hand, TrumpColor))), PlayableCards),
	PlayableCards \= [],
	lowest_card(PlayableCards, BestCard), !.


% - WinningCard NOT REQUIRED (Opening)
% 	- want to win: (fallback for when you have no safe or risky tricks)
%			- play the most dangerous to maximize trick won
best_playable_card(Hand, _, none, TrumpColor, Bids, Tricks, BestCard) :-
	wants_to_win(Bids, Tricks),
	most_dangerous_card(Hand, TrumpColor, BestCard), !.

% Cuts keep the final decision API deterministic once a strategy clause matches.
% WANTS TO WIN:
% 	best_playable_card([card(12, red), wizard, card(10, red), wizard], card(10, yellow), yellow, blue, 5, 3, BestCard) -> BestCard / wizard
%		best_playable_card([card(1, blue), card(12, red), wizard, card(10, red), wizard], none, none, red, 5, 3, BestCard) -> BestCard / card(10,red)


% WANTS TO LOSE:
%	 best_playable_card([card(12, red), wizard, card(10, red), wizard], wizard, yellow, red, 2, 3, BestCard) -> BestCard / wizard
%  best_playable_card([card(1, red), card(12, yellow)], card(10, yellow), yellow, blue, 0, 0, BestCard) -> BestCard / card(12,yellow)
