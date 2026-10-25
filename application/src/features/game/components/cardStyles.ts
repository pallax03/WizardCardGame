import type { CardColor } from "../types";

export interface CardColorStyle {
  bg: string;
  text: string;
  border: string;
  glow: string;
}

export const CARD_COLOR_STYLES: Record<CardColor, CardColorStyle> = {
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

export const CARD_COLOR_BADGE_STYLES: Record<CardColor, string> = {
  Red: "bg-rose-950/90 text-rose-200 border-rose-600 shadow-rose-950/50",
  Blue: "bg-blue-950/90 text-blue-200 border-blue-600 shadow-blue-950/50",
  Green: "bg-emerald-950/90 text-emerald-200 border-emerald-600 shadow-emerald-950/50",
  Yellow: "bg-amber-950/90 text-amber-200 border-amber-500 shadow-amber-950/50",
};
