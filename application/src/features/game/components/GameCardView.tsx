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

const colorStyles: Record<CardColor, { bg: string; text: string; border: string; glow: string }> = {
  Red: {
    bg: "bg-gradient-to-b from-rose-950/90 to-rose-900/80",
    text: "text-rose-300",
    border: "border-rose-500/60",
    glow: "shadow-rose-900/40",
  },
  Blue: {
    bg: "bg-gradient-to-b from-blue-950/90 to-blue-900/80",
    text: "text-blue-300",
    border: "border-blue-500/60",
    glow: "shadow-blue-900/40",
  },
  Green: {
    bg: "bg-gradient-to-b from-emerald-950/90 to-emerald-900/80",
    text: "text-emerald-300",
    border: "border-emerald-500/60",
    glow: "shadow-emerald-900/40",
  },
  Yellow: {
    bg: "bg-gradient-to-b from-amber-950/90 to-amber-900/80",
    text: "text-amber-300",
    border: "border-amber-500/60",
    glow: "shadow-amber-900/40",
  },
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
    sm: "w-14 h-20 text-xs p-1.5 rounded-lg",
    md: "w-20 h-28 text-sm p-2 rounded-xl",
    lg: "w-24 h-36 text-base p-2.5 rounded-xl sm:w-28 sm:h-40",
  }[size];

  let cardStyle = "border-zinc-700 bg-zinc-900 text-zinc-300 shadow-black/60";
  let label = "";
  let subLabel = "";

  if (card.type === "Standard") {
    const style = colorStyles[card.color];
    cardStyle = `${style.border} ${style.bg} ${style.text} ${style.glow}`;
    label = String(card.rank);
    subLabel = card.color;
  } else if (card.type === "Wizard") {
    cardStyle =
      "border-amber-400/80 bg-gradient-to-b from-purple-950 via-purple-900 to-indigo-950 text-amber-300 shadow-purple-900/60 ring-1 ring-amber-400/30";
    label = "W";
    subLabel = `Wizard #${card.id}`;
  } else if (card.type === "Jester") {
    cardStyle =
      "border-cyan-400/80 bg-gradient-to-b from-slate-950 via-cyan-950 to-slate-900 text-cyan-300 shadow-cyan-900/60 ring-1 ring-cyan-400/30";
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
        "relative flex flex-col justify-between border-2 font-mono font-bold shadow-xl transition-all duration-200 select-none text-left backdrop-blur-md transform-gpu",
        sizeClasses,
        cardStyle,
        isClickable && "cursor-pointer hover:-translate-y-2 hover:shadow-2xl hover:brightness-110",
        !isClickable && "cursor-default",
        !isLegal && "opacity-35 grayscale-[60%] hover:translate-y-0",
        isSelected &&
          "ring-4 ring-amber-400 -translate-y-4 shadow-2xl shadow-amber-500/50 scale-105 z-20"
      )}
    >
      <div className="flex justify-between items-start">
        <span className="text-base sm:text-lg leading-none font-extrabold tracking-tighter">
          {label}
        </span>
      </div>

      <div className="my-auto text-center">
        <span className="text-2xl sm:text-3xl leading-none drop-shadow-md">
          {card.type === "Standard" ? "♦" : card.type === "Wizard" ? "🧙" : "🃏"}
        </span>
      </div>

      <div className="truncate text-[9px] sm:text-[10px] uppercase font-sans font-black tracking-wider opacity-90 text-center">
        {subLabel}
      </div>
    </button>
  );
}