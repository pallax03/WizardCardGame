"use client";

import { useMemo, useState } from "react";
import { Button } from "@/ui/components/button";
import { X, Trophy} from "lucide-react";
import type { Scoreboard, ScoreEntry } from "../types";

interface GameScoreboardProps {
  players?: Record<string, unknown>[];
  scoreboard: Scoreboard | null;
  playersMap: Map<number, { name: string; isOnline?: boolean }>;
  myPlayerId?: number | null;
  initialSelectedPlayerId?: number;
  onClose?: () => void;
  isScrollable?: boolean;
}

export function GameScoreboard({
  scoreboard,
  playersMap,
  myPlayerId,
  initialSelectedPlayerId,
  onClose,
  players,
  isScrollable = true,
}: GameScoreboardProps) {
  // Elaborazione classifica e dati del tabellone
  const { leaderboard, playerEntriesMap } = useMemo(() => {
    const entriesMap = new Map<number, ScoreEntry[]>();
    const leaderboardData: { playerId: number; totalScore: number }[] = [];

    if (Array.isArray(players)) {
      for (const p of players) {
        if (typeof p.id === "number") {
          entriesMap.set(p.id, []);
        }
      }
    }

    if (scoreboard && typeof scoreboard === 'object') {
      Object.entries(scoreboard).forEach(([pIdStr, entries]) => {
        const pId = Number(pIdStr);
        const validEntries = Array.isArray(entries) ? entries : [];
        const sortedEntries = [...validEntries].sort((a, b) => (a.round || 0) - (b.round || 0));
        entriesMap.set(pId, sortedEntries);
      });
    }

    entriesMap.forEach((sortedEntries, pId) => {
      const lastEntry = sortedEntries.length > 0 ? sortedEntries[sortedEntries.length - 1] : null;
      const totalScore = lastEntry ? (Number(lastEntry.score) || 0) : 0;
      leaderboardData.push({ playerId: pId, totalScore });
    });

    leaderboardData.sort((a, b) => b.totalScore - a.totalScore);
    return { leaderboard: leaderboardData, playerEntriesMap: entriesMap };
  }, [scoreboard, players]);

  // Giocatore attualmente selezionato (default: myPlayerId o il primo in classifica)
  const [selectedPlayerId, setSelectedPlayerId] = useState<number>(() => {
    if (initialSelectedPlayerId !== undefined && playerEntriesMap.has(initialSelectedPlayerId)) {
      return initialSelectedPlayerId;
    }
    if (myPlayerId && playerEntriesMap.has(myPlayerId)) {
      return myPlayerId;
    }
    return leaderboard[0]?.playerId ?? 0;
  });

  if (!scoreboard) return null;

  const getBotDifficultyLabel = (p?: Record<string, unknown>) => {
    if (!p?.difficulty || typeof p.name !== "string") return null;
    const isRealBot = p.name.toLowerCase().includes("bot");
    if (!isRealBot) return "BOT";
    return p.difficulty === 'Dumb' ? 'Stupido' : p.difficulty === 'Prolog' ? 'Normale' : String(p.difficulty);
  };

  const selectedP = Array.isArray(players) ? players.find((p) => p.id === selectedPlayerId) : undefined;
  const selectedPlayerNameRaw = playersMap.get(selectedPlayerId)?.name ?? `Giocatore ${selectedPlayerId}`;
  const selectedDiff = getBotDifficultyLabel(selectedP);
  const selectedPlayerName = selectedPlayerNameRaw;
  const selectedPlayerEntries = playerEntriesMap.get(selectedPlayerId) ?? [];

  return (
    <div className={`relative bg-zinc-950/95 border border-zinc-700/60 backdrop-blur-md shadow-2xl rounded-2xl ${isScrollable ? 'max-h-[85vh] overflow-hidden' : ''} flex flex-col w-full max-w-md mx-auto p-3 sm:p-4 space-y-4`}>
      {/* Header Classifica con pulsante di chiusura */}
      <div className="flex items-center justify-between border-b border-zinc-800 pb-2.5 shrink-0">
        <div className="text-xs font-black uppercase tracking-widest text-white flex items-center gap-1.5">
          <Trophy className="size-4 text-white" /> Classifica
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

      <div className={`space-y-4 ${isScrollable ? "overflow-y-auto pr-0.5" : ""}`}>
        {/* Classifica Generale (Interattiva: cliccando si seleziona il giocatore) */}
        <div className="flex flex-col gap-1.5 text-xs font-mono">
          {leaderboard.map((item, idx) => {
            const pId = item.playerId;
            const p = Array.isArray(players) ? players.find((p) => p.id === pId) : undefined;
            const pNameRaw = playersMap.get(pId)?.name ?? `P${pId}`;
            const diffLabel = getBotDifficultyLabel(p);
            const pName = pNameRaw;
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
                    ? "bg-zinc-100 text-black border-white shadow-lg font-bold"
                    : isMe
                    ? "bg-zinc-800 border-zinc-600 text-zinc-100 hover:bg-zinc-700"
                    : "bg-zinc-900/60 border-zinc-800/80 text-zinc-300 hover:bg-zinc-800/60 hover:text-white"
                }`}
              >
                <div className="flex items-center gap-2.5 truncate mr-2">
                  <span className={`text-[11px] font-bold min-w-5.5 ${isSelected ? "text-black" : "text-zinc-500"}`}>
                    {rank}°
                  </span>
                  <span className="truncate font-semibold">{pName}</span>
                  {diffLabel && <span className={`text-[9px] uppercase font-bold px-1 rounded-sm ${isSelected ? "bg-black/10 text-black/60" : "bg-white/10 text-white/50"}`}>{diffLabel}</span>}
                  {isMe && <span className="text-[10px] text-zinc-500 font-normal">(Tu)</span>}
                </div>
                <span className={`font-black shrink-0 ${isSelected ? "text-black" : "text-white"}`}>
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
              {selectedDiff && <span className="text-[10px] uppercase font-bold bg-white/10 text-white/70 px-1.5 py-0.5 rounded-sm">{selectedDiff}</span>}
              {selectedPlayerId === myPlayerId && (
                <span className="text-black text-xs font-normal">(Tu)</span>
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