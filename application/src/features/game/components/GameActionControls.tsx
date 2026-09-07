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
              {Array.from({ length: round + 1 }, (_, i) => (
                <Button
                  key={i}
                  size="sm"
                  variant={bidInput === i ? "confirming" : "outline"}
                  disabled={isSubmitting}
                  onClick={() => onSelectBid(i)}
                  className="w-8 h-8 p-0 text-xs font-mono font-bold"
                >
                  {i}
                </Button>
              ))}
            </div>
            <Button
              size="default"
              variant="primary"
              disabled={isSubmitting}
              onClick={() => onPlaceBid(bidInput)}
              className="w-full font-bold bg-amber-500 hover:bg-amber-400 text-zinc-950 shadow-lg"
            >
              Conferma Puntata ({bidInput})
            </Button>
          </div>
        )}

        {/* Gioca Carta */}
        {canPlay && (
          <div className="p-3 rounded-xl bg-amber-950/30 border border-amber-500/40 space-y-2">
            <p className="text-xs font-bold text-white">Carta Selezionata:</p>
            <p className="text-[11px] text-amber-200/80 font-mono truncate">
              {selectedCard ? cardToString(selectedCard) : "Seleziona una carta dalla mano"}
            </p>
            <Button
              size="lg"
              variant="primary"
              disabled={!selectedCard || isSubmitting}
              onClick={() => {
                if (selectedCard) onPlayCard(selectedCard);
              }}
              className="w-full font-black bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 hover:to-amber-500 text-zinc-950 shadow-xl"
            >
              GIOCA CARTA
            </Button>
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