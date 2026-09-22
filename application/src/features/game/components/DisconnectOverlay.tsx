"use client";

import { useEffect, useState } from "react";
import { ArrowLeft, Loader2 } from "lucide-react";
import { t } from "@/ui/i18n/core";

const lobbyI18n = t("lobby");

interface DisconnectOverlayProps {
  timerSeconds: number;
  offlineNames: string;
  isWorking: boolean;
  onBackToLobby: () => void;
}

/**
 * Overlay modale al centro della pagina quando la lobby è DISCONNECTING:
 * mostra il countdown del timer di disconnessione del backend e un solo
 * bottone per tornare alla lobby (mette in pausa subito). Se il timer scade,
 * il server sostituisce tutti gli offline con bot e la partita continua;
 * se restano zero umani in gioco la partita va in pausa (logica backend
 * invariata). Resta visibile finché lo stato non cambia.
 */
export function DisconnectOverlay({
  timerSeconds,
  offlineNames,
  isWorking,
  onBackToLobby,
}: DisconnectOverlayProps) {
  const total = Number.isFinite(timerSeconds) && timerSeconds > 0 ? timerSeconds : 30;
  const [secondsLeft, setSecondsLeft] = useState(total);

  useEffect(() => {
    const next = total;
    queueMicrotask(() => setSecondsLeft(next));
  }, [total]);

  useEffect(() => {
    if (secondsLeft <= 0) return;
    const id = setTimeout(() => setSecondsLeft((s) => Math.max(0, s - 1)), 1000);
    return () => clearTimeout(id);
  }, [secondsLeft]);

  const radius = 26;
  const circumference = 2 * Math.PI * radius;
  const progress = total > 0 ? Math.max(0, Math.min(1, secondsLeft / total)) : 0;

  return (
    <div
      role="alertdialog"
      aria-modal="true"
      aria-label={lobbyI18n.disconnectOverlay.title}
      className="fixed inset-0 z-[60] grid place-items-center overflow-y-auto bg-black/70 p-4 backdrop-blur-sm"
    >
    <div className="flex w-max max-w-full flex-col items-center gap-2.5 rounded-3xl border border-amber-400/60 bg-zinc-950/95 p-4 shadow-2xl sm:p-5">
      <span className="rounded-full border border-amber-400/50 bg-amber-500/15 px-3 py-0.5 text-[11px] font-black tracking-wider text-amber-300 uppercase">
        ⚠ {lobbyI18n.disconnectOverlay.title}
      </span>
      <p className="max-w-72 text-center text-xs text-zinc-300">
        {lobbyI18n.disconnectOverlay.description(offlineNames)}
      </p>
      <div className="relative grid size-20 place-items-center">
        <svg viewBox="0 0 64 64" className="size-20 -rotate-90">
          <circle
            cx="32"
            cy="32"
            r={radius}
            fill="none"
            strokeWidth="6"
            className="stroke-white/10"
          />
          <circle
            cx="32"
            cy="32"
            r={radius}
            fill="none"
            strokeWidth="6"
            strokeLinecap="round"
            className="stroke-amber-400 transition-[stroke-dashoffset] duration-1000 ease-linear"
            strokeDasharray={circumference}
            strokeDashoffset={circumference * (1 - progress)}
          />
        </svg>
        <span className="absolute font-mono text-lg font-black text-amber-300">
          {secondsLeft}
        </span>
      </div>
      <span className="max-w-72 animate-pulse text-center font-mono text-[11px] font-bold text-zinc-200">
        {secondsLeft > 0
          ? lobbyI18n.disconnectOverlay.timer(secondsLeft)
          : lobbyI18n.disconnectOverlay.expired}
      </span>
      <div className="flex flex-wrap items-center justify-center gap-2 pt-1">
        <button
          type="button"
          disabled={isWorking}
          onClick={onBackToLobby}
          className="flex items-center gap-1.5 rounded-xl border border-zinc-600/60 bg-zinc-800/80 px-3.5 py-2 text-xs font-bold text-zinc-200 transition-colors hover:bg-zinc-700 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {isWorking ? (
            <Loader2 className="size-3.5 animate-spin" />
          ) : (
            <ArrowLeft className="size-3.5" />
          )}
          {lobbyI18n.disconnectOverlay.backToLobby}
        </button>
      </div>
    </div>
    </div>
  );
}
