% WIZARD API -> Thin deterministic facade exposed to WizardPrologEngine

% choose_trump(+Hand, -TrumpColor)
choose_trump(Hand, TrumpColor) :-
    strategy_choose_trump(Hand, TrumpColor), !.

% place_bid(+Hand, +TrumpColor, -Bid)
place_bid(Hand, TrumpColor, Bid) :-
    strategy_place_bid(Hand, TrumpColor, Bid), !.

% adjust_bid(+Hand, +RejectedBid, -FinalBid)
adjust_bid(Hand, RejectedBid, FinalBid) :-
    strategy_adjust_bid(Hand, RejectedBid, FinalBid), !.

% best_playable_card(+Hand, +WinningCard, +FollowingColor, +TrumpColor, +Bids, +Tricks, -BestCard)
best_playable_card(Hand, WinningCard, FollowingColor, TrumpColor, Bids, Tricks, BestCard) :-
    strategy_best_card(Hand, WinningCard, FollowingColor, TrumpColor, Bids, Tricks, BestCard), !.
