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
      className={`p-4 rounded-2xl border text-center transition-all ${
        isMyTurn
          ? "bg-indigo-950/50 border-indigo-500/80 shadow-indigo-900/20 shadow-lg animate-pulse"
          : "bg-zinc-900/60 border-zinc-800 text-zinc-300"
      }`}
    >
      <p className="text-xs uppercase tracking-widest text-zinc-400 font-semibold mb-1">
        Turn Status
      </p>
      <p className="text-lg font-bold text-white">{turnPrompt}</p>
      {lastError && (
        <p className="mt-2 text-sm text-rose-400 font-medium bg-rose-950/40 py-1 px-3 rounded-lg inline-block border border-rose-800/40">
          {lastError}
        </p>
      )}
      {actionStatus && (
        <p className="mt-2 text-xs text-amber-300 font-mono">
          {actionStatus}
        </p>
      )}
    </div>
  );
}
