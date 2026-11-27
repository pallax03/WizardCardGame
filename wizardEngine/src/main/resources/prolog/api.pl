% WIZARD API -> Thin deterministic facade exposed to WizardPrologEngine

% choose_trump(+Hand, -TrumpColor)
choose_trump(Hand, TrumpColor) :-
    strategy_choose_trump(Hand, TrumpColor), !.

% place_bid(+Hand, +TrumpColor, +TotalPlayers, +BidsPlaced, +OtherBidsSum, -Bid)
place_bid(Hand, TrumpColor, TotalPlayers, BidsPlaced, OtherBidsSum, Bid) :-
    strategy_place_bid(Hand, TrumpColor, TotalPlayers, BidsPlaced, OtherBidsSum, Bid), !.

% place_bid(+Hand, +TrumpColor, -Bid) -> legacy compatibility
place_bid(Hand, TrumpColor, Bid) :-
    place_bid(Hand, TrumpColor, 4, 0, 0, Bid).

% adjust_bid(+Hand, +TrumpColor, +TotalPlayers, +BidsPlaced, +OtherBidsSum, +RejectedBid, -FinalBid)
adjust_bid(Hand, TrumpColor, TotalPlayers, BidsPlaced, OtherBidsSum, RejectedBid, FinalBid) :-
    strategy_adjust_bid(Hand, TrumpColor, TotalPlayers, BidsPlaced, OtherBidsSum, RejectedBid, FinalBid), !.

% adjust_bid(+Hand, +RejectedBid, -FinalBid) -> legacy compatibility
adjust_bid(Hand, RejectedBid, FinalBid) :-
    adjust_bid(Hand, none, 4, 0, 0, RejectedBid, FinalBid).

% best_playable_card(+Hand, +TableCards, +WinningCard, +FollowingColor, +TrumpColor, +Bids, +Tricks, +RemainingPlayers, +TableOverbid, -BestCard)
best_playable_card(Hand, TableCards, WinningCard, FollowingColor, TrumpColor, Bids, Tricks, RemainingPlayers, TableOverbid, BestCard) :-
    strategy_best_card(Hand, TableCards, WinningCard, FollowingColor, TrumpColor, Bids, Tricks, RemainingPlayers, TableOverbid, BestCard), !.

% best_playable_card(+Hand, +WinningCard, +FollowingColor, +TrumpColor, +Bids, +Tricks, -BestCard) -> legacy compatibility
best_playable_card(Hand, WinningCard, FollowingColor, TrumpColor, Bids, Tricks, BestCard) :-
    best_playable_card(Hand, [], WinningCard, FollowingColor, TrumpColor, Bids, Tricks, 0, 0, BestCard).
