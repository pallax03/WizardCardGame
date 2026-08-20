"use client";

import React, { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { Card, CardHeader, CardTitle, CardContent } from "@/ui/components/card";
import { Avatar, AvatarFallback } from "@/ui/components/avatar";
import { Badge } from "@/ui/components/badge";
import { Button } from "@/ui/components/button";
import { Copy, Check, LogOut, Settings, Users, UserPlus, Bot as BotIcon, Plus, X, Loader2 } from "lucide-react";
import { getLobbyAction, addBotAction } from "@/features/lobby/actions/lobby-actions";
import { ApiPlayer, LobbyApiResponse } from "@/features/lobby/types/lobby-types";

interface LobbyViewProps {
  lobbyId: string;
  initialPlayerId?: string;
  currentPlayerName?: string;
  maxPlayers?: number;
}

export function LobbyView({
  lobbyId,
  initialPlayerId,
  currentPlayerName,
  maxPlayers = 6,
}: LobbyViewProps) {
  const router = useRouter();
  const [lobbyData, setLobbyData] = useState<LobbyApiResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState<boolean>(false);
  const [isAddingBot, setIsAddingBot] = useState<boolean>(false);

  const fetchLobbyDetails = async () => {
    setError(null);
    const result = await getLobbyAction(lobbyId);

    if (result.error) {
      setError(result.error);
    } else if (result.data) {
      setLobbyData(result.data);
    }
  };

  useEffect(() => {
    async function init() {
      setIsLoading(true);
      await fetchLobbyDetails();
      setIsLoading(false);
    }

    init();
  }, [lobbyId]);

  const handleCopyCode = () => {
    navigator.clipboard.writeText(roomCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleLeaveRoom = () => {
    router.push("/");
  };

  const handleStartGame = async () => {
    router.push(`/game/${lobbyId}`);
  };

  const handleAddBot = async (difficulty: "Dumb" | "Prolog") => {
    setIsAddingBot(true);
    const result = await addBotAction(lobbyId, difficulty);

    if (result.error) {
      console.error(result.error);
    } else {
      await fetchLobbyDetails();
    }
    setIsAddingBot(false);
  };

  const players = lobbyData?.players || [];
  const emptySlotsCount = Math.max(0, maxPlayers - players.length);
  const roomCode = lobbyData?.lobbyId || lobbyId;

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <p className="text-slate-400 animate-pulse font-medium">Caricamento stanza in corso...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] gap-4">
        <p className="text-red-400 font-medium">{error}</p>
        <Button onClick={() => router.push("/")} variant="outline">
          Torna alla Home
        </Button>
      </div>
    );
  }

  return (
    <div className="w-full max-w-5xl space-y-6">
      {/* Header Stanza */}
      <Card className="bg-slate-900 border-slate-800 text-slate-100">
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-4">
          <div>
            <CardTitle className="text-2xl font-bold text-white">
              Stanza #{roomCode}
            </CardTitle>
            <p className="text-sm text-slate-400 mt-1">
              Modalità: <span className="text-slate-200 font-medium">Standard</span> • MAX {maxPlayers} Giocatori
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

      {/* Layout Principale a 2 Colonne */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Lista Giocatori */}
        <Card className="lg:col-span-2 bg-slate-900 border-slate-800 text-slate-100">
          <CardHeader className="pb-3">
            <CardTitle className="text-lg font-semibold text-white flex items-center justify-between">
              <span className="flex items-center gap-2">
                <Users className="w-5 h-5 text-slate-400" />
                Giocatori Inseriti
              </span>
              <Badge variant="secondary" className="bg-slate-800 text-slate-300 border border-slate-700">
                {players.length} / {maxPlayers}
              </Badge>
            </CardTitle>
          </CardHeader>
          <CardContent className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            
            {players.map((player, index) => {
                const isMe =
                    (initialPlayerId && initialPlayerId !== "undefined" && String(player.id) === String(initialPlayerId)) ||
                    (currentPlayerName && player.name.toLowerCase() === currentPlayerName.toLowerCase());

                return (
                    <PlayerCard 
                        key={player.id || index} 
                        player={player} 
                        isCurrentPlayer={Boolean(isMe)} 
                    />
                );
            })}

            {Array.from({ length: emptySlotsCount }).map((_, index) => (
              <EmptySlotCard key={`empty-${index}`} onAddBot={handleAddBot} isDisabled={isAddingBot} />
            ))}

          </CardContent>
        </Card>

        {/* Dettagli Match & Pulsanti Azione */}
        <div className="flex flex-col justify-between gap-6">
          <Card className="bg-slate-900 border-slate-800 text-slate-100 flex-1">
            <CardHeader className="pb-3">
              <CardTitle className="text-md font-medium text-slate-300">Dettagli Match</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 text-sm">
              <div className="flex justify-between py-1.5 border-b border-slate-800">
                <span className="text-slate-400">Tempo per Turno</span>
                <span className="font-semibold text-slate-200">30s</span>
              </div>
              <div className="flex justify-between py-1.5 border-b border-slate-800">
                <span className="text-slate-400">Giocatori Bot</span>
                <span className="font-semibold text-slate-200">
                  {players.filter((p) => p.difficulty !== undefined && p.difficulty !== null).length}
                </span>
              </div>
              <div className="flex justify-between py-1.5">
                <span className="text-slate-400">Visibilità</span>
                <span className="font-semibold text-slate-200">Privata</span>
              </div>
            </CardContent>
          </Card>

          <div className="flex gap-3">
            <Button
              onClick={handleLeaveRoom}
              variant="destructive"
              size="lg"
              className="w-1/3 gap-2 bg-red-950/80 hover:bg-red-900 border border-red-800/50 text-red-200"
            >
              <LogOut className="w-4 h-4" /> Esci
            </Button>
            <Button
              onClick={handleStartGame}
              size="lg"
              className="w-2/3 bg-emerald-600 hover:bg-emerald-500 text-white font-bold gap-2 shadow-lg shadow-emerald-950/40"
            >
              <Check className="w-5 h-5" /> AVVIA PARTITA
            </Button>
          </div>
        </div>

      </div>
    </div>
  );
}

function PlayerCard({ player, isCurrentPlayer }: { player: ApiPlayer; isCurrentPlayer?: boolean }) {
  const isBot = player.difficulty !== undefined && player.difficulty !== null;
  const initials = player.name.slice(0, 2).toUpperCase() || "P";
  const difficultyLabel = isBot && player.difficulty
      ? typeof player.difficulty === "object"
        ? (player.difficulty as any).level || (player.difficulty as any).name || "Std"
        : String(player.difficulty)
      : null;

  return (
    <div className={`flex items-center justify-between p-3.5 rounded-xl border ${
      isCurrentPlayer 
        ? "bg-slate-800/90 border-indigo-500/50 ring-1 ring-indigo-500/30" 
        : "bg-slate-800/60 border-slate-700/60"
    }`}>
      <div className="flex items-center gap-3">
        <Avatar className={`h-10 w-10 ${isBot ? "border border-cyan-500/50" : ""}`}>
          <AvatarFallback className={isBot ? "bg-cyan-950 text-cyan-400" : "bg-slate-700 text-slate-200"}>
            {isBot ? <BotIcon className="w-5 h-5" /> : initials}
          </AvatarFallback>
        </Avatar>
        <div>
          <p className="font-semibold text-sm text-slate-100 flex items-center gap-1.5">
            {player.name}
            {isCurrentPlayer && <span className="text-[10px] bg-indigo-950 text-indigo-300 border border-indigo-800/60 px-1.5 py-0.2 rounded font-mono">(Tu)</span>}
          </p>
          <span className="text-xs text-slate-400">
            {isBot ? "AI Player" : "Umano"}
          </span>
        </div>
      </div>

      {isBot ? (
        <Badge variant="outline" className="text-cyan-400 border-cyan-500/40 bg-cyan-950/20">
          Bot ({difficultyLabel})
        </Badge>
      ) : (
        <Badge className="bg-emerald-950/80 text-emerald-400 border border-emerald-800/60">
          Pronto
        </Badge>
      )}
    </div>
  );
}

function EmptySlotCard({ 
  onAddBot, 
  isDisabled 
}: { 
  onAddBot: (difficulty: "Dumb" | "Prolog") => Promise<void>; 
  isDisabled?: boolean;
}) {
  const [isSelectingDifficulty, setIsSelectingDifficulty] = useState(false);
  const [loadingDifficulty, setLoadingDifficulty] = useState<"Dumb" | "Prolog" | null>(null);

  const handleSelect = async (difficulty: "Dumb" | "Prolog") => {
    setLoadingDifficulty(difficulty);
    await onAddBot(difficulty);
    setLoadingDifficulty(null);
    setIsSelectingDifficulty(false);
  };

  if (isSelectingDifficulty) {
    return (
      <div className="flex items-center justify-between p-2 rounded-xl border border-dashed border-cyan-800/80 bg-slate-950/50 text-slate-300 text-xs font-medium">
        <div className="flex items-center gap-1.5 text-cyan-400 pl-1">
          <BotIcon className="w-4 h-4" />
          <span>Difficoltà:</span>
        </div>
        <div className="flex items-center gap-1.5">
          <Button
            size="sm"
            variant="outline"
            disabled={isDisabled}
            className="h-7 px-2 border-slate-700 bg-slate-800 hover:bg-cyan-950 hover:border-cyan-600 text-slate-200 text-xs gap-1"
            onClick={() => handleSelect("Dumb")}
          >
            {loadingDifficulty === "Dumb" && <Loader2 className="w-3 h-3 animate-spin" />}
            Dumb
          </Button>
          <Button
            size="sm"
            variant="outline"
            disabled={isDisabled}
            className="h-7 px-2 border-slate-700 bg-slate-800 hover:bg-cyan-950 hover:border-cyan-600 text-slate-200 text-xs gap-1"
            onClick={() => handleSelect("Prolog")}
          >
            {loadingDifficulty === "Prolog" && <Loader2 className="w-3 h-3 animate-spin" />}
            Prolog
          </Button>
          <Button
            size="icon"
            variant="ghost"
            disabled={isDisabled}
            className="h-7 w-7 text-slate-400 hover:text-slate-100"
            onClick={() => setIsSelectingDifficulty(false)}
          >
            <X className="w-3.5 h-3.5" />
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex items-center justify-between p-3.5 rounded-xl border border-dashed border-slate-800 bg-slate-950/30 text-slate-500 text-sm font-medium">
      <div className="flex items-center gap-2">
        <UserPlus className="w-4 h-4 opacity-50" />
        <span>In attesa...</span>
      </div>
      <Button
        size="sm"
        variant="ghost"
        disabled={isDisabled}
        className="h-7 gap-1 px-2 border border-slate-800 hover:border-cyan-500/50 bg-slate-900 hover:bg-cyan-950/40 text-slate-400 hover:text-cyan-300 text-xs transition-colors"
        onClick={() => setIsSelectingDifficulty(true)}
      >
        <Plus className="w-3.5 h-3.5" /> Bot
      </Button>
    </div>
  );
}