"use client";

import { useSearchParams } from "next/navigation";
import { GameBoard } from "@/features/game";

export default function GamePage() {
  const searchParams = useSearchParams();
  const queryPlayerId = searchParams.get("playerId");
  const customPlayerId = queryPlayerId ? Number(queryPlayerId) : undefined;

  return (
    <main className="min-h-screen bg-radial from-zinc-900 via-zinc-950 to-black text-zinc-100 p-2 sm:p-6 overflow-x-hidden">
      <GameBoard customPlayerId={customPlayerId} />
    </main>
  );
}