"use client";

import { useSearchParams } from "next/navigation";
import { GameBoard } from "@/features/game";

export default function GamePage() {
  const searchParams = useSearchParams();
  const queryPlayerId = searchParams.get("playerId");
  const customPlayerId = queryPlayerId ? Number(queryPlayerId) : undefined;

  return (
    <main className="app-page p-4 sm:p-6 min-h-screen">
      <GameBoard customPlayerId={customPlayerId} />
    </main>
  );
}