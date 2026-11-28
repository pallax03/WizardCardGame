% WIZARD UTILS -> Pure O(1) / O(N) tail-recursive helpers missing from tuProlog BasicLibrary

% in_range(+Low, +High, +Value) -> deterministic O(1) integer bounds check
in_range(Low, High, Value) :-
    integer(Value),
    Value >= Low,
    Value =< High.

% max_scored(+Pairs, -BestItem) -> O(N) selection of item with highest Score in pair(Score, Item)
max_scored([pair(Score, Item) | Rest], BestItem) :-
    max_scored_acc(Rest, Score, Item, BestItem).

max_scored_acc([], _, BestItem, BestItem).
max_scored_acc([pair(Score, Item) | Rest], BestScore, _, BestItem) :-
    Score > BestScore, !,
    max_scored_acc(Rest, Score, Item, BestItem).
max_scored_acc([_ | Rest], BestScore, CurrentItem, BestItem) :-
    max_scored_acc(Rest, BestScore, CurrentItem, BestItem).

% min_scored(+Pairs, -BestItem) -> O(N) selection of item with lowest Score in pair(Score, Item)
min_scored([pair(Score, Item) | Rest], BestItem) :-
    min_scored_acc(Rest, Score, Item, BestItem).

min_scored_acc([], _, BestItem, BestItem).
min_scored_acc([pair(Score, Item) | Rest], BestScore, _, BestItem) :-
    Score < BestScore, !,
    min_scored_acc(Rest, Score, Item, BestItem).
min_scored_acc([_ | Rest], BestScore, CurrentItem, BestItem) :-
    min_scored_acc(Rest, BestScore, CurrentItem, BestItem).

% sum_list(+Numbers, -Sum) -> tail-recursive O(N) integer sum
sum_list(List, Sum) :-
    sum_list_acc(List, 0, Sum).

sum_list_acc([], Acc, Acc).
sum_list_acc([H | T], Acc, Sum) :-
    NextAcc is Acc + H,
    sum_list_acc(T, NextAcc, Sum).

% clamp(+Min, +Max, +Value, -Clamped) -> deterministic O(1) bounds clamping
clamp(Min, _, Value, Min) :- Value < Min, !.
clamp(_, Max, Value, Max) :- Value > Max, !.
clamp(_, _, Value, Value).
