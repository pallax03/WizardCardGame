"use client";

import { Button } from "@/ui/components/button";
import type { CardColor } from "../types";

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
  isSubmitting: boolean;
  forbiddenBid?: number | null;
  bidsTotal?: number;
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
  isSubmitting,
  forbiddenBid,
  bidsTotal,
}: GameActionControlsProps) {
  if (!isMyTurn) return null;

  return (
    <div className="w-full bg-zinc-950/90 border border-amber-500/40 rounded-2xl p-2.5 backdrop-blur-md shadow-xl space-y-2">
      {/* Scelta Briscola */}
      {canChooseTrump && (
        <div className="p-2 rounded-xl bg-amber-950/40 border border-amber-500/40 space-y-1.5">
          <p className="text-[11px] font-bold text-amber-300 text-center">Scegli Briscola:</p>
          <div className="grid grid-cols-4 gap-1.5">
            {TRUMP_COLORS.map((color) => (
              <Button
                key={color}
                size="sm"
                variant={selectedColor === color ? "confirming" : "outline"}
                disabled={isSubmitting}
                onClick={() => {
                  onSelectColor(color);
                  onChooseTrump(color);
                }}
                className="w-full font-bold text-[10px] py-1 px-0 h-8"
              >
                {color}
              </Button>
            ))}
          </div>
        </div>
      )}

      {/* Scelta Puntata (Bid) */}
      {canBid && (
        <div className="p-2 rounded-xl bg-amber-950/40 border border-amber-500/40 space-y-2">
          <p className="text-[11px] font-bold text-amber-300 text-center">
            Puntata Round {round}:
          </p>
          <div className="flex flex-wrap gap-1 justify-center max-h-24 overflow-y-auto">
            {Array.from({ length: round + 1 }, (_, i) => {
              const isForbidden = forbiddenBid !== null && forbiddenBid !== undefined && i === forbiddenBid;
              return (
                <Button
                  key={i}
                  size="sm"
                  variant={bidInput === i ? "confirming" : "outline"}
                  disabled={isSubmitting || isForbidden}
                  onClick={() => onSelectBid(i)}
                  className={`size-7 p-0 text-xs font-mono font-bold ${isForbidden ? "opacity-30 line-through" : ""}`}
                >
                  {i}
                </Button>
              );
            })}
          </div>
          {forbiddenBid !== null && forbiddenBid !== undefined && (
            <p className="text-[10px] font-semibold text-amber-200/90 bg-amber-950/60 border border-amber-500/40 rounded px-2 py-0.5 text-center">
              🚫 {forbiddenBid} vietata (somma != {round})
            </p>
          )}
          <Button
            size="sm"
            variant="primary"
            disabled={
              isSubmitting ||
              (forbiddenBid !== null && forbiddenBid !== undefined && bidInput === forbiddenBid)
            }
            onClick={() => onPlaceBid(bidInput)}
            className="w-full font-bold text-xs bg-amber-500 hover:bg-amber-400 text-zinc-950 shadow"
          >
            Conferma Puntata ({bidInput})
          </Button>
        </div>
      )}
    </div>
  );
}