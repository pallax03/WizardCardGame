"use client";

import { Card as UiCard, CardContent, CardHeader, CardTitle } from "@/ui/components/card";
import { GameCardView } from "./GameCardView";
import { cardEquals } from "../state/gameReducer";
import type { Card } from "../types";

interface PlayerHandProps {
  hand: Card[];
  selectedCard: Card | null;
  canPlay: boolean;
  isCardPlayable: (card: Card) => boolean;
  onSelectCard: (card: Card | null) => void;
}

export function PlayerHand({
  hand,
  selectedCard,
  canPlay,
  isCardPlayable,
  onSelectCard,
}: PlayerHandProps) {
  return (
    <UiCard className="bg-zinc-900/60 border-zinc-800">
      <CardHeader className="pb-2 flex flex-row items-center justify-between">
        <CardTitle className="text-sm font-semibold uppercase tracking-wider text-zinc-400">
          Your Hand ({hand.length} cards)
        </CardTitle>
        {canPlay && (
          <span className="text-xs text-indigo-400 font-semibold animate-pulse">
            Select a card to play
          </span>
        )}
      </CardHeader>
      <CardContent>
        {hand.length > 0 ? (
          <div className="flex flex-wrap gap-3 items-center justify-center p-4 bg-zinc-950/40 rounded-xl border border-zinc-800/80 min-h-36">
            {hand.map((card, index) => {
              const isSelected = cardEquals(card, selectedCard);
              const isLegal = isCardPlayable(card);

              return (
                <GameCardView
                  key={index}
                  card={card}
                  size="lg"
                  isSelected={isSelected}
                  isLegal={canPlay ? isLegal : true}
                  isClickable={canPlay && isLegal}
                  onClick={() => {
                    if (canPlay && isLegal) {
                      onSelectCard(isSelected ? null : card);
                    }
                  }}
                />
              );
            })}
          </div>
        ) : (
          <div className="text-center py-8 text-zinc-500 text-sm">
            Your hand is empty (waiting for next round cards)
          </div>
        )}
      </CardContent>
    </UiCard>
  );
}
