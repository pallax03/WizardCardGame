"use client";

import { Card, CardHeader, CardTitle, CardContent } from "@/ui/components/card";
import { Badge } from "@/ui/components/badge";
import { Button } from "@/ui/components/button";
import { Users, UserPlus, Plus, X } from "lucide-react";
import { PlayerCard } from "@/features/lobby/components/PlayerCard";
import { EmptySlotProps, PlayerListProps } from "../types";
import { lobbyI18n } from "@/ui/i18n/lobby";

export function PlayerList({
  players,
  maxPlayers,
  currentUserId,
  connectedPlayerIds,
  activeBotSlot,
  isAddingBot,
  removingBotId,
  onSelectBotSlot,
  onAddBot,
  onRemoveBot,
}: PlayerListProps) {
  const emptySlotsCount = Math.max(0, maxPlayers - players.length);

  return (
    <Card className="surface-card text-zinc-100">
      <CardHeader className="pb-3">
        <CardTitle className="text-lg font-semibold text-white flex items-center justify-between">
          <span className="flex items-center gap-2">
            <Users className="w-5 h-5 text-zinc-400" />
            {lobbyI18n.playersCard.title}
          </span>
          <Badge variant="secondary" className="bg-zinc-800 text-zinc-300 border border-zinc-700">
            {players.length} / {maxPlayers}
          </Badge>
        </CardTitle>
      </CardHeader>
      <CardContent className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {players.map((player) => {
          const isMe = currentUserId !== null && Number(player.id) === Number(currentUserId);
          const isBot = player.difficulty !== undefined && player.difficulty !== null;
          const isOnline = isBot || connectedPlayerIds.some((id) => Number(id) === Number(player.id));

          return (
            <PlayerCard
              key={player.id}
              player={player}
              isMe={isMe}
              isBot={isBot}
              isOnline={isOnline}
              isRemoving={removingBotId === player.id}
              onRemoveBot={onRemoveBot}
            />
          );
        })}

        {Array.from({ length: emptySlotsCount }).map((_, index) => (
          <EmptySlot
            key={`empty-${index}`}
            isSelecting={activeBotSlot === index}
            isAddingBot={isAddingBot}
            onOpenSelect={() => onSelectBotSlot(index)}
            onCloseSelect={() => onSelectBotSlot(null)}
            onAddBot={async (difficulty) => {
              await onAddBot(difficulty);
              onSelectBotSlot(null);
            }}
          />
        ))}
      </CardContent>
    </Card>
  );
}

function EmptySlot({
  isSelecting,
  isAddingBot,
  onOpenSelect,
  onCloseSelect,
  onAddBot,
}: EmptySlotProps) {
  if (isSelecting) {
    return (
      <div className="flex items-center justify-between p-2.5 gap-2 rounded-xl border border-dashed border-cyan-800/80 bg-zinc-950/50 text-zinc-300 text-xs font-medium">
        <span className="text-cyan-400 pl-1">{lobbyI18n.botSelection.label}</span>
        <div className="flex items-center gap-1.5">
          <Button
            size="sm"
            variant="outline"
            disabled={isAddingBot}
            className="h-7 px-2 border-zinc-700 bg-zinc-800 text-xs"
            onClick={() => onAddBot("Dumb")}
          >
            {lobbyI18n.botSelection.dumb}
          </Button>
          <Button
            size="sm"
            variant="outline"
            disabled={isAddingBot}
            className="h-7 px-2 border-zinc-700 bg-zinc-800 text-xs"
            onClick={() => onAddBot("Prolog")}
          >
            {lobbyI18n.botSelection.prolog}
          </Button>
          <Button size="icon" variant="ghost" disabled={isAddingBot} className="h-7 w-7" onClick={onCloseSelect}>
            <X className="w-3.5 h-3.5" />
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex items-center justify-between p-3.5 rounded-xl border border-dashed border-zinc-800 bg-zinc-950/30 text-zinc-500 text-sm font-medium">
      <div className="flex items-center gap-2">
        <UserPlus className="w-4 h-4 opacity-50" />
        <span>{lobbyI18n.waitingSlot.waiting}</span>
      </div>
      <Button
        size="sm"
        variant="ghost"
        disabled={isAddingBot}
        className="h-7 gap-1 px-2 border border-zinc-800 hover:border-cyan-500/50 bg-zinc-900 hover:bg-cyan-950/40 text-zinc-400 hover:text-cyan-300 text-xs transition-colors"
        onClick={onOpenSelect}
      >
        <Plus className="w-3.5 h-3.5" /> {lobbyI18n.waitingSlot.addBotButton}
      </Button>
    </div>
  );
}