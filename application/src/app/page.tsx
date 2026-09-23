"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Image from "next/image";
import { Loader2, ArrowRight, X, Globe } from "lucide-react";
import { createLobbyAction, joinLobbyAction } from "@/features/lobby/api";
import { getLobbyState } from "@/features/lobby-session/api";
import { readSavedLobbies, removeSavedLobby } from "@/features/lobby-session/storage";
import { ApiError } from "@/lib/api/api";
import { t } from "@/ui/i18n/core";
const homeI18n = t("home");
import { Button } from "@/ui/components/button";
import { Input } from "@/ui/components/input";
import { getErrorMessage } from "@/ui/i18n/errors";

export default function Home() {
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [lobbyIdToJoin, setLobbyIdToJoin] = useState("");
  const [showJoinInput, setShowJoinInput] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [isJoining, setIsJoining] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [savedEntries, setSavedEntries] = useState<
    {
      lobbyId: string;
      playerId: number;
      savedAt: number;
      status?: string;
      playersCount?: number;
      playerName?: string;
      createdAt?: number;
      failed?: boolean;
    }[]
  >([]);

  useEffect(() => {
    const searchParams = new URLSearchParams(window.location.search);
    const lobbyId = searchParams.get("lobbyId");

    if (lobbyId) {
      queueMicrotask(() => {
        setLobbyIdToJoin(lobbyId);
        setShowJoinInput(true);
      });
      return;
    }

    const saved = readSavedLobbies();
    if (saved.length === 0) return;
    queueMicrotask(() =>
      setSavedEntries(
        saved.map((entry) => ({
          lobbyId: entry.lobbyId,
          playerId: entry.playerId,
          savedAt: entry.savedAt,
          createdAt: entry.savedAt,
        }))
      )
    );
    let cancelled = false;
    void (async () => {
      for (const entry of saved) {
        try {
          const state = await getLobbyState(entry.lobbyId);
          if (cancelled) return;
          setSavedEntries((prev) =>
            prev.map((item) =>
              item.lobbyId === entry.lobbyId
                ? {
                    ...item,
                    status: state.status,
                    playersCount: state.players.length,
                    playerName: state.players.find((p) => p.id === entry.playerId)?.name,
                    createdAt:
                      typeof state.createdAt === "number" && state.createdAt > 0
                        ? state.createdAt
                        : entry.savedAt,
                  }
                : item
            )
          );
        } catch (reason) {
          if (cancelled) return;
          if (reason instanceof ApiError && reason.status === 404) {
            removeSavedLobby(entry.lobbyId);
            setSavedEntries((prev) => prev.filter((item) => item.lobbyId !== entry.lobbyId));
          } else {
            setSavedEntries((prev) =>
              prev.map((item) =>
                item.lobbyId === entry.lobbyId ? { ...item, failed: true } : item
              )
            );
          }
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const handleToggleJoin = () => {
    setError(null);
    setShowJoinInput((prev) => !prev);
  };

  const handleCreateLobby = async () => {
    setIsCreating(true);
    setError(null);

    const result = await createLobbyAction(username);

    if (result?.error) {
      setError(getErrorMessage(result.error));
      setIsCreating(false);
    }
  };

  const handleJoinLobby = async () => {
    setIsJoining(true);
    setError(null);

    const result = await joinLobbyAction(username, lobbyIdToJoin);

    if (result?.error) {
      setError(getErrorMessage(result.error));
      setIsJoining(false);
    }
  };

  const handleEnterKey = (e: React.KeyboardEvent) => {
    if (e.key !== "Enter") return;
    e.preventDefault();
    if (isCreating || isJoining) return;
    if (lobbyIdToJoin.trim()) {
      void handleJoinLobby();
    } else {
      void handleCreateLobby();
    }
  };

  const handleRejoinSaved = (lobbyId: string) => {
    router.push(`/lobby/${lobbyId}`);
  };

  const handleRemoveSaved = (lobbyId: string) => {
    removeSavedLobby(lobbyId);
    setSavedEntries((prev) => prev.filter((item) => item.lobbyId !== lobbyId));
  };

  const formatLobbyDate = (timestamp: number) => {
    const date = new Date(timestamp);
    if (Number.isNaN(date.getTime())) return "";
    return date.toLocaleString(undefined, {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const savedStatusLabel = (status?: string, failed?: boolean) => {
    if (failed) return homeI18n.savedLobbies.unavailable;
    switch (status) {
      case "WAITING":
        return homeI18n.savedLobbies.statusWaiting;
      case "IN_GAME":
        return homeI18n.savedLobbies.statusInGame;
      case "DISCONNECTING":
        return homeI18n.savedLobbies.statusDisconnecting;
      case "PAUSED":
        return homeI18n.savedLobbies.statusPaused;
      case "FINISHED":
        return homeI18n.savedLobbies.statusFinished;
      default:
        return homeI18n.savedLobbies.loading;
    }
  };

  return (
    <main className="app-page min-h-[100dvh] relative flex flex-col items-center justify-center p-4 bg-zinc-950 overflow-hidden">
      <div className="fixed top-4 right-4 z-50">
        <Button
          suppressHydrationWarning
          variant="outline"
          size="sm"
          className="gap-2 border-zinc-800 bg-zinc-900/50 backdrop-blur text-zinc-300 rounded-full cursor-pointer"
          onClick={() => {
            const isEn = document.cookie.includes('wizard_lang=en');
            document.cookie = `wizard_lang=${isEn ? 'it' : 'en'}; path=/; max-age=31536000`;
            window.location.reload();
          }}
        >
          <Globe className="w-4 h-4" />
          {typeof document !== 'undefined' && document.cookie.includes('wizard_lang=en') ? 'IT' : 'EN'}
        </Button>
      </div>

      <div className="relative z-10 w-full max-w-sm flex flex-col items-center gap-8">
        
        {/* Logo only, no duplicate text */}
        <Image 
          src="/wizard_logo.svg" 
          alt="Wizard" 
          width={280} 
          height={120} 
          className="w-full max-w-[280px] drop-shadow-2xl" 
          priority 
        />

        {/* Unified Card for everything */}
        <div className="w-full bg-zinc-900 border border-zinc-800 rounded-3xl p-5 shadow-2xl space-y-5 relative">
          
          <div className="space-y-1">
            <Input
              placeholder={homeI18n.card.usernamePlaceholder}
              value={username}
              onChange={(e) => {
                setUsername(e.target.value);
                if (error) setError(null);
              }}
              onKeyDown={handleEnterKey}
              className="bg-zinc-950/80 border-zinc-800 text-center text-lg h-14 rounded-2xl focus-visible:ring-zinc-600"
            />
            {error && <p className="text-xs text-red-400 font-medium text-center">{error}</p>}
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Button
              type="button"
              onClick={handleToggleJoin}
              disabled={isCreating || isJoining}
              variant={showJoinInput ? "outline" : "secondary"}
              className="h-12 rounded-xl font-bold cursor-pointer transition-colors"
            >
              {showJoinInput ? <X className="w-5 h-5" /> : homeI18n.buttons.join}
            </Button>

            <Button
              type="button"
              onClick={handleCreateLobby}
              disabled={isCreating || isJoining}
              variant="default"
              className="h-12 rounded-xl font-bold cursor-pointer transition-colors"
            >
              {isCreating ? <Loader2 className="w-5 h-5 animate-spin" /> : homeI18n.buttons.createLobby}
            </Button>
          </div>

          {showJoinInput && (
            <div className="flex gap-2 animate-in fade-in-50 slide-in-from-top-2">
              <Input
                placeholder={homeI18n.joinSection.lobbyCodePlaceholder}
                value={lobbyIdToJoin}
                autoFocus
                onChange={(e) => {
                  setLobbyIdToJoin(e.target.value);
                  if (error) setError(null);
                }}
                onKeyDown={handleEnterKey}
                className="bg-zinc-950 border-zinc-800 text-center text-lg h-12 rounded-xl font-mono uppercase"
              />
              <Button
                type="button"
                onClick={handleJoinLobby}
                disabled={isJoining || !lobbyIdToJoin.trim()}
                variant="default"
                className="w-12 h-12 p-0 shrink-0 rounded-xl cursor-pointer"
              >
                {isJoining ? <Loader2 className="w-5 h-5 animate-spin" /> : <ArrowRight className="w-5 h-5" />}
              </Button>
            </div>
          )}
          
          {/* Saved Lobbies */}
          {savedEntries.length > 0 && (
            <div className="pt-4 border-t border-zinc-800 space-y-2">
              <p className="text-xs font-semibold text-zinc-500 uppercase tracking-wider px-1">
                {homeI18n.savedLobbies.title}
              </p>
              {savedEntries.map((entry) => (
                <div key={entry.lobbyId} className="flex items-center gap-2 p-2 rounded-xl bg-zinc-950/50 border border-zinc-800/50 hover:border-zinc-700 transition-colors">
                  <div 
                    className="flex-1 flex flex-col cursor-pointer"
                    onClick={() => handleRejoinSaved(entry.lobbyId)}
                  >
                    <span className="text-sm font-medium text-zinc-200">
                      {entry.playerName ?? homeI18n.savedLobbies.unknownPlayer}
                    </span>
                    {entry.createdAt ? (
                      <span className="text-[10px] text-zinc-500">
                        {homeI18n.savedLobbies.createdAt(formatLobbyDate(entry.createdAt))}
                      </span>
                    ) : null}
                    <span className="text-[10px] text-zinc-500">
                      {savedStatusLabel(entry.status, entry.failed)}
                      {typeof entry.playersCount === "number"
                        ? ` • ${homeI18n.savedLobbies.players(entry.playersCount)}`
                        : null}
                    </span>
                  </div>
                  <Button
                    type="button"
                    size="icon"
                    variant="ghost"
                    onClick={(e) => { e.stopPropagation(); handleRemoveSaved(entry.lobbyId); }}
                    className="h-8 w-8 text-zinc-600 hover:text-red-400 cursor-pointer"
                  >
                    <X className="w-4 h-4" />
                  </Button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </main>
  );
}