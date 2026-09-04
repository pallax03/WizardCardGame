"use client";

import { useState } from "react";
import { Button } from "@/ui/components/button";
import { Card as UiCard, CardContent, CardHeader, CardTitle } from "@/ui/components/card";
import { cardToString } from "../state/gameReducer";
import type { Card, CardColor } from "../types";

const TRUMP_COLORS: CardColor[] = ["Red", "Yellow", "Green", "Blue"];

interface ManualApiTesterProps {
  selectedCard: Card | null;
  onPlayCard: (c?: Card) => void;
  bidInput: number;
  onSetBidInput: (b: number) => void;
  onPlaceBid: (b?: number) => void;
  selectedColor: CardColor;
  onSetSelectedColor: (c: CardColor) => void;
  onChooseTrump: (c?: CardColor) => void;
  isSubmitting: boolean;
}

export function ManualApiTester({
  selectedCard,
  onPlayCard,
  bidInput,
  onSetBidInput,
  onPlaceBid,
  selectedColor,
  onSetSelectedColor,
  onChooseTrump,
  isSubmitting,
}: ManualApiTesterProps) {
  const [isOpen, setIsOpen] = useState(true);

  return (
    <UiCard className="bg-zinc-900/60 border-zinc-800">
      <CardHeader
        className="pb-2 cursor-pointer flex flex-row items-center justify-between"
        onClick={() => setIsOpen((prev) => !prev)}
      >
        <CardTitle className="text-sm font-semibold uppercase tracking-wider text-zinc-400">
          Direct API Tester / Manual Buttons {isOpen ? "▲" : "▼"}
        </CardTitle>
        <span className="text-xs text-zinc-500">
          Use to test backend calls directly
        </span>
      </CardHeader>
      {isOpen && (
        <CardContent className="space-y-4 pt-2">
          <p className="text-xs text-zinc-400">
            These buttons trigger direct calls to the backend endpoints (`choose`, `place`, `play`)
            using currently selected values or sensible defaults. Useful for testing invalid moves,
            playing out of turn, or completing moves rapidly.
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            {/* Play Card Button */}
            <div className="p-3 bg-zinc-950/60 border border-zinc-800 rounded-xl space-y-2">
              <span className="text-xs font-semibold text-zinc-300">Play Card</span>
              <p className="text-[11px] text-zinc-500 truncate">
                Card: {selectedCard ? cardToString(selectedCard) : "Blue 7 (Default)"}
              </p>
              <Button
                variant="primary"
                size="default"
                className="w-full"
                disabled={isSubmitting}
                onClick={() =>
                  onPlayCard(
                    selectedCard ?? { type: "Standard", color: "Blue", rank: 7 }
                  )
                }
              >
                Play Card
              </Button>
            </div>

            {/* Place Bid Button */}
            <div className="p-3 bg-zinc-950/60 border border-zinc-800 rounded-xl space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-zinc-300">Place Bid</span>
                <input
                  type="number"
                  min={0}
                  max={20}
                  value={bidInput}
                  onChange={(e) => onSetBidInput(Math.max(0, Number(e.target.value)))}
                  className="w-14 bg-zinc-900 border border-zinc-700 rounded px-1.5 py-0.5 text-xs text-white text-right font-mono"
                />
              </div>
              <p className="text-[11px] text-zinc-500">Bid amount: {bidInput}</p>
              <Button
                variant="primary"
                size="default"
                className="w-full"
                disabled={isSubmitting}
                onClick={() => onPlaceBid(bidInput)}
              >
                Place Bid
              </Button>
            </div>

            {/* Choose Trump Color Button */}
            <div className="p-3 bg-zinc-950/60 border border-zinc-800 rounded-xl space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-zinc-300">Choose Trump</span>
                <select
                  value={selectedColor}
                  onChange={(e) => onSetSelectedColor(e.target.value as CardColor)}
                  className="bg-zinc-900 border border-zinc-700 rounded px-1.5 py-0.5 text-xs text-white"
                >
                  {TRUMP_COLORS.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </div>
              <p className="text-[11px] text-zinc-500">Color: {selectedColor}</p>
              <Button
                variant="primary"
                size="default"
                className="w-full"
                disabled={isSubmitting}
                onClick={() => onChooseTrump(selectedColor)}
              >
                Choose Trump Color
              </Button>
            </div>
          </div>
        </CardContent>
      )}
    </UiCard>
  );
}
