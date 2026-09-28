import { Card, CardContent } from "@/ui/components/card";
import { GameCardView } from "@/features/game/components/GameCardView";
import type { GameBoardState } from "@/features/game/types";
import type { LobbyPlayer } from "@/features/lobby-session/types";
import { isBotPlayer, isPlayerOnline } from "@/features/lobby-session/presence";
import { PlayerCard } from "@/features/lobby/components/PlayerCard";
import { t } from "@/ui/i18n/core";

const lobbyI18n = t("lobby");

interface PausedGameSummaryProps {
  board: GameBoardState | null;
  isLoading: boolean;
  loadFailed: boolean;
  playerId: number | null;
  players: LobbyPlayer[];
  connectedPlayerIds?: number[];
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

export function PausedGameSummary({ board, isLoading, loadFailed, playerId, players, connectedPlayerIds = [] }: PausedGameSummaryProps) {
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

  const getEffectiveColorCircle = (color: string) => {
    switch (color) {
      case "RED": return "bg-red-500";
      case "BLUE": return "bg-blue-500";
      case "GREEN": return "bg-emerald-500";
      case "YELLOW": return "bg-amber-500";
      default: return "";
    }
  };
  
  const effectiveTrumpColorClass = board.effectiveTrumpColor ? getEffectiveColorCircle(board.effectiveTrumpColor) : "";

  return (
    <div className="flex flex-col gap-4">
      
      {/* Game State Header */}
      <div className="flex items-center justify-between px-1">
        <span className="text-lg font-semibold text-white">
          {lobbyI18n.pausedSummary.round(board.round)} - {phaseLabel(board.status)}
        </span>
        
        {/* Trump */}
        {!isEnded && (
          <div className="flex items-center gap-2 text-xs font-medium text-zinc-400">
            {board.trump && "card" in board.trump && board.trump.card ? (
              <div className="flex items-center gap-1.5">
                <div className="scale-75 origin-right">
                  <GameCardView card={board.trump.card} size="sm" isClickable={false} />
                </div>
                {effectiveTrumpColorClass && (
                  <div className={`w-3 h-3 rounded-full ${effectiveTrumpColorClass} shadow-sm`} title={board.effectiveTrumpColor || undefined} />
                )}
              </div>
            ) : (
              <span className="italic">{lobbyI18n.pausedSummary.noTrump}</span>
            )}
          </div>
        )}
      </div>

      {/* Standings via PlayerCards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4">
        {players.map((player) => {
          const isMe = playerId !== null && player.id === playerId;
          const isBot = isBotPlayer(player);
          const isOnline = isPlayerOnline(player, connectedPlayerIds);
          const lastScore = board.scoreboard?.[String(player.id)]?.at(-1);

          return (
            <PlayerCard
              key={player.id}
              player={player}
              isMe={isMe}
              isBot={isBot}
              isOnline={isOnline}
              isRemoving={false}
              canRemove={false}
              onRemoveBot={async () => {}}
              gameData={{
                bid: board.bids[player.id],
                tricks: board.tricksWon[player.id],
                points: lastScore?.score ?? 0,
                isTurn: turnId === player.id,
                hand: isMe ? board.hand : undefined,
              }}
            />
          );
        })}
      </div>
      
    </div>
  );
}
