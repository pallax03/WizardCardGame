"use client";

import { Button } from "@/ui/components/button";
import { Card as UiCard, CardContent, CardHeader, CardTitle } from "@/ui/components/card";
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
  /** Puntata vietata dalla regola somma != round (ultimo bidder). */
  forbiddenBid?: number | null;
  /** Somma delle bid già piazzate nel round, per spiegare il divieto. */
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
  return (
    <UiCard className="bg-zinc-950/80 border-amber-500/40 backdrop-blur-md shadow-2xl h-full">
      <CardHeader className="p-3 pb-2 border-b border-zinc-800/60">
        <CardTitle className="text-xs font-black uppercase tracking-widest text-amber-400">
          Pulsantiera Azioni
        </CardTitle>
      </CardHeader>
      <CardContent className="p-3 space-y-3">
        {/* Scelta Colore Briscola */}
        {canChooseTrump && (
          <div className="p-3 rounded-xl bg-amber-950/30 border border-amber-500/40 space-y-2">
            <p className="text-xs font-bold text-white">Scegli Colore Briscola:</p>
            <div className="grid grid-cols-2 gap-2">
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
                  className="w-full font-bold text-xs"
                >
                  {color}
                </Button>
              ))}
            </div>
          </div>
        )}

        {/* Inserimento Puntata / Bid */}
        {canBid && (
          <div className="p-3 rounded-xl bg-amber-950/30 border border-amber-500/40 space-y-2">
            <p className="text-xs font-bold text-white">
              Puntata Round {round}:
            </p>
            <div className="flex flex-wrap gap-1.5 justify-center mb-2">
              {Array.from({ length: round + 1 }, (_, i) => {
                const isForbidden = forbiddenBid !== null && forbiddenBid !== undefined && i === forbiddenBid;
                return (
                  <Button
                    key={i}
                    size="sm"
                    variant={bidInput === i ? "confirming" : "outline"}
                    disabled={isSubmitting || isForbidden}
                    onClick={() => onSelectBid(i)}
                    title={
                      isForbidden
                        ? `Puntata non valida: con ${bidsTotal ?? 0} già puntati, ${bidsTotal ?? 0} + ${i} pareggerebbe le carte del Round ${round}`
                        : `Punta ${i}`
                    }
                    className={`w-8 h-8 p-0 text-xs font-mono font-bold ${isForbidden ? "opacity-40 line-through" : ""}`}
                  >
                    {i}
                  </Button>
                );
              })}
            </div>
            {forbiddenBid !== null && forbiddenBid !== undefined && (
              <p className="text-[11px] font-semibold text-amber-200/90 bg-amber-950/60 border border-amber-500/40 rounded-lg px-2 py-1 text-center">
                🚫 La puntata {forbiddenBid} non è valida: la somma delle puntate non può essere
                uguale al numero di carte ({round}).
              </p>
            )}
            <Button
              size="default"
              variant="primary"
              disabled={
                isSubmitting ||
                (forbiddenBid !== null && forbiddenBid !== undefined && bidInput === forbiddenBid)
              }
              onClick={() => onPlaceBid(bidInput)}
              className="w-full font-bold bg-amber-500 hover:bg-amber-400 text-zinc-950 shadow-lg disabled:opacity-40"
            >
              Conferma Puntata ({bidInput})
            </Button>
          </div>
        )}

        {/* Gioca Carta: drag & drop sul tavolo (doppio click come fallback) */}
        {canPlay && (
          <div className="p-4 text-center text-xs font-semibold text-amber-200/90 bg-amber-950/30 rounded-xl border border-amber-500/40">
            🃏 Trascina una carta sul tavolo per giocarla
            <span className="mt-1 block text-[11px] font-normal text-amber-200/60">
              (oppure fai doppio click sulla carta)
            </span>
          </div>
        )}

        {!isMyTurn && (
          <div className="p-4 text-center text-xs text-zinc-500 italic bg-zinc-900/40 rounded-xl border border-zinc-800">
            Attendi il tuo turno per abilitare i comandi di gioco.
          </div>
        )}
      </CardContent>
    </UiCard>
  );
}