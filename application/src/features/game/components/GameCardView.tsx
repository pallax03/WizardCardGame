"use client";

import { cn } from "@/lib/utils";
import type { Card, CardColor } from "../types";
import { cardToString } from "../state/gameReducer";

interface GameCardViewProps {
  card: Card;
  isSelected?: boolean;
  isLegal?: boolean;
  isClickable?: boolean;
  onClick?: () => void;
  size?: "sm" | "md" | "lg";
}

const colorStyles: Record<CardColor, string> = {
  Red: "border-rose-500/60 bg-rose-950/40 text-rose-300 hover:border-rose-400",
  Blue: "border-blue-500/60 bg-blue-950/40 text-blue-300 hover:border-blue-400",
  Green: "border-emerald-500/60 bg-emerald-950/40 text-emerald-300 hover:border-emerald-400",
  Yellow: "border-amber-500/60 bg-amber-950/40 text-amber-300 hover:border-amber-400",
};

export function GameCardView({
  card,
  isSelected = false,
  isLegal = true,
  isClickable = false,
  onClick,
  size = "md",
}: GameCardViewProps) {
  const sizeClasses = {
    sm: "w-16 h-22 text-xs p-1.5",
    md: "w-20 h-28 text-sm p-2",
    lg: "w-24 h-34 text-base p-2.5",
  }[size];

  let cardStyle = "border-zinc-700 bg-zinc-900 text-zinc-300";
  let label = "";
  let subLabel = "";

  if (card.type === "Standard") {
    cardStyle = colorStyles[card.color];
    label = String(card.rank);
    subLabel = card.color;
  } else if (card.type === "Wizard") {
    cardStyle =
      "border-purple-500/70 bg-purple-950/50 text-purple-300 shadow-purple-900/20";
    label = "W";
    subLabel = `Wizard #${card.id}`;
  } else if (card.type === "Jester") {
    cardStyle =
      "border-cyan-500/70 bg-cyan-950/50 text-cyan-300 shadow-cyan-900/20";
    label = "J";
    subLabel = `Jester #${card.id}`;
  }

  return (
    <button
      type="button"
      title={cardToString(card)}
      disabled={!isClickable}
      onClick={onClick}
      className={cn(
        "relative flex flex-col justify-between rounded-xl border-2 font-mono font-bold shadow-md transition-all select-none text-left",
        sizeClasses,
        cardStyle,
        isClickable && "cursor-pointer hover:-translate-y-1 hover:shadow-lg",
        !isClickable && "cursor-default",
        !isLegal && "opacity-40 grayscale-[40%]",
        isSelected && "ring-3 ring-indigo-400 -translate-y-2 shadow-indigo-500/30"
      )}
    >
      <div className="flex justify-between items-start">
        <span className="text-base sm:text-lg leading-none">{label}</span>
      </div>

      <div className="my-auto text-center">
        <span className="text-xl sm:text-2xl leading-none">
          {card.type === "Standard" ? "♦" : card.type === "Wizard" ? "🧙" : "🃏"}
        </span>
      </div>

      <div className="truncate text-[10px] sm:text-xs uppercase font-sans font-semibold tracking-wider opacity-80">
        {subLabel}
      </div>
    </button>
  );
}
