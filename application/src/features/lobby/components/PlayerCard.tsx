"use client";

import { Button } from "@/ui/components/button";
import { X, Loader2 } from "lucide-react";
import { t } from "@/ui/i18n/core";
const lobbyI18n = t("lobby");
import { PlayerCardProps } from "../types";

import { PlayerAvatar } from "@/ui/components/player-avatar";

export function PlayerCard({ player, isMe, isBot, isOnline, isRemoving, onRemoveBot }: PlayerCardProps) {
  return (
    <div
      className={`flex items-center justify-between p-3.5 rounded-xl border ${
        isMe
          ? "bg-zinc-800/90 border-indigo-500/50 ring-1 ring-indigo-500/30"
          : "bg-zinc-800/60 border-zinc-700/60"
      }`}
    >
      <div className="flex items-center gap-3 min-w-0">
        <PlayerAvatar
          playerId={player.id}
          name={player.name}
          isBot={isBot}
          isMe={isMe}
          isOnline={isOnline}
          showDot={true}
        />

        <div className="min-w-0">
          <p className="font-semibold text-sm text-zinc-100 flex items-center gap-1.5 truncate">
            <span className="truncate">
              {isBot 
                ? `${player.name} (${player.difficulty === "Dumb" ? lobbyI18n.botSelection.dumb : player.difficulty === "Prolog" ? lobbyI18n.botSelection.prolog : player.difficulty})` 
                : player.name}
            </span>
            {isMe && (
              <span className="text-[10px] bg-indigo-950 text-indigo-300 border border-indigo-800/60 px-1.5 py-0.2 rounded font-mono shrink-0">
                {lobbyI18n.playersCard.youBadge}
              </span>
            )}
          </p>
          <span className="text-xs text-zinc-400 font-mono">
            {isOnline ? "Online" : "Offline"}
          </span>
        </div>
      </div>

      {!isMe && (
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
      )}
    </div>
  );
}