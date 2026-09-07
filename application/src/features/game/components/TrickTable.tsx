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
    <UiCard className="bg-zinc-950/80 border-emerald-500/30 backdrop-blur-md shadow-2xl w-full">
      <CardHeader className="p-3 pb-2 flex flex-row items-center justify-between border-b border-zinc-800/60">
        <CardTitle className="text-xs font-black uppercase tracking-widest text-emerald-400 flex items-center gap-1.5">
          <span>♦</span> Mano sul Tavolo
        </CardTitle>
        {followingColor && (
          <Badge variant="outline" className="text-[10px] bg-emerald-950/60 border-emerald-600 text-emerald-300 font-bold">
            Palo: {followingColor}
          </Badge>
        )}
      </CardHeader>
      <CardContent className="p-3">
        {table.length > 0 ? (
          <div className="flex flex-wrap gap-3 sm:gap-5 items-center justify-center min-h-[120px] p-3 bg-emerald-950/30 rounded-xl border border-emerald-800/40">
            {table.map((entry, index) => {
              const playerName =
                playersMap.get(entry.playerId)?.name ?? `Giocatore ${entry.playerId}`;
              const isWinning =
                winningCard && cardEquals(entry.card, winningCard);

              return (
                <div key={index} className="flex flex-col items-center gap-1">
                  <span className="text-[11px] font-bold text-zinc-200 bg-zinc-900/90 px-2 py-0.5 rounded border border-zinc-700/80 shadow">
                    {playerName} {entry.playerId === myPlayerId && "(Tu)"}
                  </span>
                  <GameCardView card={entry.card} size="md" isClickable={false} />
                  {isWinning && (
                    <span className="text-[9px] bg-amber-500/20 text-amber-300 border border-amber-400/60 px-1.5 py-0.5 rounded font-black uppercase tracking-wider animate-pulse">
                      ★ In testa
                    </span>
                  )}
                </div>
              );
            })}
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-6 bg-zinc-900/40 rounded-xl border border-dashed border-zinc-800 text-zinc-500 text-xs">
            <span>Nessuna carta giocata in questa mano</span>
            {lastTrick && (
              <div className="mt-2 text-[11px] text-zinc-400 text-center font-mono">
                <span>
                  Ultima mano vinta da{" "}
                  <strong className="text-amber-400">
                    {playersMap.get(lastTrick.winnerId)?.name ??
                      `Giocatore ${lastTrick.winnerId}`}
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