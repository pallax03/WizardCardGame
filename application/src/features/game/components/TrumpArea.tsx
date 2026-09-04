"use client";

import { Card as UiCard, CardContent, CardHeader, CardTitle } from "@/ui/components/card";
import { GameCardView } from "./GameCardView";
import type { CardColor, Trump } from "../types";

interface TrumpAreaProps {
  trump: Trump | null;
  effectiveTrumpColor: CardColor | null;
}

const colorBadgeStyles: Record<CardColor, string> = {
  Red: "bg-rose-900 text-rose-200 border-rose-700",
  Blue: "bg-blue-900 text-blue-200 border-blue-700",
  Green: "bg-emerald-900 text-emerald-200 border-emerald-700",
  Yellow: "bg-amber-900 text-amber-200 border-amber-700",
};

export function TrumpArea({ trump, effectiveTrumpColor }: TrumpAreaProps) {
  return (
    <UiCard className="bg-zinc-900/60 border-zinc-800">
      <CardHeader className="pb-2">
        <CardTitle className="text-sm font-semibold uppercase tracking-wider text-zinc-400">
          Trump Card
        </CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col items-center justify-center gap-3">
        {trump ? (
          <div className="flex flex-col items-center gap-2">
            {"card" in trump && trump.card && (
              <GameCardView card={trump.card} size="sm" isClickable={false} />
            )}
            <div className="text-xs text-center text-zinc-300">
              <p className="font-semibold">{trump.type}</p>
              {effectiveTrumpColor && (
                <span
                  className={`inline-block mt-1 px-2.5 py-0.5 rounded font-bold text-xs border ${colorBadgeStyles[effectiveTrumpColor]}`}
                >
                  Trump: {effectiveTrumpColor}
                </span>
              )}
            </div>
          </div>
        ) : (
          <p className="text-zinc-500 text-sm italic py-4">No trump dealt yet</p>
        )}
      </CardContent>
    </UiCard>
  );
}
