"use client";

import { Avatar, AvatarFallback } from "@/ui/components/avatar";
import { Button } from "@/ui/components/button";
import { Bot as BotIcon, X, Loader2 } from "lucide-react";
import { lobbyI18n } from "@/ui/i18n/lobby";
import { PlayerCardProps } from "../types";

export function PlayerCard({ player, isMe, isBot, isOnline, isRemoving, onRemoveBot }: PlayerCardProps) {
  const initials = player.name ? player.name.slice(0, 2).toUpperCase() : "P";

  return (
    <div
      className={`flex items-center justify-between p-3.5 rounded-xl border ${
        isMe
          ? "bg-slate-800/90 border-indigo-500/50 ring-1 ring-indigo-500/30"
          : "bg-slate-800/60 border-slate-700/60"
      }`}
    >
      <div className="flex items-center gap-3 min-w-0">
        <div className="relative inline-block shrink-0">
          <Avatar className={`h-10 w-10 ${isBot ? "border border-cyan-500/50" : ""}`}>
            <AvatarFallback className={isBot ? "bg-cyan-950 text-cyan-400" : "bg-slate-700 text-slate-200"}>
              {isBot ? <BotIcon className="w-5 h-5" /> : initials}
            </AvatarFallback>
          </Avatar>
          <span
            className={`absolute -bottom-0.5 -right-0.5 z-10 block h-3 w-3 rounded-full ring-2 ring-slate-900 ${
              isOnline ? "bg-emerald-500" : "bg-slate-500"
            }`}
          />
        </div>

        <div className="min-w-0">
          <p className="font-semibold text-sm text-slate-100 flex items-center gap-1.5 truncate">
            <span className="truncate">{player.name}</span>
            {isMe && (
              <span className="text-[10px] bg-indigo-950 text-indigo-300 border border-indigo-800/60 px-1.5 py-0.2 rounded font-mono shrink-0">
                {lobbyI18n.playersCard.youBadge}
              </span>
            )}
          </p>
          <span className="text-xs text-slate-400 font-mono">
            {isOnline ? "Online" : "Offline"}
          </span>
        </div>
      </div>

      {isBot && (
        <Button
          size="icon"
          variant="ghost"
          disabled={isRemoving}
          className="h-7 w-7 text-slate-400 hover:text-red-400 hover:bg-red-950/40"
          onClick={() => onRemoveBot(player.id)}
        >
          {isRemoving ? <Loader2 className="w-3.5 h-3.5 animate-spin text-red-400" /> : <X className="w-3.5 h-3.5" />}
        </Button>
      )}
    </div>
  );
}