"use client";

import { Button } from "@/ui/components/button";
import { Lightbulb } from "lucide-react";
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
  hintedBid?: number | null;
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
  hintedBid = null,
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
          <div className="flex flex-wrap gap-1 justify-center max-h-24 overflow-y-auto p-4">
            {Array.from({ length: round + 1 }, (_, i) => {
              const isForbidden = forbiddenBid !== null && forbiddenBid !== undefined && i === forbiddenBid;
              const isHinted = hintedBid === i && bidInput === i;
              return (
                <Button
                  key={i}
                  size="sm"
                  variant={bidInput === i ? "confirming" : "outline"}
                  disabled={isSubmitting || isForbidden}
                  onClick={() => onSelectBid(i)}
                  aria-label={isHinted ? `Puntata ${i} suggerita` : `Puntata ${i}`}
                  title={isHinted ? "Puntata suggerita dall'AI" : undefined}
                  className={`relative size-7 p-0 text-xs font-mono font-bold ${
                    isForbidden ? "opacity-30 line-through" : ""
                  } ${
                    isHinted
                      ? "bg-cyan-400 text-zinc-950 border-cyan-200 ring-2 ring-cyan-300 shadow-lg shadow-cyan-500/50 scale-110 hover:bg-cyan-300"
                      : ""
                  }`}
                >
                  {i}
                  {isHinted && (
                    <span className="absolute -top-2 -right-2 grid size-4 place-items-center rounded-full border border-cyan-200 bg-cyan-500 text-[9px] shadow-md shadow-cyan-500/50">
                      <Lightbulb className="size-2.5 fill-current" />
                    </span>
                  )}
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