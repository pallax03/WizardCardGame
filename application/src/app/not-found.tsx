"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

export default function NotFound() {
  const router = useRouter();
  
  useEffect(() => {
    const storedLobbyId = localStorage.getItem("wizard_lobbyId");
    const storedPlayerId = localStorage.getItem("wizard_playerId");
    
    if (storedLobbyId && storedPlayerId) {
      router.push(`/lobby/${storedLobbyId}`);
    } else {
      router.push("/");
    }
  }, [router]);

  return (
    <div className="flex h-screen w-full items-center justify-center bg-zinc-950 text-indigo-400">
      <div className="flex flex-col items-center gap-4 animate-pulse">
        <div className="w-8 h-8 border-4 border-indigo-500/30 border-t-indigo-500 rounded-full animate-spin" />
        <span className="text-sm font-medium tracking-wide">Wizard Card Game</span>
      </div>
    </div>
  );
}
