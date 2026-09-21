"use client";

import { Card as UiCard, CardContent, CardHeader, CardTitle } from "@/ui/components/card";
import { Button } from "@/ui/components/button";
import { X } from "lucide-react";
import type { Scoreboard } from "../types";

interface GameScoreboardProps {
  scoreboard: Scoreboard | null;
  playersMap: Map<number, { name: string }>;
  myPlayerId: number | null;
  onClose?: () => void;
}

export function GameScoreboard({
  scoreboard,
  playersMap,
  myPlayerId,
  onClose,
}: GameScoreboardProps) {
  if (!scoreboard) return null;

  return (
    <UiCard className="relative bg-zinc-950/95 border-amber-500/40 backdrop-blur-md shadow-2xl overflow-hidden max-h-[85vh] flex flex-col w-full">
      <CardHeader className="p-3 pb-2.5 border-b border-zinc-800 flex flex-row items-center justify-between shrink-0">
        <CardTitle className="text-xs font-black uppercase tracking-widest text-amber-400 flex items-center gap-1.5">
          🏆 Tabellone Punteggi
        </CardTitle>
        {onClose && (
          <Button
            type="button"
            variant="ghost"
            size="icon"
            onClick={onClose}
            className="size-7 text-zinc-400 hover:text-white hover:bg-zinc-800/80 rounded-lg transition-colors shrink-0"
            title="Chiudi tabellone"
          >
            <X className="size-4" />
          </Button>
        )}
      </CardHeader>

      <CardContent className="p-3 overflow-y-auto overflow-x-auto">
        <table className="w-full text-left text-xs font-mono">
          <thead>
            <tr className="border-b border-zinc-800 text-zinc-500 uppercase text-[10px]">
              <th className="py-2 px-2 sm:px-3">Giocatore</th>
              <th className="py-2 px-2 sm:px-3">Round</th>
              <th className="py-2 px-2 sm:px-3">Bid</th>
              <th className="py-2 px-2 sm:px-3 text-right">Punti</th>
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
                  <td className="py-2 px-2 sm:px-3 text-white flex items-center gap-1">
                    <span className="truncate max-w-[90px] sm:max-w-[130px]">{pName}</span>
                    {pId === myPlayerId && (
                      <span className="text-amber-400 text-[9px] shrink-0">(Tu)</span>
                    )}
                  </td>
                  <td className="py-2 px-2 sm:px-3 text-zinc-400">
                    {lastEntry?.round ?? "-"}
                  </td>
                  <td className="py-2 px-2 sm:px-3 text-amber-300">
                    {lastEntry?.bid ?? "-"}
                  </td>
                  <td className="py-2 px-2 sm:px-3 text-emerald-400 font-black text-right">
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