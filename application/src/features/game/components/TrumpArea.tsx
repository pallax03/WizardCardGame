"use client";

import { GameCardView } from "./GameCardView";
import { CARD_COLOR_BADGE_STYLES } from "./cardStyles";
import type { CardColor, Trump } from "../types";

interface TrumpAreaProps {
  trump: Trump | null;
  effectiveTrumpColor: CardColor | null;
}

export function TrumpArea({ trump, effectiveTrumpColor }: TrumpAreaProps) {
  return (
    <div className="flex min-w-[128px] flex-col items-center gap-1.5 rounded-2xl border border-amber-500/40 bg-zinc-950/85 px-3 py-2.5 shadow-2xl backdrop-blur-md">
      <span className="text-[10px] font-black tracking-[0.2em] text-amber-400 uppercase">
        👑 Briscola
      </span>
      {trump && "card" in trump && trump.card ? (
        <GameCardView card={trump.card} size="sm" isClickable={false} />
      ) : (
        <span className="py-3 text-xs text-zinc-500 italic">In attesa…</span>
      )}
      {trump && (
        <span className="text-[9px] font-semibold tracking-wider text-zinc-500 uppercase">
          {trump.type}
        </span>
      )}
      {effectiveTrumpColor && (
        <span
          className={`inline-block rounded-full border px-3 py-0.5 text-[11px] font-black tracking-wider shadow-md uppercase ${CARD_COLOR_BADGE_STYLES[effectiveTrumpColor]}`}
        >
          {effectiveTrumpColor}
        </span>
      )}
    </div>
  );
}
