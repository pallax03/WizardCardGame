"use client";

import { AlertCircle, Clock, ShieldAlert, Sparkles } from "lucide-react";

interface GameTurnBannerProps {
  isMyTurn: boolean;
  turnPrompt: string;
  lastError: string | null;
  actionStatus: string | null;
  bidWarning?: string | null;
  statusWarning?: string | null;
  turnTimerSeconds?: number | null;
  strikes?: number;
}

export function GameTurnBanner({
  isMyTurn,
  turnPrompt,
  lastError,
  actionStatus,
  bidWarning,
  statusWarning,
  turnTimerSeconds,
  strikes = 0,
}: GameTurnBannerProps) {
  const isLowTime =
    turnTimerSeconds !== null &&
    turnTimerSeconds !== undefined &&
    turnTimerSeconds <= 5;

  return (
    <div
      className={`relative w-full overflow-hidden rounded-2xl border transition-all duration-300 shadow-2xl backdrop-blur-xl ${
        isMyTurn
          ? "border-amber-400/80 bg-gradient-to-r from-amber-950/80 via-amber-900/60 to-amber-950/80 ring-1 ring-amber-400/40 shadow-amber-500/10"
          : "border-zinc-800/80 bg-gradient-to-r from-zinc-950/90 via-zinc-900/80 to-zinc-950/90 text-zinc-300"
      }`}
    >
      {/* Effetto bagliore superiore quando è il proprio turno */}
      {isMyTurn && (
        <div className="absolute top-0 inset-x-0 h-[2px] bg-gradient-to-r from-transparent via-amber-400 to-transparent animate-pulse" />
      )}

      <div className="p-2.5 sm:p-3.5 space-y-2">
        {/* Barra Superiore: Status Turno + Timer + Strikes */}
        <div className="flex items-center justify-between gap-2">
          {/* Badge Indicatore Turno */}
          <div className="flex items-center gap-1.5">
            <span
              className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] sm:text-[11px] font-black uppercase tracking-wider ${
                isMyTurn
                  ? "bg-amber-400/20 text-amber-300 border border-amber-400/40 animate-pulse"
                  : "bg-zinc-800/80 text-zinc-400 border border-zinc-700/60"
              }`}
            >
              {isMyTurn ? (
                <>
                  <Sparkles className="size-3 text-amber-400" />
                  IL TUO TURNO
                </>
              ) : (
                "IN ATTESA"
              )}
            </span>
          </div>

          {/* Indicatore Timer e Strikes */}
          {turnTimerSeconds !== null && turnTimerSeconds !== undefined && (
            <div className="flex items-center gap-1.5 font-mono text-[11px]">
              <div
                className={`flex items-center gap-1 px-2.5 py-0.5 rounded-full border font-extrabold shadow-sm transition-colors ${
                  isLowTime
                    ? "bg-rose-950/90 text-rose-300 border-rose-500/80 animate-bounce"
                    : "bg-zinc-900/90 text-amber-300 border-amber-500/30"
                }`}
              >
                <Clock className={`size-3 ${isLowTime ? "text-rose-400" : "text-amber-400"}`} />
                <span>{turnTimerSeconds}s</span>
              </div>

              {strikes > 0 && (
                <div className="flex items-center gap-1 px-2 py-0.5 rounded-full border bg-rose-950/80 text-rose-300 border-rose-500/50 font-extrabold">
                  <ShieldAlert className="size-3 text-rose-400" />
                  <span>{strikes}</span>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Prompt Principale d'Azione */}
        <div className="text-center py-0.5">
          <p
            className={`text-xs sm:text-sm font-extrabold tracking-tight ${
              isMyTurn ? "text-amber-100 drop-shadow-[0_1px_2px_rgba(0,0,0,0.8)]" : "text-zinc-300"
            }`}
          >
            {turnPrompt}
          </p>
        </div>

        {/* Avvisi ed Errori */}
        {(statusWarning || bidWarning || lastError || actionStatus) && (
          <div className="flex flex-wrap items-center justify-center gap-1.5 pt-0.5">
            {statusWarning && (
              <span className="inline-flex items-center gap-1 text-[10px] sm:text-[11px] text-sky-200 font-semibold bg-sky-950/80 py-0.5 px-2.5 rounded-lg border border-sky-500/50">
                ⏸️ {statusWarning}
              </span>
            )}
            {bidWarning && (
              <span className="inline-flex items-center gap-1 text-[10px] sm:text-[11px] text-amber-200 font-semibold bg-amber-950/90 py-0.5 px-2.5 rounded-lg border border-amber-500/50">
                🚫 {bidWarning}
              </span>
            )}
            {lastError && (
              <span className="inline-flex items-center gap-1 text-[10px] sm:text-[11px] text-rose-200 font-semibold bg-rose-950/90 py-0.5 px-2.5 rounded-lg border border-rose-600/60 shadow">
                <AlertCircle className="size-3 text-rose-400 shrink-0" />
                {lastError}
              </span>
            )}
            {actionStatus && (
              <span className="text-[10px] text-amber-300/80 font-mono italic block w-full text-center">
                {actionStatus}
              </span>
            )}
          </div>
        )}
      </div>
    </div>
  );
}