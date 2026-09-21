"use client";

import { useMemo, Fragment } from "react";
import { Card as UiCard, CardContent, CardHeader, CardTitle } from "@/ui/components/card";
import { Button } from "@/ui/components/button";
import { X } from "lucide-react";
import type { Scoreboard, ScoreEntry } from "../types";

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

  const { playerIds, maxRound, entriesByRoundAndPlayer } = useMemo(() => {
    const pIds = Object.keys(scoreboard).map(Number);
    let maxR = 0;

    const map = new Map<number, Map<number, ScoreEntry>>();

    Object.entries(scoreboard).forEach(([pIdStr, entries]) => {
      const pId = Number(pIdStr);
      entries.forEach((entry) => {
        if (entry.round > maxR) maxR = entry.round;
        if (!map.has(entry.round)) {
          map.set(entry.round, new Map());
        }
        map.get(entry.round)!.set(pId, entry);
      });
    });

    return {
      playerIds: pIds,
      maxRound: maxR,
      entriesByRoundAndPlayer: map,
    };
  }, [scoreboard]);

  const roundsArray = Array.from({ length: maxRound }, (_, i) => i + 1);

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

      <CardContent className="p-2 sm:p-3 overflow-y-auto overflow-x-auto">
        <table className="w-full text-center text-xs font-mono border-collapse">
          <thead>
            {/* Prima riga intestazione: Nomi Giocatori */}
            <tr className="border-b border-zinc-800 text-zinc-400 text-[10px] font-bold">
              <th className="py-2 px-2 text-left sticky left-0 bg-zinc-950/95 z-10 border-r border-zinc-800">
                R
              </th>
              {playerIds.map((pId) => {
                const pName = playersMap.get(pId)?.name ?? `P${pId}`;
                const isMe = pId === myPlayerId;
                return (
                  <th
                    key={pId}
                    colSpan={3}
                    className={`py-2 px-2 border-r border-zinc-800/60 min-w-[120px] ${
                      isMe ? "bg-amber-500/10 text-amber-300" : "text-zinc-200"
                    }`}
                  >
                    <div className="flex items-center justify-center gap-1">
                      <span className="truncate max-w-[80px] sm:max-w-[100px]">{pName}</span>
                      {isMe && <span className="text-amber-400 text-[9px]">(Tu)</span>}
                    </div>
                  </th>
                );
              })}
            </tr>

            {/* Seconda riga intestazione: Dettagli per giocatore (Bid, Won, Totale) */}
            <tr className="border-b border-zinc-800 text-[9px] text-zinc-500 font-semibold uppercase">
              <th className="py-1 px-2 text-left sticky left-0 bg-zinc-950/95 z-10 border-r border-zinc-800">
                #
              </th>
              {playerIds.map((pId) => (
                <Fragment key={pId}>
                  <th className="py-1 px-1 text-amber-400/80">Bid</th>
                  <th className="py-1 px-1 text-emerald-400/80">Won</th>
                  <th className="py-1 px-1 text-white border-r border-zinc-800/60">Tot</th>
                </Fragment>
              ))}
            </tr>
          </thead>

          <tbody className="divide-y divide-zinc-800/50 text-[11px]">
            {roundsArray.map((r) => {
              const roundMap = entriesByRoundAndPlayer.get(r);

              return (
                <tr key={r} className="hover:bg-zinc-900/40 transition-colors">
                  {/* Colonna numero Round */}
                  <td className="py-2 px-2 text-left font-bold text-zinc-400 sticky left-0 bg-zinc-950/95 z-10 border-r border-zinc-800">
                    {r}
                  </td>

                  {/* Dati per ogni giocatore nel Round r */}
                  {playerIds.map((pId) => {
                    const entry = roundMap?.get(pId);
                    const isMe = pId === myPlayerId;

                    return (
                      <Fragment key={pId}>
                        {/* Bid */}
                        <td className={`py-2 px-1 text-amber-300 ${isMe ? "bg-amber-500/5" : ""}`}>
                          {entry?.bid ?? "-"}
                        </td>
                        {/* Trick vinti */}
                        <td className={`py-2 px-1 text-emerald-400 ${isMe ? "bg-amber-500/5" : ""}`}>
                          {entry?.tricksWon ?? "-"}
                        </td>
                        {/* Punteggio Totale Personale accumulato */}
                        <td
                          className={`py-2 px-1 font-black text-white border-r border-zinc-800/60 ${
                            isMe ? "bg-amber-500/10 text-amber-200" : ""
                          }`}
                        >
                          {entry?.score ?? "-"}
                        </td>
                      </Fragment>
                    );
                  })}
                </tr>
              );
            })}
          </tbody>
        </table>
      </CardContent>
    </UiCard>
  );
}