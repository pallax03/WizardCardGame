import { useState } from "react";
import { Button } from "@/ui/components/button";
import { X, Loader2, Target, CheckSquare, ChevronDown, ChevronUp } from "lucide-react";
import { t } from "@/ui/i18n/core";
const lobbyI18n = t("lobby");
import { PlayerCardProps } from "../types";
import { GameCardView } from "@/features/game/components/GameCardView";

import { PlayerAvatar } from "@/ui/components/player-avatar";

export function PlayerCard({ player, isMe, isBot, isOnline, isRemoving, canRemove, onRemoveBot, gameData }: PlayerCardProps) {
  const [showHand, setShowHand] = useState(false);

  // Styling logic for bid and tricks
  const hasBid = gameData?.bid !== undefined && gameData.bid !== null;
  const bidDisplay = hasBid ? gameData.bid : "-";
  const tricksDisplay = hasBid ? (gameData?.tricks ?? 0) : "-";
  
  const bidColor = hasBid ? "text-amber-400 font-bold" : "text-zinc-500";
  const tricksColor = !hasBid 
    ? "text-zinc-500" 
    : (gameData.tricks === gameData.bid ? "text-emerald-400 font-bold" : "text-red-500 font-bold");

  return (
    <div
      className={`flex flex-col rounded-xl border transition-colors ${
        gameData?.isTurn 
          ? "bg-sky-500/10 border-sky-500/40 ring-1 ring-sky-500/30"
          : isMe
            ? "bg-zinc-800/90 border-zinc-500/50 ring-1 ring-zinc-500/30"
            : "bg-zinc-800/60 border-zinc-700/60"
      }`}
    >
      <div className="flex items-center justify-between p-3.5">
        <div className="flex items-center gap-3 min-w-0">
          <PlayerAvatar
            playerId={player.id}
            name={player.name}
            isBot={isBot}
            isMe={isMe}
            isOnline={isOnline}
            showDot={true}
          />

          <div className="min-w-0 flex flex-col">
            <p className="font-semibold text-sm flex items-center gap-1.5 truncate">
              <span className={`truncate ${gameData?.isTurn ? "text-sky-400" : "text-zinc-100"}`}>
                {isBot 
                  ? `${player.name} (${player.difficulty === "Dumb" ? lobbyI18n.botSelection.dumb : player.difficulty === "Prolog" ? lobbyI18n.botSelection.prolog : player.difficulty})` 
                  : player.name}
              </span>
              {isMe && (
                <span className="text-[10px] bg-zinc-900 text-zinc-300 border border-zinc-700/60 px-1.5 py-0.2 rounded font-mono shrink-0">
                  {lobbyI18n.playersCard.youBadge}
                </span>
              )}
            </p>
            <span className="text-xs text-zinc-400 font-mono">
              {isOnline ? "Online" : "Offline"}
            </span>
          </div>
        </div>

        {gameData ? (
          <div className="flex items-center gap-3 sm:gap-4 shrink-0 text-xs ml-2">
            <div className="flex items-center gap-1 text-zinc-400" title="Bid">
              <Target className="w-3.5 h-3.5" />
              <span className={bidColor}>{bidDisplay}</span>
            </div>
            <div className="flex items-center gap-1 text-zinc-400" title="Tricks">
              <CheckSquare className="w-3.5 h-3.5" />
              <span className={tricksColor}>{tricksDisplay}</span>
            </div>
            <div className="text-right font-black text-white whitespace-nowrap" title="Points">
              {gameData.points ?? 0} <span className="text-[9px] text-zinc-500 font-normal">Pts</span>
            </div>
          </div>
        ) : (
          !isMe && canRemove && (
            <Button
              size="icon"
              variant="ghost"
              disabled={isRemoving}
              title={isBot ? lobbyI18n.playersCard.removeBotTooltip : lobbyI18n.playersCard.removePlayerTooltip}
              className="h-7 w-7 text-zinc-400 hover:text-red-400 hover:bg-red-950/40"
              onClick={() => onRemoveBot(player.id)}
            >
              {isRemoving ? <Loader2 className="w-3.5 h-3.5 animate-spin text-red-400" /> : <X className="w-3.5 h-3.5" />}
            </Button>
          )
        )}
      </div>
      
      {/* Hand section if available */}
      {gameData?.hand && gameData.hand.length > 0 && (
        <div className="border-t border-zinc-700/50 bg-zinc-900/30 rounded-b-xl overflow-hidden">
          <button
            type="button"
            onClick={() => setShowHand(!showHand)}
            className="w-full flex items-center justify-between px-3 py-2 text-[10px] font-bold uppercase tracking-widest text-zinc-500 hover:text-zinc-300 transition-colors"
          >
            <span>{lobbyI18n.pausedSummary?.yourHand ? lobbyI18n.pausedSummary.yourHand(gameData.hand.length) : `La tua mano (${gameData.hand.length})`}</span>
            {showHand ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
          </button>
          
          {showHand && (
            <div className="p-3 pt-0 flex flex-wrap gap-2 justify-center">
              {gameData.hand.map((card, index) => (
                <div key={index} className="shrink-0">
                  <GameCardView card={card} size="sm" isClickable={false} />
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}