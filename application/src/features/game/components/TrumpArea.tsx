"use client";

import { GameCardView } from "./GameCardView";
import { CARD_COLOR_BADGE_STYLES } from "./cardStyles";
import type { CardColor, Trump } from "../types";

interface TrumpAreaProps {
  trump: Trump | null;
  effectiveTrumpColor: CardColor | null;
}

export function TrumpArea({ trump, effectiveTrumpColor }: TrumpAreaProps) {
  const hasCard = trump && "card" in trump && trump.card;

  return (
    <div className="flex flex-col items-center gap-1">
      {hasCard ? (
        <GameCardView card={trump.card} size="sm" isClickable={false} />
      ) : (
        <div className="grid h-20 w-14 place-items-center rounded-lg border border-dashed border-white/20 bg-black/20">
          <span className="text-[10px] text-zinc-400 italic">In attesa…</span>
        </div>
      )}
      {effectiveTrumpColor && (
        <span
          className={`inline-block rounded-full border px-2 py-0.5 text-[10px] font-black tracking-wider shadow-md uppercase ${CARD_COLOR_BADGE_STYLES[effectiveTrumpColor]}`}
        >
          {effectiveTrumpColor}
        </span>
      )}
    </div>
  );
}