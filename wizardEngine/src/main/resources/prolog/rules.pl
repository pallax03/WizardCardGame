% WIZARD ENGINE.RULES -> Standard Rules of the Game

% following_standard_cards(+Hand, +FollowingColor, -LegalStandardCards)
following_standard_cards(Hand, FollowingColor, LegalStandardCards) :-
	findall(Card, (member(Card, Hand), card_of_color(Card, FollowingColor)), LegalStandardCards).

% following_standard_cards([card(1, red), card(4, red), card(1, yellow), card(13, blue), wizard, jester], red, L) -> L / [card(1,red),card(4,red)]
% following_standard_cards([card(1, red), card(4, red), card(1, yellow), card(13, blue), wizard, jester], jester, L) -> L / []

% special_cards(+Hand, -Specials)
special_cards(HAND, S) :- findall(E, (member(E, HAND), (E = wizard ; E = jester)), S).
% special_cards([wizard, jester, card(13, green), wizard, wizard, jester, jester, card(1, red)], S) -> S / [wizard,jester,wizard,wizard,jester,jester]

% playable_cards(+Hand, ?FollowingColor, -PlayableCards) -> return a List of Playable Cards from Hand.
playable_cards(Hand, FollowingColor, Hand) :- following_standard_cards(Hand, FollowingColor, []), !.
playable_cards(Hand, FollowingColor, PlayableCards) :-
	following_standard_cards(Hand, FollowingColor, FollowingStandardCards),
	special_cards(Hand, SpecialCards),
	append(FollowingStandardCards, SpecialCards, PlayableCards).

% playable_cards([card(1, red), card(4, red), card(1, yellow), card(13, blue), wizard, jester], red, L) -> L / [card(1,red),card(4,red),wizard,jester]
% playable_cards([card(1, red), card(4, red), card(1, yellow), card(13, blue), wizard, jester], none, L) -> L / [card(1,red),card(4,red),card(1,yellow),card(13,blue),wizard,jester]
