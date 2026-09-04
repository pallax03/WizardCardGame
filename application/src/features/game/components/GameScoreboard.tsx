"use client";

import { Card as UiCard, CardContent, CardHeader, CardTitle } from "@/ui/components/card";
import type { Scoreboard } from "../types";

interface GameScoreboardProps {
  scoreboard: Scoreboard | null;
  playersMap: Map<number, { name: string }>;
  myPlayerId: number;
}

export function GameScoreboard({
  scoreboard,
  playersMap,
  myPlayerId,
}: GameScoreboardProps) {
  if (!scoreboard) return null;

  return (
    <UiCard className="bg-zinc-900/60 border-zinc-800">
      <CardHeader className="pb-2">
        <CardTitle className="text-sm font-semibold uppercase tracking-wider text-zinc-400">
          Scoreboard
        </CardTitle>
      </CardHeader>
      <CardContent className="overflow-x-auto">
        <table className="w-full text-left text-xs font-mono">
          <thead>
            <tr className="border-b border-zinc-800 text-zinc-400">
              <th className="py-2 px-3">Player</th>
              <th className="py-2 px-3">Round</th>
              <th className="py-2 px-3">Bid</th>
              <th className="py-2 px-3">Score</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-800/60">
            {Object.entries(scoreboard).map(([pIdStr, entries]) => {
              const pId = Number(pIdStr);
              const pName = playersMap.get(pId)?.name ?? `Player ${pId}`;
              const lastEntry = entries[entries.length - 1];

              return (
                <tr
                  key={pIdStr}
                  className={pId === myPlayerId ? "bg-indigo-950/20 font-bold" : ""}
                >
                  <td className="py-2 px-3 text-white">
                    {pName} {pId === myPlayerId && "(You)"}
                  </td>
                  <td className="py-2 px-3 text-zinc-400">
                    {lastEntry?.round ?? "-"}
                  </td>
                  <td className="py-2 px-3 text-zinc-400">
                    {lastEntry?.bid ?? "-"}
                  </td>
                  <td className="py-2 px-3 text-emerald-400">
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
