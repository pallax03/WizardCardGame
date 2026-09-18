"use client";

import { useMemo } from "react";
import { PauseCircle, Spade, Trophy } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/ui/components/card";
import { Badge } from "@/ui/components/badge";
import { GameCardView } from "@/features/game/components/GameCardView";
import { CARD_COLOR_BADGE_STYLES } from "@/features/game/components/cardStyles";
import type { GameBoardState } from "@/features/game/types";
import type { LobbyPlayer } from "@/features/lobby-session/types";
import { t } from "@/ui/i18n/core";

const lobbyI18n = t("lobby");

interface PausedGameSummaryProps {
  board: GameBoardState | null;
  isLoading: boolean;
  loadFailed: boolean;
  playerId: number | null;
  players: LobbyPlayer[];
}

function phaseLabel(status: GameBoardState["status"]): string {
  const s = lobbyI18n.pausedSummary;
  switch (status) {
    case "CHOOSING_TRUMP":
      return s.phaseChoosingTrump;
    case "BIDDING":
      return s.phaseBidding;
    case "PLAYING":
      return s.phasePlaying;
    case "ROUND_SCORED":
      return s.phaseRoundScored;
    case "GAME_ENDED":
      return s.phaseEnded;
    default:
      return s.phaseWaiting;
  }
}

export function PausedGameSummary({ board, isLoading, loadFailed, playerId, players }: PausedGameSummaryProps) {
  const names = useMemo(() => {
    const map = new Map<number, string>();
    for (const p of players) map.set(p.id, p.name);
    return map;
  }, [players]);

  if (isLoading) {
    return (
      <Card className="surface-card text-zinc-100">
        <CardContent className="py-5">
          <p className="text-center text-sm text-slate-400 animate-pulse">
            {lobbyI18n.pausedSummary.loading}
          </p>
        </CardContent>
      </Card>
    );
  }

  if (!board || loadFailed) {
    if (loadFailed) {
      return (
        <Card className="surface-card text-zinc-100">
          <CardContent className="py-4">
            <p className="text-center text-xs text-zinc-500">
              {lobbyI18n.pausedSummary.unavailable}
            </p>
          </CardContent>
        </Card>
      );
    }
    return null;
  }

  const isEnded = board.status === "GAME_ENDED";

  const turnId = board.currentTurn.playerId;
  const isMyTurn = playerId !== null && turnId === playerId;
  const turnName = turnId !== null ? names.get(turnId) ?? `Player ${turnId}` : null;
  const turnText = isMyTurn
    ? lobbyI18n.pausedSummary.turnYou
    : turnName
      ? lobbyI18n.pausedSummary.turnOf(turnName)
      : lobbyI18n.pausedSummary.turnUnknown;

  const rows = players.map((p) => {
    const lastScore = board.scoreboard?.[String(p.id)]?.at(-1);
    return {
      id: p.id,
      name: p.name,
      isMe: playerId !== null && p.id === playerId,
      isTurn: turnId === p.id,
      bid: board.bids[p.id],
      tricks: board.tricksWon[p.id] ?? 0,
      points: lastScore?.score ?? 0,
    };
  });

  return (
    <Card className="surface-card text-zinc-100 border-sky-500/20">
      <CardHeader className="pb-3">
        <CardTitle className="flex flex-wrap items-center justify-between gap-2 text-base font-semibold text-white">
          <span className="flex items-center gap-2">
            {isEnded ? (
              <Trophy className="w-5 h-5 text-amber-400" />
            ) : (
              <PauseCircle className="w-5 h-5 text-sky-400" />
            )}
            {isEnded ? lobbyI18n.pausedSummary.titleEnded : lobbyI18n.pausedSummary.title}
          </span>
          <span className="flex items-center gap-2">
            <Badge variant="secondary" className="bg-zinc-800 text-zinc-200 border border-zinc-700">
              {lobbyI18n.pausedSummary.round(board.round)}
            </Badge>
            <Badge variant="outline" className="border-sky-500/40 text-sky-300">
              {phaseLabel(board.status)}
            </Badge>
          </span>
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {!isEnded && (
        <div
          className={`flex items-center gap-2 rounded-xl border px-3 py-2 text-sm font-semibold ${
            isMyTurn
              ? "border-amber-400/50 bg-amber-500/10 text-amber-300 animate-pulse"
              : "border-zinc-700/60 bg-zinc-900/60 text-zinc-200"
          }`}
        >
          <span
            className={`h-2 w-2 shrink-0 rounded-full ${isMyTurn ? "bg-amber-400" : "bg-sky-400"}`}
          />
          <span className="truncate">{turnText}</span>
          {board.trump && "card" in board.trump && board.trump.card ? (
            <span className="ml-auto flex shrink-0 items-center gap-1.5 text-xs font-normal text-zinc-400">
              <Spade className="w-3.5 h-3.5 text-zinc-500" />
              {lobbyI18n.pausedSummary.trump}:{" "}
              {board.effectiveTrumpColor ? (
                <span
                  className={`rounded-full border px-2 py-0.5 text-[10px] font-bold uppercase ${CARD_COLOR_BADGE_STYLES[board.effectiveTrumpColor]}`}
                >
                  {board.effectiveTrumpColor}
                </span>
              ) : (
                <span className="italic">{lobbyI18n.pausedSummary.noTrump}</span>
              )}
            </span>
          ) : (
            <span className="ml-auto shrink-0 text-xs font-normal text-zinc-500 italic">
              {lobbyI18n.pausedSummary.trump}: {lobbyI18n.pausedSummary.noTrump}
            </span>
          )}
        </div>
        )}

        {board.hand.length > 0 && (
          <div className="space-y-2">
            <p className="text-xs font-bold uppercase tracking-widest text-zinc-400">
              {lobbyI18n.pausedSummary.yourHand(board.hand.length)}
            </p>
            <div className="flex gap-2 overflow-x-auto pb-1">
              {board.hand.map((card, index) => (
                <div key={index} className="shrink-0 origin-top scale-90">
                  <GameCardView card={card} size="sm" isClickable={false} />
                </div>
              ))}
            </div>
          </div>
        )}

        <div className="space-y-1.5">
          <p className="text-xs font-bold uppercase tracking-widest text-zinc-400">
            {lobbyI18n.pausedSummary.standings}
          </p>
          <ul className="divide-y divide-zinc-800/60 rounded-xl border border-zinc-800/60 bg-zinc-950/40">
            {rows.map((row) => (
              <li
                key={row.id}
                className={`flex items-center gap-2 px-3 py-1.5 text-xs ${
                  row.isMe ? "bg-indigo-500/10" : ""
                }`}
              >
                <span
                  className={`grid size-6 shrink-0 place-items-center rounded-full text-[10px] font-black ${
                    row.isMe ? "bg-indigo-500/30 text-indigo-200" : "bg-zinc-800 text-zinc-300"
                  }`}
                >
                  {row.name.charAt(0).toUpperCase()}
                </span>
                <span className="min-w-0 flex-1 truncate font-semibold text-zinc-100">
                  {row.name}
                  {row.isMe && <span className="ml-1 text-[10px] text-indigo-300">(Tu)</span>}
                </span>
                {row.isTurn && (
                  <span className="shrink-0 rounded-full bg-amber-500/15 border border-amber-400/40 px-1.5 py-0.5 text-[9px] font-black uppercase text-amber-300">
                    ▶
                  </span>
                )}
                <span className="shrink-0 font-mono text-zinc-400">
                  {lobbyI18n.pausedSummary.bidShort}{" "}
                  <strong className="text-amber-300">{row.bid ?? "-"}</strong>
                </span>
                <span className="shrink-0 font-mono text-zinc-400">
                  {lobbyI18n.pausedSummary.tricksShort}{" "}
                  <strong className="text-emerald-300">{row.tricks}</strong>
                </span>
                <span className="w-12 shrink-0 text-right font-mono font-black text-zinc-100">
                  {row.points}
                  <span className="ml-0.5 text-[9px] font-normal text-zinc-500">
                    {lobbyI18n.pausedSummary.pointsShort}
                  </span>
                </span>
              </li>
            ))}
          </ul>
        </div>
      </CardContent>
    </Card>
  );
}
