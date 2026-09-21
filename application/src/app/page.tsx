"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { Sparkles, PlusCircle, LogIn, Loader2, ArrowRight, X, Users, Globe, History } from "lucide-react";
import { createLobbyAction, joinLobbyAction } from "@/features/lobby/api";
import { getLobbyState } from "@/features/lobby-session/api";
import { readSavedLobbies, removeSavedLobby } from "@/features/lobby-session/storage";
import { ApiError } from "@/lib/api/api";
import { t } from "@/ui/i18n/core";
const homeI18n = t("home");
import { Button } from "@/ui/components/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/ui/components/card";
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
    { lobbyId: string; playerId: number; status?: string; playersCount?: number; failed?: boolean }[]
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
      setSavedEntries(saved.map((entry) => ({ lobbyId: entry.lobbyId, playerId: entry.playerId })))
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
                ? { ...item, status: state.status, playersCount: state.players.length }
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
    <main className="app-page relative flex flex-col items-center justify-center p-4 selection:bg-purple-500/30 overflow-hidden">
      <div aria-hidden="true" className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_50%_-15%,rgba(99,102,241,0.28),transparent_42%)]" />
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,var(--tw-gradient-stops))] from-indigo-900/20 via-zinc-950 to-zinc-950 pointer-events-none" />

      {/* Language Switcher */}
      <div className="absolute top-4 right-4 z-20">
        <Button
          suppressHydrationWarning
          variant="outline"
          size="sm"
          className="gap-2 border-zinc-200 text-zinc-300"
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

      <div className="relative z-10 w-full max-w-md space-y-8">

        {savedEntries.length > 0 && (
          <Card className="bg-zinc-900/80 border-zinc-800 backdrop-blur-md shadow-2xl">
            <CardHeader>
              <div className="mb-2 grid size-10 place-items-center rounded-2xl bg-sky-500/15 text-sky-300"><History className="size-5" /></div>
              <CardTitle className="text-lg text-zinc-100 font-semibold">{homeI18n.savedLobbies.title}</CardTitle>
              <CardDescription className="text-zinc-400 text-sm">{homeI18n.savedLobbies.subtitle}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-2">
              {savedEntries.map((entry) => (
                <div
                  key={entry.lobbyId}
                  className="flex items-center gap-2 p-2.5 rounded-xl border border-zinc-800 bg-zinc-950/60"
                >
                  <div className="min-w-0 flex-1">
                    <p className="font-mono text-sm text-zinc-100 truncate">{entry.lobbyId}</p>
                    <p className="text-[11px] text-zinc-500">
                      {savedStatusLabel(entry.status, entry.failed)}
                      {entry.playersCount !== undefined &&
                        ` · ${homeI18n.savedLobbies.players(entry.playersCount)}`}
                    </p>
                  </div>
                  <Button
                    type="button"
                    size="sm"
                    onClick={() => handleRejoinSaved(entry.lobbyId)}
                    className="gap-1 shrink-0"
                  >
                    {homeI18n.savedLobbies.rejoin} <ArrowRight className="w-3.5 h-3.5" />
                  </Button>
                  <Button
                    type="button"
                    size="icon"
                    variant="ghost"
                    onClick={() => handleRemoveSaved(entry.lobbyId)}
                    title={homeI18n.savedLobbies.removeTitle}
                    className="h-8 w-8 shrink-0 text-zinc-500 hover:text-red-400 hover:bg-red-950/40"
                  >
                    <X className="w-3.5 h-3.5" />
                  </Button>
                </div>
              ))}
            </CardContent>
          </Card>
        )}
        <div className="flex flex-col items-center gap-3 text-center">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 text-xs font-medium backdrop-blur-md">
            <Sparkles className="w-3.5 h-3.5" /> {homeI18n.badge}
          </div>
          <h1 suppressHydrationWarning className="text-6xl md:text-7xl font-extrabold tracking-tighter text-transparent bg-clip-text bg-linear-to-br from-indigo-200 via-purple-300 to-pink-300 drop-shadow-sm">
            {homeI18n.title}
          </h1>
          <p suppressHydrationWarning className="text-zinc-400 text-sm font-light">
            {homeI18n.subtitle}
          </p>
        </div>

        <Card className="bg-zinc-900/80 border-zinc-800 backdrop-blur-md shadow-2xl">
          <CardHeader>
            <div className="mb-2 grid size-10 place-items-center rounded-2xl bg-indigo-500/15 text-indigo-300"><Users className="size-5" /></div>
            <CardTitle className="text-lg text-zinc-100 font-semibold">{homeI18n.card.title}</CardTitle>
            <CardDescription className="text-zinc-400 text-sm">{homeI18n.card.description}</CardDescription>
          </CardHeader>
          <CardContent className="space-y-5">
            <div className="space-y-2">
              <Input
                placeholder={homeI18n.card.usernamePlaceholder}
                value={username}
                onChange={(e) => {
                  setUsername(e.target.value);
                  if (error) setError(null);
                }}
                onKeyDown={handleEnterKey}
                className="bg-zinc-950/60 border-zinc-800 text-zinc-100 focus-visible:ring-indigo-500 h-11"
              />
              {error && <p className="text-xs text-red-400 font-medium pl-1">{error}</p>}
            </div>

            <div className="space-y-3 pt-2">
              <div className="grid grid-cols-2 gap-3">
                <Button
                  type="button"
                  onClick={handleToggleJoin}
                  disabled={isCreating || isJoining}
                  variant={showJoinInput ? "outline" : "secondary"}
                  size="lg"
                  className="gap-2 transition-all cursor-pointer"
                >
                  {showJoinInput ? (
                    <>
                      <X className="w-4 h-4" /> {homeI18n.buttons.close}
                    </>
                  ) : (
                    <>
                      <LogIn className="w-4 h-4" /> {homeI18n.buttons.join}
                    </>
                  )}
                </Button>

                <Button
                  type="button"
                  onClick={handleCreateLobby}
                  disabled={isCreating || isJoining}
                  size="lg"
                  className="gap-2 font-medium transition-all cursor-pointer"
                >
                  {isCreating ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <>
                      <PlusCircle className="w-4 h-4" /> {homeI18n.buttons.createLobby}
                    </>
                  )}
                </Button>
              </div>

              {showJoinInput && (
                <div className="p-3 rounded-lg bg-zinc-950/80 border border-zinc-800 space-y-3 animate-in fade-in-50 slide-in-from-top-2 duration-200">
                  <label className="text-xs font-medium text-zinc-400 block">
                    {homeI18n.joinSection.label}
                  </label>
                  <div className="flex gap-2">
                    <Input
                      placeholder={homeI18n.joinSection.lobbyCodePlaceholder}
                      value={lobbyIdToJoin}
                      autoFocus
                      onChange={(e) => {
                        setLobbyIdToJoin(e.target.value);
                        if (error) setError(null);
                      }}
                      onKeyDown={handleEnterKey}
                      className="bg-zinc-900 border-zinc-800 text-zinc-100 focus-visible:ring-primary h-11 font-mono uppercase text-sm"
                    />
                    <Button
                      type="button"
                      onClick={handleJoinLobby}
                      disabled={isJoining || !lobbyIdToJoin.trim()}
                      size="lg"
                      className="px-4 font-medium gap-1 shrink-0 cursor-pointer"
                    >
                      {isJoining ? (
                        <Loader2 className="w-4 h-4 animate-spin" />
                      ) : (
                        <>
                          {homeI18n.buttons.enter} <ArrowRight className="w-4 h-4" />
                        </>
                      )}
                    </Button>
                  </div>
                </div>
              )}

            </div>
          </CardContent>
        </Card>

      </div>
    </main>
  );
}