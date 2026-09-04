"use client";

import { Button } from "@/ui/components/button";
import { Card as UiCard, CardContent, CardHeader, CardTitle } from "@/ui/components/card";
import { cardToString } from "../state/gameReducer";
import type { Card, CardColor } from "../types";

const TRUMP_COLORS: CardColor[] = ["Red", "Yellow", "Green", "Blue"];

interface GameActionControlsProps {
  isMyTurn: boolean;
  canChooseTrump: boolean;
  canBid: boolean;
  canPlay: boolean;
  round: number;
  selectedColor: CardColor;
  onSelectColor: (c: CardColor) => void;
  onChooseTrump: (c?: CardColor) => void;
  bidInput: number;
  onSelectBid: (b: number) => void;
  onPlaceBid: (b?: number) => void;
  selectedCard: Card | null;
  onPlayCard: (c?: Card) => void;
  isSubmitting: boolean;
}

export function GameActionControls({
  isMyTurn,
  canChooseTrump,
  canBid,
  canPlay,
  round,
  selectedColor,
  onSelectColor,
  onChooseTrump,
  bidInput,
  onSelectBid,
  onPlaceBid,
  selectedCard,
  onPlayCard,
  isSubmitting,
}: GameActionControlsProps) {
  return (
    <UiCard className="bg-zinc-900/80 border-indigo-500/30">
      <CardHeader className="pb-2">
        <CardTitle className="text-sm font-semibold uppercase tracking-wider text-zinc-300">
          Current Action Controls
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* A. Choose Trump Color */}
        {canChooseTrump && (
          <div className="p-4 rounded-xl bg-indigo-950/40 border border-indigo-500/50 space-y-3">
            <p className="text-sm font-semibold text-white">Choose Trump Color:</p>
            <div className="flex flex-wrap gap-2">
              {TRUMP_COLORS.map((color) => (
                <Button
                  key={color}
                  size="lg"
                  variant={selectedColor === color ? "confirming" : "outline"}
                  disabled={isSubmitting}
                  onClick={() => {
                    onSelectColor(color);
                    onChooseTrump(color);
                  }}
                >
                  {color}
                </Button>
              ))}
            </div>
          </div>
        )}

        {/* B. Place Bid */}
        {canBid && (
          <div className="p-4 rounded-xl bg-indigo-950/40 border border-indigo-500/50 space-y-3">
            <p className="text-sm font-semibold text-white">
              Place your Bid for Round {round}:
            </p>
            <div className="flex flex-wrap items-center gap-2">
              {Array.from({ length: round + 1 }, (_, i) => (
                <Button
                  key={i}
                  size="sm"
                  variant={bidInput === i ? "confirming" : "outline"}
                  disabled={isSubmitting}
                  onClick={() => onSelectBid(i)}
                >
                  {i}
                </Button>
              ))}
              <Button
                size="default"
                variant="primary"
                disabled={isSubmitting}
                onClick={() => onPlaceBid(bidInput)}
                className="ml-auto"
              >
                Confirm Bid ({bidInput})
              </Button>
            </div>
          </div>
        )}

        {/* C. Play Card */}
        {canPlay && (
          <div className="p-4 rounded-xl bg-indigo-950/40 border border-indigo-500/50 flex flex-wrap items-center justify-between gap-4">
            <div>
              <p className="text-sm font-semibold text-white">Play Card:</p>
              <p className="text-xs text-zinc-400">
                {selectedCard
                  ? `Selected: ${cardToString(selectedCard)}`
                  : "Click one of the legal cards in your hand above"}
              </p>
            </div>
            <Button
              size="lg"
              variant="primary"
              disabled={!selectedCard || isSubmitting}
              onClick={() => {
                if (selectedCard) onPlayCard(selectedCard);
              }}
            >
              Play Selected Card
            </Button>
          </div>
        )}

        {!isMyTurn && (
          <p className="text-xs text-zinc-500 italic">
            Controls will activate when it is your turn.
          </p>
        )}
      </CardContent>
    </UiCard>
  );
}
