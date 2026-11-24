"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Image from "next/image";
import { Loader2, ArrowRight, X, Globe, BookOpen } from "lucide-react";
import { createLobbyAction, joinLobbyAction } from "@/features/lobby/api";
import { getLobbyState } from "@/features/lobby-session/api";
import {
  readSavedLobbies,
  removeSavedLobby,
} from "@/features/lobby-session/storage";
import { ApiError } from "@/lib/api/api";
import { cn } from "@/lib/utils";
import { t } from "@/ui/i18n/core";
const homeI18n = t("home");
import { Button, buttonVariants } from "@/ui/components/button";
import { Input } from "@/ui/components/input";
import { getErrorMessage } from "@/ui/i18n/errors";

export default function Home() {
  const [mounted, setMounted] = useState(false);
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMounted(true);
  }, []);
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
    const saved = readSavedLobbies();

    if (lobbyId) {
      const cleanLobbyId = lobbyId.trim();
      if (saved.some((s) => s.lobbyId === cleanLobbyId)) {
        window.location.replace(`/lobby/${cleanLobbyId}`);
        return;
      }
      queueMicrotask(() => {
        setLobbyIdToJoin(cleanLobbyId);
        setShowJoinInput(true);
      });
      return;
    }
    if (saved.length === 0) return;
    queueMicrotask(() =>
      setSavedEntries(
        saved.map((entry) => ({
          lobbyId: entry.lobbyId,
          playerId: entry.playerId,
          savedAt: entry.savedAt,
          createdAt: entry.savedAt,
        })),
      ),
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
                    playerName: state.players.find(
                      (p) => p.id === entry.playerId,
                    )?.name,
                    createdAt:
                      typeof state.createdAt === "number" && state.createdAt > 0
                        ? state.createdAt
                        : entry.savedAt,
                  }
                : item,
            ),
          );
        } catch (reason) {
          if (cancelled) return;
          if (reason instanceof ApiError && reason.status === 404) {
            removeSavedLobby(entry.lobbyId);
            setSavedEntries((prev) =>
              prev.filter((item) => item.lobbyId !== entry.lobbyId),
            );
          } else {
            setSavedEntries((prev) =>
              prev.map((item) =>
                item.lobbyId === entry.lobbyId
                  ? { ...item, failed: true }
                  : item,
              ),
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
    const targetLobbyId = lobbyIdToJoin.trim().toLowerCase();
    if (!targetLobbyId) return;

    const alreadySaved = savedEntries.find(
      (e) => e.lobbyId.toLowerCase() === targetLobbyId,
    );
    if (alreadySaved) {
      router.push(`/lobby/${alreadySaved.lobbyId}`);
      return;
    }

    setIsJoining(true);
    setError(null);

    const result = await joinLobbyAction(username, targetLobbyId);

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
    <main className="app-page min-h-svh relative flex flex-col items-center justify-center p-4 bg-zinc-950 overflow-hidden">
      <div className="fixed top-4 left-4 z-50">
        <Button
          suppressHydrationWarning
          variant="outline"
          size="sm"
          className="gap-2 border-zinc-800 bg-zinc-900/50 backdrop-blur text-zinc-300 rounded-full cursor-pointer"
          onClick={() => {
            const isEn = document.cookie.includes("wizard_lang=en");
            document.cookie = `wizard_lang=${isEn ? "it" : "en"}; path=/; max-age=31536000`;
            window.location.reload();
          }}
        >
          <Globe className="w-4 h-4" />
          {mounted
            ? document.cookie.includes("wizard_lang=en")
              ? "IT"
              : "EN"
            : "EN"}
        </Button>
      </div>

      <div className="fixed top-4 right-4 z-50 flex items-center gap-2">
        <a
          suppressHydrationWarning
          href="https://pallax03.github.io/WizardCardGame/"
          target="_blank"
          rel="noopener noreferrer"
          title={mounted ? homeI18n.links.reportTooltip : "Technical Report"}
          aria-label={
            mounted ? homeI18n.links.reportTooltip : "Technical Report"
          }
          className={cn(
            buttonVariants({ variant: "outline", size: "sm" }),
            "gap-2 border-zinc-800 bg-zinc-900/50 backdrop-blur text-zinc-300 hover:text-white rounded-full",
          )}
        >
          <BookOpen className="w-4 h-4" />
          <span>{mounted ? homeI18n.links.reportLabel : "Report"}</span>
        </a>
        <a
          suppressHydrationWarning
          href="https://github.com/pallax03/WizardCardGame"
          target="_blank"
          rel="noopener noreferrer"
          title={mounted ? homeI18n.links.githubTooltip : "GitHub Repository"}
          aria-label={
            mounted ? homeI18n.links.githubTooltip : "GitHub Repository"
          }
          className={cn(
            buttonVariants({ variant: "outline", size: "icon-sm" }),
            "border-zinc-800 bg-zinc-900/50 backdrop-blur text-zinc-300 hover:text-white rounded-full",
          )}
        >
          <svg
            viewBox="0 0 24 24"
            aria-hidden="true"
            className="w-4 h-4 fill-current"
          >
            <path d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12" />
          </svg>
        </a>
      </div>

      <div className="relative z-10 w-full max-w-sm flex flex-col items-center gap-8">
        {/* Logo only, no duplicate text */}
        <Image
          src="/wizard_logo.svg"
          alt="Wizard"
          width={280}
          height={120}
          className="w-full max-w-70 drop-shadow-2xl"
          priority
          fetchPriority="high"
        />

        {/* Unified Card for everything */}
        <div className="w-full bg-zinc-900 border border-zinc-800 rounded-3xl p-5 shadow-2xl space-y-5 relative">
          <div className="space-y-1">
            <Input
              placeholder={mounted ? homeI18n.card.usernamePlaceholder : ""}
              value={username}
              onChange={(e) => {
                setUsername(e.target.value);
                if (error) setError(null);
              }}
              onKeyDown={handleEnterKey}
              className="bg-zinc-950/80 border-zinc-800 text-center text-lg h-14 rounded-2xl focus-visible:ring-zinc-600"
            />
            {error && (
              <p className="text-xs text-red-400 font-medium text-center">
                {error}
              </p>
            )}
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Button
              type="button"
              onClick={handleToggleJoin}
              disabled={isCreating || isJoining}
              variant={showJoinInput ? "outline" : "secondary"}
              className="h-12 rounded-xl font-bold cursor-pointer transition-colors"
            >
              {showJoinInput ? (
                <X className="w-5 h-5" />
              ) : mounted ? (
                homeI18n.buttons.join
              ) : (
                ""
              )}
            </Button>

            <Button
              type="button"
              onClick={handleCreateLobby}
              disabled={isCreating || isJoining}
              variant="default"
              className="h-12 rounded-xl font-bold cursor-pointer transition-colors"
            >
              {isCreating ? (
                <Loader2 className="w-5 h-5 animate-spin" />
              ) : mounted ? (
                homeI18n.buttons.createLobby
              ) : (
                ""
              )}
            </Button>
          </div>

          {showJoinInput && (
            <div className="flex gap-2 animate-in fade-in-50 slide-in-from-top-2">
              <Input
                placeholder={
                  mounted ? homeI18n.joinSection.lobbyCodePlaceholder : ""
                }
                value={lobbyIdToJoin}
                autoFocus
                onChange={(e) => {
                  setLobbyIdToJoin(e.target.value);
                  if (error) setError(null);
                }}
                onKeyDown={handleEnterKey}
                className="bg-zinc-950 border-zinc-800 text-center text-lg h-12 rounded-xl font-mono"
              />
              <Button
                type="button"
                onClick={handleJoinLobby}
                disabled={isJoining || !lobbyIdToJoin.trim()}
                variant="default"
                className="w-12 h-12 p-0 shrink-0 rounded-xl cursor-pointer"
              >
                {isJoining ? (
                  <Loader2 className="w-5 h-5 animate-spin" />
                ) : (
                  <ArrowRight className="w-5 h-5" />
                )}
              </Button>
            </div>
          )}

          {/* Saved Lobbies */}
          {mounted && savedEntries.length > 0 && (
            <div className="pt-4 border-t border-zinc-800 space-y-2">
              <p className="text-xs font-semibold text-zinc-500 uppercase tracking-wider px-1">
                {mounted ? homeI18n.savedLobbies.title : ""}
              </p>
              {savedEntries.map((entry) => (
                <div
                  key={entry.lobbyId}
                  className="flex items-center gap-2 p-2 rounded-xl bg-zinc-950/50 border border-zinc-800/50 hover:border-zinc-700 transition-colors"
                >
                  <div
                    className="flex-1 flex flex-col cursor-pointer"
                    onClick={() => handleRejoinSaved(entry.lobbyId)}
                  >
                    <span className="text-sm font-medium text-zinc-200">
                      {entry.playerName ??
                        (mounted ? homeI18n.savedLobbies.unknownPlayer : "")}
                    </span>
                    {entry.createdAt ? (
                      <span className="text-[10px] text-zinc-500">
                        {homeI18n.savedLobbies.createdAt(
                          formatLobbyDate(entry.createdAt),
                        )}
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
                    onClick={(e) => {
                      e.stopPropagation();
                      handleRemoveSaved(entry.lobbyId);
                    }}
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
