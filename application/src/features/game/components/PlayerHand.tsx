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
    <UiCard className="bg-zinc-950/80 border-amber-500/40 backdrop-blur-md shadow-2xl">
      <CardHeader className="p-3 pb-2 flex flex-row items-center justify-between border-b border-zinc-800/60">
        <CardTitle className="text-xs font-black uppercase tracking-widest text-amber-400 flex items-center gap-2">
          <span>🃏</span> La Tua Mano ({hand.length} carte)
        </CardTitle>
        {canPlay && (
          <span className="text-xs text-amber-300 font-bold animate-pulse">
            Seleziona la carta da giocare
          </span>
        )}
      </CardHeader>
      <CardContent className="p-3">
        {hand.length > 0 ? (
          <div className="flex flex-wrap gap-2 sm:gap-3 items-center justify-center p-3 bg-zinc-900/60 rounded-xl border border-zinc-800/80 min-h-[140px]">
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
          <div className="text-center py-8 text-zinc-500 text-xs">
            Mano vuota (in attesa delle carte del prossimo round)
          </div>
        )}
      </CardContent>
    </UiCard>
  );
}