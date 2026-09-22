"use client";

import { useMemo, useState } from "react";
import { Button } from "@/ui/components/button";
import { X, Trophy, Check, X as XIcon } from "lucide-react";
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

  // Elaborazione classifica e dati del tabellone
  const { leaderboard, playerEntriesMap } = useMemo(() => {
    const entriesMap = new Map<number, ScoreEntry[]>();
    const leaderboardData: { playerId: number; totalScore: number }[] = [];

    Object.entries(scoreboard).forEach(([pIdStr, entries]) => {
      const pId = Number(pIdStr);
      // Ordina le entry per round crescente
      const sortedEntries = [...entries].sort((a, b) => a.round - b.round);
      entriesMap.set(pId, sortedEntries);

      // Punteggio totale (preso dall'ultima entry disponibile)
      const lastEntry = sortedEntries[sortedEntries.length - 1];
      const totalScore = lastEntry ? lastEntry.score : 0;

      leaderboardData.push({ playerId: pId, totalScore });
    });

    // Ordina la classifica per punteggio decrescente
    leaderboardData.sort((a, b) => b.totalScore - a.totalScore);

    return {
      leaderboard: leaderboardData,
      playerEntriesMap: entriesMap,
    };
  }, [scoreboard]);

  // Giocatore attualmente selezionato (default: myPlayerId o il primo in classifica)
  const [selectedPlayerId, setSelectedPlayerId] = useState<number>(() => {
    if (myPlayerId && playerEntriesMap.has(myPlayerId)) {
      return myPlayerId;
    }
    return leaderboard[0]?.playerId ?? 0;
  });

  // Trova il piazzamento in classifica del giocatore selezionato
  const selectedRank = useMemo(() => {
    const index = leaderboard.findIndex((item) => item.playerId === selectedPlayerId);
    return index !== -1 ? index + 1 : null;
  }, [leaderboard, selectedPlayerId]);

  const selectedPlayerName = playersMap.get(selectedPlayerId)?.name ?? `Giocatore ${selectedPlayerId}`;
  const selectedPlayerEntries = playerEntriesMap.get(selectedPlayerId) ?? [];
  const selectedTotalScore = selectedPlayerEntries.length > 0
    ? selectedPlayerEntries[selectedPlayerEntries.length - 1].score
    : 0;

  return (
    <div className="relative bg-zinc-950/95 border border-amber-500/40 backdrop-blur-md shadow-2xl rounded-2xl overflow-hidden max-h-[85vh] flex flex-col w-full max-w-md mx-auto p-3 sm:p-4 space-y-4">
      {/* Header Classifica con pulsante di chiusura */}
      <div className="flex items-center justify-between border-b border-zinc-800 pb-2.5 shrink-0">
        <div className="text-xs font-black uppercase tracking-widest text-amber-400 flex items-center gap-1.5">
          <Trophy className="size-4 text-amber-400" /> Classifica
        </div>
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
      </div>

      <div className="overflow-y-auto space-y-4 pr-0.5">
        {/* Classifica Generale (Interattiva: cliccando si seleziona il giocatore) */}
        <div className="flex flex-col gap-1.5 text-xs font-mono">
          {leaderboard.map((item, idx) => {
            const pId = item.playerId;
            const pName = playersMap.get(pId)?.name ?? `P${pId}`;
            const isMe = pId === myPlayerId;
            const isSelected = pId === selectedPlayerId;
            const rank = idx + 1;

            return (
              <button
                key={pId}
                type="button"
                onClick={() => setSelectedPlayerId(pId)}
                className={`flex items-center justify-between px-3 py-2 rounded-xl border text-xs transition-all w-full text-left cursor-pointer select-none ${
                  isSelected
                    ? "bg-amber-500/20 border-amber-500 text-amber-200 shadow-md shadow-amber-500/10 font-bold"
                    : isMe
                    ? "bg-zinc-900 border-amber-500/30 text-amber-300/90 hover:bg-zinc-800/80"
                    : "bg-zinc-900/60 border-zinc-800/80 text-zinc-300 hover:bg-zinc-800/60 hover:text-white"
                }`}
              >
                <div className="flex items-center gap-2.5 truncate mr-2">
                  <span className={`text-[11px] font-bold min-w-[22px] ${isSelected ? "text-amber-400" : "text-zinc-500"}`}>
                    {rank}°
                  </span>
                  <span className="truncate font-semibold">{pName}</span>
                  {isMe && <span className="text-[10px] text-amber-400 font-normal">(Tu)</span>}
                </div>
                <span className={`font-black shrink-0 ${isSelected ? "text-amber-300" : "text-white"}`}>
                  {item.totalScore} pt
                </span>
              </button>
            );
          })}
        </div>

        {/* Tabella Personale Dettagliata */}
        <div className="space-y-2.5 pt-2 border-t border-zinc-800/80">
          {/* Header Giocatore Selezionato */}
          <div className="flex items-baseline justify-between border-b border-zinc-800 pb-1.5 px-1">
            <div className="font-black text-sm uppercase tracking-wide text-zinc-100 flex items-center gap-1.5">
              <span>{selectedPlayerName}</span>
              {selectedPlayerId === myPlayerId && (
                <span className="text-amber-400 text-xs font-normal">(Tu)</span>
              )}
            </div>
          </div>

          {/* Tabella Dettaglio Round */}
          <table className="w-full text-center text-xs font-mono border-collapse">
            <thead>
              <tr className="border-b border-zinc-800 text-zinc-400 text-[10px] font-bold uppercase">
                <th className="py-1.5 px-2 text-left">Round</th>
                <th className="py-1.5 px-2 text-right">Totale</th>
                <th className="py-1.5 px-2">Bid</th>
                <th className="py-1.5 px-2">Delta</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800/50">
              {selectedPlayerEntries.length === 0 ? (
                <tr>
                  <td colSpan={4} className="py-4 text-center text-zinc-500 italic">
                    Nessun round giocato
                  </td>
                </tr>
              ) : (
                selectedPlayerEntries.map((entry, idx) => {
                  const prevScore = idx > 0 ? selectedPlayerEntries[idx - 1].score : 0;
                  const delta = entry.score - prevScore;

                  const bid = entry.bid;
                  const isSuccess = delta > 0;

                  return (
                    <tr key={entry.round} className="hover:bg-zinc-900/40 transition-colors">
                      <td className="py-1.5 px-2 text-left font-bold text-zinc-400">
                        R{entry.round}
                      </td>
                      <td className="py-1.5 px-2 text-right font-black text-white">
                        {entry.score}
                      </td>
                      <td className="py-1.5 px-2 text-zinc-200">
                        <span className="flex items-center justify-center gap-1">
                          <span>
                            {bid}
                          </span>
                        </span>
                      </td>
                      <td
                        className={`py-1.5 px-2 font-bold ${
                          delta > 0
                            ? "text-emerald-400"
                            : delta < 0
                            ? "text-rose-400"
                            : "text-zinc-400"
                        }`}
                      >
                        {delta > 0 ? `+${delta}` : delta}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}