"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { GameBoard } from "@/features/game";

function GamePageContent() {
  const searchParams = useSearchParams();
  const queryPlayerId = searchParams.get("playerId");
  const parsed = queryPlayerId !== null ? Number(queryPlayerId) : NaN;
  const customPlayerId =
    Number.isInteger(parsed) && parsed >= 0 ? (parsed as number) : undefined;

  return (
    <main className="h-dvh bg-radial from-zinc-900 via-zinc-950 to-black text-zinc-100 overflow-hidden p-2.5 sm:p-4">
      <GameBoard customPlayerId={customPlayerId} />
    </main>
  );
}

export default function GamePage() {
  return (
    <Suspense
      fallback={
        <main className="h-dvh bg-radial from-zinc-900 via-zinc-950 to-black text-zinc-100 flex items-center justify-center p-2.5 sm:p-4">
          <p className="text-slate-400 animate-pulse font-medium text-center">
            Caricamento partita…
          </p>
        </main>
      }
    >
      <GamePageContent />
    </Suspense>
  );
}
