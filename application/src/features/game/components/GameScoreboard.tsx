"use client";

import { Card as UiCard, CardContent, CardHeader, CardTitle } from "@/ui/components/card";
import type { Scoreboard } from "../types";

interface GameScoreboardProps {
  scoreboard: Scoreboard | null;
  playersMap: Map<number, { name: string }>;
  myPlayerId: number | null;
}

export function GameScoreboard({
  scoreboard,
  playersMap,
  myPlayerId,
}: GameScoreboardProps) {
  if (!scoreboard) return null;

  return (
    <UiCard className="bg-zinc-950/80 border-zinc-800 backdrop-blur-md shadow-2xl">
      <CardHeader className="p-3 pb-2 border-b border-zinc-800/80">
        <CardTitle className="text-xs font-black uppercase tracking-widest text-zinc-400">
          🏆 Tabellone Punteggi
        </CardTitle>
      </CardHeader>
      <CardContent className="p-3 overflow-x-auto">
        <table className="w-full text-left text-xs font-mono">
          <thead>
            <tr className="border-b border-zinc-800 text-zinc-500 uppercase">
              <th className="py-2 px-3">Giocatore</th>
              <th className="py-2 px-3">Round</th>
              <th className="py-2 px-3">Bid</th>
              <th className="py-2 px-3 text-right">Punti</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-800/50">
            {Object.entries(scoreboard).map(([pIdStr, entries]) => {
              const pId = Number(pIdStr);
              const pName = playersMap.get(pId)?.name ?? `Giocatore ${pId}`;
              const lastEntry = entries[entries.length - 1];

              return (
                <tr
                  key={pIdStr}
                  className={pId === myPlayerId ? "bg-amber-500/10 font-bold" : ""}
                >
                  <td className="py-2 px-3 text-white flex items-center gap-1.5">
                    {pName} {pId === myPlayerId && <span className="text-amber-400 text-[10px]">(Tu)</span>}
                  </td>
                  <td className="py-2 px-3 text-zinc-400">
                    {lastEntry?.round ?? "-"}
                  </td>
                  <td className="py-2 px-3 text-amber-300">
                    {lastEntry?.bid ?? "-"}
                  </td>
                  <td className="py-2 px-3 text-emerald-400 font-black text-right">
                    {lastEntry?.score ?? "-"}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </CardContent>
    </UiCard>
  );
}