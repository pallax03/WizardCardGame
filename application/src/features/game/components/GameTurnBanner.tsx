"use client";

interface GameTurnBannerProps {
  isMyTurn: boolean;
  turnPrompt: string;
  lastError: string | null;
  actionStatus: string | null;
}

export function GameTurnBanner({
  isMyTurn,
  turnPrompt,
  lastError,
  actionStatus,
}: GameTurnBannerProps) {
  return (
    <div
      className={`p-3 sm:p-4 rounded-2xl border backdrop-blur-md text-center transition-all duration-300 shadow-2xl ${
        isMyTurn
          ? "bg-gradient-to-r from-amber-950/80 via-amber-900/90 to-amber-950/80 border-amber-400/90 shadow-amber-500/20 ring-2 ring-amber-400/40 animate-pulse"
          : "bg-zinc-950/80 border-zinc-800/80 text-zinc-300"
      }`}
    >
      <p className="text-[10px] uppercase tracking-widest text-amber-400/90 font-extrabold mb-0.5">
        ♠ Status Turno ♠
      </p>
      <p className="text-sm sm:text-base font-black text-white tracking-tight drop-shadow">
        {turnPrompt}
      </p>
      {lastError && (
        <p className="mt-2 text-xs text-rose-300 font-semibold bg-rose-950/80 py-1 px-3 rounded-lg inline-block border border-rose-700/60 shadow">
          ⚠️ {lastError}
        </p>
      )}
      {actionStatus && (
        <p className="mt-1 text-xs text-amber-300 font-mono italic">
          {actionStatus}
        </p>
      )}
    </div>
  );
}