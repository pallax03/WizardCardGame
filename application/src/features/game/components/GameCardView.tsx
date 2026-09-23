"use client";

import { cn } from "@/lib/utils";
import type { Card, CardColor } from "../types";
import { cardToString } from "../state/gameReducer";
import { CARD_COLOR_STYLES } from "./cardStyles";

interface GameCardViewProps {
  card: Card;
  isSelected?: boolean;
  isLegal?: boolean;
  isClickable?: boolean;
  isHinted?: boolean;
  onClick?: () => void;
  size?: "sm" | "md" | "lg";
  effectiveColor?: string; // e.g. "RED"
}

export function GameCardView({
  card,
  isSelected = false,
  isLegal = true,
  isClickable = false,
  isHinted = false,
  onClick,
  size = "md",
  effectiveColor,
}: GameCardViewProps) {
  const sizeClasses = {
    sm: "w-14 h-20 text-xs p-1.5 rounded-lg",
    md: "w-20 h-28 text-sm p-2 rounded-xl",
    lg: "w-24 h-36 text-base p-2.5 rounded-xl sm:w-28 sm:h-40",
  }[size];

  let cardStyle = "border-zinc-700 bg-zinc-900 text-zinc-300 shadow-black/60";
  let label = "";

  const mapColor = (c: string): CardColor => {
    if (c === "RED") return "Red";
    if (c === "BLUE") return "Blue";
    if (c === "GREEN") return "Green";
    if (c === "YELLOW") return "Yellow";
    return c as CardColor;
  };

  if (card.type === "Standard") {
    const style = CARD_COLOR_STYLES[card.color];
    cardStyle = `${style.border} ${style.bg} ${style.text} ${style.glow}`;
    label = String(card.rank);
  } else if (card.type === "Wizard") {
    label = "W";
    if (effectiveColor) {
      const style = CARD_COLOR_STYLES[mapColor(effectiveColor)];
      if (style) {
        cardStyle = `${style.border} ${style.bg} ${style.text} ${style.glow}`;
      } else {
        cardStyle = "border-zinc-300 bg-gradient-to-b from-zinc-100 via-zinc-200 to-zinc-300 text-zinc-900 shadow-zinc-400/60";
      }
    } else {
      cardStyle = "border-zinc-300 bg-gradient-to-b from-zinc-100 via-zinc-200 to-zinc-300 text-zinc-900 shadow-zinc-400/60";
    }
  } else if (card.type === "Jester") {
    label = "J";
    cardStyle = "border-zinc-600/80 bg-gradient-to-b from-black via-zinc-900 to-black text-white shadow-zinc-900/60 ring-1 ring-zinc-600/30";
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
          "ring-4 ring-sky-400 -translate-y-4 shadow-2xl shadow-sky-500/50 scale-105 z-20",
        isHinted &&
          !isSelected &&
          "ring-4 ring-purple-400 -translate-y-2 shadow-2xl shadow-purple-500/50 scale-105 animate-pulse"
      )}
    >
      <div className="flex justify-between items-start">
        <span className="text-base sm:text-lg leading-none font-extrabold tracking-tighter">
          {label}
        </span>
      </div>

      <div className="flex justify-end items-end h-full">
        <span className="text-base sm:text-lg leading-none font-extrabold tracking-tighter rotate-180">
          {label}
        </span>
      </div>

      {isHinted && (
        <span
          aria-label="Carta suggerita"
          title="Carta suggerita dall'AI"
          className="absolute -top-2.5 -right-2.5 grid size-7 place-items-center rounded-full border-2 border-purple-300 bg-purple-500 text-sm shadow-lg shadow-purple-500/50"
        >
          💡
        </span>
      )}
    </button>
  );
}
