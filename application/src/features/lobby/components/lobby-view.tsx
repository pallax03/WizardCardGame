"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Card, CardHeader, CardTitle, CardContent } from "@/ui/components/card";
import { Avatar, AvatarFallback } from "@/ui/components/avatar";
import { Badge } from "@/ui/components/badge";
import { Button } from "@/ui/components/button";
import { Copy, Check, LogOut, Settings, Users, UserPlus, Bot as BotIcon, Plus, X, Loader2 } from "lucide-react";
import { addBotAction, leaveLobbyAction } from "@/features/lobby/actions/lobby-actions";
import { useLobbySession } from "@/features/lobby-session";
import { lobbyI18n } from "@/i18n/lobby";

interface LobbyViewProps {
  maxPlayers?: number;
}

export function LobbyView({ maxPlayers = 6 }: LobbyViewProps) {
  const router = useRouter();
  
  const { lobby, playerId, connectionState, refreshLobby, error: sessionError } = useLobbySession();
  
  const [copied, setCopied] = useState<boolean>(false);
  const [isAddingBot, setIsAddingBot] = useState<boolean>(false);
  const [removingBotId, setRemovingBotId] = useState<string | number | null>(null);
  const [activeBotSlot, setActiveBotSlot] = useState<number | null>(null);
  const [isLeaving, setIsLeaving] = useState<boolean>(false);

  const handleCopyCode = () => {
    if (!lobby?.lobbyId) return;
    navigator.clipboard.writeText(lobby.lobbyId);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleLeaveRoom = async () => {
    if (lobby?.lobbyId && playerId !== null) {
      setIsLeaving(true);
      await leaveLobbyAction(lobby.lobbyId, playerId);
    }

    localStorage.removeItem("wizard_lobbyId");
    localStorage.removeItem("wizard_playerId");
    router.push("/");
  };

  const handleRemoveBot = async (botId: number) => {
    if (!lobby?.lobbyId) return;
    setRemovingBotId(botId);
    
    const result = await leaveLobbyAction(lobby.lobbyId, botId);

    if (result && (result as any).error) {
      console.error((result as any).error);
    } else {
      await refreshLobby();
    }
    setRemovingBotId(null);
  };

  const handleStartGame = async () => {
    if (lobby?.lobbyId) {
      router.push(`/game/${lobby.lobbyId}`);
    }
  };

  const handleAddBot = async (difficulty: "Dumb" | "Prolog") => {
    if (!lobby?.lobbyId) return;
    setIsAddingBot(true);
    const result = await addBotAction(lobby.lobbyId, difficulty);

    if (result.error) {
      console.error(result.error);
    } else {
      await refreshLobby();
    }
    setIsAddingBot(false);
  };

  if (!lobby && connectionState === "connecting") {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <p className="text-slate-400 animate-pulse font-medium">{lobbyI18n.loading}</p>
      </div>
    );
  }

  if (sessionError) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] gap-4">
        <p className="text-red-400 font-medium">{sessionError.message}</p>
        <Button onClick={() => router.push("/")} variant="outline">
          {lobbyI18n.backToHome}
        </Button>
      </div>
    );
  }

  const players = lobby?.players || [];
  const emptySlotsCount = Math.max(0, maxPlayers - players.length);
  const roomCode = lobby?.lobbyId || "";

  return (
    <div className="w-full max-w-5xl space-y-6">
      {connectionState === "reconnecting" && (
        <div className="bg-amber-500/10 border border-amber-500/20 text-amber-400 px-4 py-2 rounded-md text-xs text-center animate-pulse">
          {lobbyI18n.reconnecting}
        </div>
      )}

      <Card className="bg-slate-900 border-slate-800 text-slate-100">
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-4">
          <div>
            <CardTitle className="text-2xl font-bold text-white">
              {lobbyI18n.header.roomTitle}{roomCode}
            </CardTitle>
            <p className="text-sm text-slate-400 mt-1">
              {lobbyI18n.header.modePrefix}<span className="text-slate-200 font-medium">{lobbyI18n.header.modeName}</span> • {lobbyI18n.header.maxPlayers.replace("{max}", String(maxPlayers))}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              className="gap-2 border-slate-700 bg-slate-800/50 hover:bg-slate-800 text-slate-200 hover:text-white"
              onClick={handleCopyCode}
            >
              {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4 text-slate-400" />}
              <code className="font-mono text-xs">#{roomCode}</code>
            </Button>
            <Button variant="ghost" size="icon" className="hover:bg-slate-800 text-slate-400 hover:text-white">
              <Settings className="w-5 h-5" />
            </Button>
          </div>
        </CardHeader>
      </Card>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        <Card className="lg:col-span-2 bg-slate-900 border-slate-800 text-slate-100">
          <CardHeader className="pb-3">
            <CardTitle className="text-lg font-semibold text-white flex items-center justify-between">
              <span className="flex items-center gap-2">
                <Users className="w-5 h-5 text-slate-400" />
                {lobbyI18n.playersCard.title}
              </span>
              <Badge variant="secondary" className="bg-slate-800 text-slate-300 border border-slate-700">
                {players.length} / {maxPlayers}
              </Badge>
            </CardTitle>
          </CardHeader>
          <CardContent className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {players.map((player, index) => {
              const isMe = playerId !== null && Number(player.id) === Number(playerId);
              const isBot = player.difficulty !== undefined && player.difficulty !== null;
              const isRemovingThisBot = removingBotId === player.id;
              const initials = player.name ? player.name.slice(0, 2).toUpperCase() : "P";
              
              const difficultyLabel = isBot && player.difficulty
                ? typeof player.difficulty === "object"
                  ? (player.difficulty as any).level || (player.difficulty as any).name || "Std"
                  : String(player.difficulty)
                : null;

              return (
                <div
                  key={player.id || index}
                  className={`flex items-center justify-between p-3.5 rounded-xl border ${
                    isMe
                      ? "bg-slate-800/90 border-indigo-500/50 ring-1 ring-indigo-500/30"
                      : "bg-slate-800/60 border-slate-700/60"
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <Avatar className={`h-10 w-10 ${isBot ? "border border-cyan-500/50" : ""}`}>
                      <AvatarFallback className={isBot ? "bg-cyan-950 text-cyan-400" : "bg-slate-700 text-slate-200"}>
                        {isBot ? <BotIcon className="w-5 h-5" /> : initials}
                      </AvatarFallback>
                    </Avatar>
                    <div>
                      <p className="font-semibold text-sm text-slate-100 flex items-center gap-1.5">
                        {player.name}
                        {isMe && (
                          <span className="text-[10px] bg-indigo-950 text-indigo-300 border border-indigo-800/60 px-1.5 py-0.2 rounded font-mono">
                            {lobbyI18n.playersCard.youBadge}
                          </span>
                        )}
                      </p>
                      <span className="text-xs text-slate-400">
                        {isBot ? lobbyI18n.playersCard.aiPlayer : lobbyI18n.playersCard.humanPlayer}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    {isBot ? (
                      <>
                        <Badge variant="outline" className="text-cyan-400 border-cyan-500/40 bg-cyan-950/20">
                          {lobbyI18n.playersCard.botBadge.replace("{difficulty}", difficultyLabel || "")}
                        </Badge>
                        <Button
                          size="icon"
                          variant="ghost"
                          disabled={isRemovingThisBot}
                          className="h-7 w-7 text-slate-400 hover:text-red-400 hover:bg-red-950/40"
                          onClick={() => handleRemoveBot(player.id)}
                          title={lobbyI18n.playersCard.removeBotTooltip}
                        >
                          {isRemovingThisBot ? (
                            <Loader2 className="w-3.5 h-3.5 animate-spin text-red-400" />
                          ) : (
                            <X className="w-3.5 h-3.5" />
                          )}
                        </Button>
                      </>
                    ) : (
                      <Badge className="bg-emerald-950/80 text-emerald-400 border border-emerald-800/60">
                        {lobbyI18n.playersCard.readyBadge}
                      </Badge>
                    )}
                  </div>
                </div>
              );
            })}

            {Array.from({ length: emptySlotsCount }).map((_, index) => {
              const isSelecting = activeBotSlot === index;

              if (isSelecting) {
                return (
                  <div
                    key={`empty-${index}`}
                    className="flex items-center justify-between p-2 rounded-xl border border-dashed border-cyan-800/80 bg-slate-950/50 text-slate-300 text-xs font-medium"
                  >
                    <div className="flex items-center gap-1.5 text-cyan-400 pl-1">
                      <BotIcon className="w-4 h-4" />
                      <span>{lobbyI18n.botSelection.label}</span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <Button
                        size="sm"
                        variant="outline"
                        disabled={isAddingBot}
                        className="h-7 px-2 border-slate-700 bg-slate-800 hover:bg-cyan-950 hover:border-cyan-600 text-slate-200 text-xs gap-1"
                        onClick={async () => {
                          await handleAddBot("Dumb");
                          setActiveBotSlot(null);
                        }}
                      >
                        {isAddingBot && <Loader2 className="w-3 h-3 animate-spin" />}
                        {lobbyI18n.botSelection.dumb}
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        disabled={isAddingBot}
                        className="h-7 px-2 border-slate-700 bg-slate-800 hover:bg-cyan-950 hover:border-cyan-600 text-slate-200 text-xs gap-1"
                        onClick={async () => {
                          await handleAddBot("Prolog");
                          setActiveBotSlot(null);
                        }}
                      >
                        {isAddingBot && <Loader2 className="w-3 h-3 animate-spin" />}
                        {lobbyI18n.botSelection.prolog}
                      </Button>
                      <Button
                        size="icon"
                        variant="ghost"
                        disabled={isAddingBot}
                        className="h-7 w-7 text-slate-400 hover:text-slate-100"
                        onClick={() => setActiveBotSlot(null)}
                      >
                        <X className="w-3.5 h-3.5" />
                      </Button>
                    </div>
                  </div>
                );
              }

              return (
                <div
                  key={`empty-${index}`}
                  className="flex items-center justify-between p-3.5 rounded-xl border border-dashed border-slate-800 bg-slate-950/30 text-slate-500 text-sm font-medium"
                >
                  <div className="flex items-center gap-2">
                    <UserPlus className="w-4 h-4 opacity-50" />
                    <span>{lobbyI18n.waitingSlot.waiting}</span>
                  </div>
                  <Button
                    size="sm"
                    variant="ghost"
                    disabled={isAddingBot}
                    className="h-7 gap-1 px-2 border border-slate-800 hover:border-cyan-500/50 bg-slate-900 hover:bg-cyan-950/40 text-slate-400 hover:text-cyan-300 text-xs transition-colors"
                    onClick={() => setActiveBotSlot(index)}
                  >
                    <Plus className="w-3.5 h-3.5" /> {lobbyI18n.waitingSlot.addBotButton}
                  </Button>
                </div>
              );
            })}
          </CardContent>
        </Card>

        <div className="flex flex-col justify-between gap-6">
          <Card className="bg-slate-900 border-slate-800 text-slate-100 flex-1">
            <CardHeader className="pb-3">
              <CardTitle className="text-md font-medium text-slate-300">{lobbyI18n.detailsCard.title}</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 text-sm">
              <div className="flex justify-between py-1.5 border-b border-slate-800">
                <span className="text-slate-400">{lobbyI18n.detailsCard.turnTimeLabel}</span>
                <span className="font-semibold text-slate-200">{lobbyI18n.detailsCard.turnTimeValue}</span>
              </div>
              <div className="flex justify-between py-1.5 border-b border-slate-800">
                <span className="text-slate-400">{lobbyI18n.detailsCard.botCountLabel}</span>
                <span className="font-semibold text-slate-200">
                  {players.filter((p) => p.difficulty !== undefined && p.difficulty !== null).length}
                </span>
              </div>
              <div className="flex justify-between py-1.5">
                <span className="text-slate-400">{lobbyI18n.detailsCard.visibilityLabel}</span>
                <span className="font-semibold text-slate-200">{lobbyI18n.detailsCard.visibilityValue}</span>
              </div>
            </CardContent>
          </Card>

          <div className="flex gap-3">
            <Button
              onClick={handleLeaveRoom}
              disabled={isLeaving}
              variant="destructive"
              size="lg"
              className="w-1/3 gap-2 bg-red-950/80 hover:bg-red-900 border border-red-800/50 text-red-200"
            >
              {isLeaving ? <Loader2 className="w-4 h-4 animate-spin" /> : <LogOut className="w-4 h-4" />} {lobbyI18n.actions.leave}
            </Button>
            <Button
              onClick={handleStartGame}
              size="lg"
              className="w-2/3 bg-emerald-600 hover:bg-emerald-500 text-white font-bold gap-2 shadow-lg shadow-emerald-950/40"
            >
              <Check className="w-5 h-5" /> {lobbyI18n.actions.startGame}
            </Button>
          </div>
        </div>

      </div>
    </div>
  );
}