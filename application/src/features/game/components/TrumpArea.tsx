"use client";

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
          className={`inline-block rounded-full border px-3 py-0.5 text-[11px] font-black tracking-wider shadow-md uppercase ${colorBadgeStyles[effectiveTrumpColor]}`}
        >
          {effectiveTrumpColor}
        </span>
      )}
    </div>
  );
}
