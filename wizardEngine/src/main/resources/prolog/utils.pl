% UTILS
range(X, _, X).
range(A, B, X) :- A2 is A+1, A2 =< B, range(A2, B, X).

count(List, E, N) :- count(List, E, 0, N).
count([], _, N, N).
count([E | T], E, Acc, N) :- Acc2 is Acc + 1, count(T, E, Acc2, N).
count([H | T], E, Acc, N) :- H \= E, count(T, E, Acc, N).

min_max([H], H, H).
min_max([H | T], H, Min) :- min_max(T, Mx, Min), H > Mx, !.
min_max([H | T], Max, H) :- min_max(T, Max, Mn), H < Mn, !.
min_max([H | T], Max, Min) :- min_max(T, Max, Min).

length([], 0).
length([_|T], N) :- length(T, N1), N is N1 + 1.

distinct(List, O) :- distinct(List, [], O).
distinct([], _, []).
distinct([H|T], Seen, O) :- member(H, Seen), !, distinct(T, Seen, O).
distinct([H|T], Seen, [H|O]) :- distinct(T, [H|Seen], O).
