"use client";

import { Badge } from "@/ui/components/badge";
import { Card as UiCard, CardContent, CardHeader, CardTitle } from "@/ui/components/card";
import { GameCardView } from "./GameCardView";
import { cardEquals, cardToString } from "../state/gameReducer";
import type { Card, CardColor, PlayedCardEntry } from "../types";

interface TrickTableProps {
  table: PlayedCardEntry[];
  playersMap: Map<number, { name: string }>;
  myPlayerId: number;
  winningCard: Card | null;
  followingColor: CardColor | null;
  lastTrick: { winnerId: number; cards: Card[]; tricksWon: number } | null;
}

export function TrickTable({
  table,
  playersMap,
  myPlayerId,
  winningCard,
  followingColor,
  lastTrick,
}: TrickTableProps) {
  return (
    <UiCard className="bg-zinc-900/60 border-zinc-800">
      <CardHeader className="pb-2 flex flex-row items-center justify-between">
        <CardTitle className="text-sm font-semibold uppercase tracking-wider text-zinc-400">
          Trick Table
        </CardTitle>
        {followingColor && (
          <Badge variant="outline" className="text-xs">
            Lead Color: {followingColor}
          </Badge>
        )}
      </CardHeader>
      <CardContent>
        {table.length > 0 ? (
          <div className="flex flex-wrap gap-4 items-center justify-center min-h-36 p-4 bg-zinc-950/40 rounded-xl border border-zinc-800/80">
            {table.map((entry, index) => {
              const playerName =
                playersMap.get(entry.playerId)?.name ?? `Player ${entry.playerId}`;
              const isWinning =
                winningCard && cardEquals(entry.card, winningCard);

              return (
                <div key={index} className="flex flex-col items-center gap-1.5">
                  <span className="text-xs font-semibold text-zinc-300">
                    {playerName} {entry.playerId === myPlayerId && "(You)"}
                  </span>
                  <GameCardView card={entry.card} size="md" isClickable={false} />
                  {isWinning && (
                    <span className="text-[10px] bg-amber-500/20 text-amber-300 border border-amber-500/40 px-1.5 py-0.5 rounded font-bold">
                      Winning Card
                    </span>
                  )}
                </div>
              );
            })}
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-10 bg-zinc-950/20 rounded-xl border border-dashed border-zinc-800 text-zinc-500 text-sm">
            <span>No cards played in this trick yet</span>
            {lastTrick && (
              <div className="mt-2 text-xs text-zinc-400 text-center">
                <span>
                  Last trick won by{" "}
                  <strong>
                    {playersMap.get(lastTrick.winnerId)?.name ??
                      `Player ${lastTrick.winnerId}`}
                  </strong>{" "}
                  ({lastTrick.cards.map(cardToString).join(", ")})
                </span>
              </div>
            )}
          </div>
        )}
      </CardContent>
    </UiCard>
  );
}
