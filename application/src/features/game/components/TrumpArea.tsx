"use client";

import { Card as UiCard, CardContent, CardHeader, CardTitle } from "@/ui/components/card";
import { GameCardView } from "./GameCardView";
import type { CardColor, Trump } from "../types";

interface TrumpAreaProps {
  trump: Trump | null;
  effectiveTrumpColor: CardColor | null;
}

const colorBadgeStyles: Record<CardColor, string> = {
  Red: "bg-rose-950/90 text-rose-200 border-rose-600 shadow-rose-950/50",
  Blue: "bg-blue-950/90 text-blue-200 border-blue-600 shadow-blue-950/50",
  Green: "bg-emerald-950/90 text-emerald-200 border-emerald-600 shadow-emerald-950/50",
  Yellow: "bg-amber-950/90 text-amber-200 border-amber-500 shadow-amber-950/50",
};

export function TrumpArea({ trump, effectiveTrumpColor }: TrumpAreaProps) {
  return (
    <UiCard className="bg-zinc-950/80 border-amber-500/40 backdrop-blur-md shadow-2xl">
      <CardHeader className="p-3 pb-1 text-center border-b border-zinc-800/60">
        <CardTitle className="text-xs font-black uppercase tracking-widest text-amber-400">
          👑 Briscola
        </CardTitle>
      </CardHeader>
      <CardContent className="p-3 flex flex-col items-center justify-center gap-2">
        {trump ? (
          <div className="flex flex-col items-center gap-2">
            {"card" in trump && trump.card && (
              <GameCardView card={trump.card} size="sm" isClickable={false} />
            )}
            <div className="text-xs text-center text-zinc-200 font-medium">
              <p className="font-bold text-[11px] uppercase tracking-wide text-zinc-400">
                Tipo: {trump.type}
              </p>
              {effectiveTrumpColor && (
                <span
                  className={`inline-block mt-1 px-3 py-0.5 rounded-full font-black text-[11px] border shadow-md uppercase tracking-wider ${colorBadgeStyles[effectiveTrumpColor]}`}
                >
                  Colore: {effectiveTrumpColor}
                </span>
              )}
            </div>
          </div>
        ) : (
          <p className="text-zinc-500 text-xs italic py-4">In attesa della briscola...</p>
        )}
      </CardContent>
    </UiCard>
  );
}