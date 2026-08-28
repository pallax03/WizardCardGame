"use client";

import { useState } from "react";
import { Sparkles, PlusCircle, LogIn, Loader2, ArrowRight, X, Users } from "lucide-react";
import { createLobbyAction, joinLobbyAction } from "@/features/lobby/actions/join-actions";
import { homeI18n } from "@/i18n/home";
import { Button } from "@/ui/components/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/ui/components/card";
import { Input } from "@/ui/components/input";
import { getErrorMessage } from "@/ui/i18n/errors";

export default function Home() {
  const [username, setUsername] = useState("");
  const [lobbyIdToJoin, setLobbyIdToJoin] = useState("");
  const [showJoinInput, setShowJoinInput] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [isJoining, setIsJoining] = useState(false);
  const [error, setError] = useState<string | null>(null);

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

  return (
    <main className="relative min-h-screen bg-zinc-950 text-zinc-100 flex flex-col items-center justify-center p-4 selection:bg-purple-500/30 overflow-hidden">
      <div aria-hidden="true" className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_50%_-15%,rgba(99,102,241,0.28),transparent_42%)]" />
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,var(--tw-gradient-stops))] from-indigo-900/20 via-zinc-950 to-zinc-950 pointer-events-none" />

      <div className="relative z-10 w-full max-w-md space-y-8">

        <div className="flex flex-col items-center gap-3 text-center">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 text-xs font-medium backdrop-blur-md">
            <Sparkles className="w-3.5 h-3.5" /> {homeI18n.badge}
          </div>
          <h1 className="text-6xl md:text-7xl font-extrabold tracking-tighter text-transparent bg-clip-text bg-linear-to-br from-indigo-200 via-purple-300 to-pink-300 drop-shadow-sm">
            {homeI18n.title}
          </h1>
          <p className="text-zinc-400 text-sm font-light">
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
                className="bg-zinc-950/60 border-zinc-800 text-zinc-100 focus-visible:ring-indigo-500 h-11"
              />
              {error && <p className="text-xs text-red-400 font-medium pl-1">{error}</p>}
            </div>

            <div className="space-y-3 pt-2">
              <div className="grid grid-cols-2 gap-3">

                <Button
                  onClick={handleCreateLobby}
                  disabled={isCreating || isJoining}
                  className="h-11 bg-indigo-500 hover:bg-indigo-400 text-white font-medium shadow-lg shadow-indigo-600/20 transition-all gap-2"
                >
                  {isCreating ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <>
                      <PlusCircle className="w-4 h-4" /> {homeI18n.buttons.createRoom}
                    </>
                  )}
                </Button>

                <Button
                  onClick={() => {
                    setShowJoinInput(!showJoinInput);
                    if (error) setError(null);
                  }}
                  disabled={isCreating || isJoining}
                  className={`h-11 bg-indigo-500 hover:bg-indigo-400 text-white transition-all gap-2 ${
                    showJoinInput ? "border-indigo-500/50 bg-indigo-500/10 text-indigo-300" : ""
                  }`}
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
              </div>

              {showJoinInput && (
                <div className="p-3 rounded-lg bg-zinc-950/80 border border-zinc-800 space-y-3 animate-in fade-in-50 slide-in-from-top-2 duration-200">
                  <label className="text-xs font-medium text-zinc-400 block">
                    {homeI18n.joinSection.label}
                  </label>
                  <div className="flex gap-2">
                    <Input
                      placeholder={homeI18n.joinSection.roomCodePlaceholder}
                      value={lobbyIdToJoin}
                      onChange={(e) => {
                        setLobbyIdToJoin(e.target.value);
                        if (error) setError(null);
                      }}
                      className="bg-zinc-900 border-zinc-800 text-zinc-100 focus-visible:ring-indigo-500 h-10 font-mono uppercase text-sm"
                    />
                    <Button
                      onClick={handleJoinLobby}
                      disabled={isJoining || !lobbyIdToJoin.trim()}
                      className="h-10 px-4 bg-indigo-600 hover:bg-indigo-500 text-white font-medium gap-1 shrink-0"
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