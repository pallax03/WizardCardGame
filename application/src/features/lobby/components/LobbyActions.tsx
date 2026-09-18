"use client";

import { useEffect, useState } from "react";
import { Button } from "@/ui/components/button";
import { t } from "@/ui/i18n/core";
const lobbyI18n = t("lobby");
import { LogOut, Check, Play, Loader2, Trash2, TriangleAlert } from "lucide-react";
import { LobbyActionsProps } from "../types";

export function LobbyActions({ isLeaving, isStarting, onLeave, onStart, isResuming, isDiscarding, onDiscard, disableStart, discardMode = "paused" }: LobbyActionsProps) {
  const busy = isLeaving || isStarting || isDiscarding;
  const [confirmingDiscard, setConfirmingDiscard] = useState(false);

  useEffect(() => {
    if (!confirmingDiscard) return;
    const timer = setTimeout(() => setConfirmingDiscard(false), 5000);
    return () => clearTimeout(timer);
  }, [confirmingDiscard]);

  const handleDiscardClick = () => {
    if (!onDiscard || isDiscarding) return;
    if (!confirmingDiscard) {
      setConfirmingDiscard(true);
      return;
    }
    setConfirmingDiscard(false);
    onDiscard();
  };

  return (
    <div className="space-y-3">
      <div className="flex gap-3">
        <Button
          onClick={onLeave}
          disabled={busy}
          variant="destructive"
          size="lg"
          className="w-1/3 gap-2"
        >
          {isLeaving ? <Loader2 className="w-4 h-4 animate-spin" /> : <LogOut className="w-4 h-4" />} {lobbyI18n.actions.leave}
        </Button>
        <Button
          onClick={onStart}
          disabled={busy || disableStart}
          size="lg"
          className="w-2/3 bg-emerald-600 hover:bg-emerald-500 text-white font-bold gap-2 shadow-lg shadow-emerald-950/40"
        >
          {isStarting ? (
            <Loader2 className="w-5 h-5 animate-spin" />
          ) : isResuming ? (
            <Play className="w-5 h-5" />
          ) : (
            <Check className="w-5 h-5" />
          )}{" "}
          {isResuming ? lobbyI18n.actions.resumeGame : lobbyI18n.actions.startGame}
        </Button>
      </div>
      {onDiscard && (
        <div className="space-y-1.5">
          <Button
            onClick={handleDiscardClick}
            disabled={busy}
            variant={confirmingDiscard ? "destructive" : "outline"}
            size="sm"
            className={
              confirmingDiscard
                ? "w-full gap-2 font-semibold"
                : "w-full gap-2 border-zinc-700 bg-transparent text-zinc-400 hover:text-red-300 hover:border-red-500/50 hover:bg-red-950/30"
            }
          >
            {isDiscarding ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : confirmingDiscard ? (
              <TriangleAlert className="w-4 h-4" />
            ) : (
              <Trash2 className="w-4 h-4" />
            )}{" "}
            {isDiscarding
              ? lobbyI18n.actions.discarding
              : confirmingDiscard
                ? (discardMode === "finished"
                    ? lobbyI18n.actions.discardFinishedConfirm
                    : lobbyI18n.actions.discardPausedConfirm)
                : (discardMode === "finished"
                    ? lobbyI18n.actions.discardFinished
                    : lobbyI18n.actions.discardPaused)}
          </Button>
          {!confirmingDiscard && (
            <p className="text-center text-[11px] text-zinc-500">
              {lobbyI18n.actions.discardPausedHint}
            </p>
          )}
        </div>
      )}
    </div>
  );
}