"use client";

import { useEffect, useState } from "react";
import { Button } from "@/ui/components/button";
import { t } from "@/ui/i18n/core";
const lobbyI18n = t("lobby");
import { LogOut, Check, Play, Loader2, Trash2, TriangleAlert, Pause, Home } from "lucide-react";
import { LobbyActionsProps } from "../types";

export function LobbyActions({ isLeaving, isStarting, onLeave, onStart, isResuming, isDiscarding, onDiscard, disableStart, discardMode = "paused", status = "WAITING", isPausing, onPause, onExitHome }: LobbyActionsProps) {
  const busy = isLeaving || isStarting || isDiscarding || isPausing;
  const [confirmingDiscard, setConfirmingDiscard] = useState(false);
  const [confirmingLeave, setConfirmingLeave] = useState(false);

  useEffect(() => {
    if (!confirmingDiscard) return;
    const timer = setTimeout(() => setConfirmingDiscard(false), 5000);
    return () => clearTimeout(timer);
  }, [confirmingDiscard]);

  useEffect(() => {
    if (!confirmingLeave) return;
    const timer = setTimeout(() => setConfirmingLeave(false), 5000);
    return () => clearTimeout(timer);
  }, [confirmingLeave]);

  useEffect(() => {
    queueMicrotask(() => {
      setConfirmingLeave(false);
      setConfirmingDiscard(false);
    });
  }, [status]);

  const handleDiscardClick = () => {
    if (!onDiscard || isDiscarding) return;
    if (!confirmingDiscard) {
      setConfirmingDiscard(true);
      return;
    }
    setConfirmingDiscard(false);
    onDiscard();
  };

  const handleLeaveClick = () => {
    if (isLeaving) return;
    if (!confirmingLeave) {
      setConfirmingLeave(true);
      return;
    }
    setConfirmingLeave(false);
    onLeave();
  };

  const isWaiting = status === "WAITING";
  const isPaused = status === "PAUSED";
  const isDisconnecting = status === "DISCONNECTING";
  const isFinished = status === "FINISHED";

  return (
    <div className="space-y-3">
      <div className="flex gap-3">
        {isWaiting ? (
          <Button
            onClick={handleLeaveClick}
            disabled={busy}
            variant={confirmingLeave ? "destructive" : "outline"}
            size="lg"
            className={
              confirmingLeave
                ? "w-1/3 gap-2 font-semibold"
                : "w-1/3 gap-2 border-zinc-700 bg-transparent text-zinc-300 hover:text-red-300 hover:border-red-500/50 hover:bg-red-950/30"
            }
          >
            {isLeaving ? <Loader2 className="w-4 h-4 animate-spin" /> : <LogOut className="w-4 h-4" />}{" "}
            {isLeaving
              ? lobbyI18n.actions.leaving
              : confirmingLeave
                ? lobbyI18n.actions.leaveConfirm
                : lobbyI18n.actions.leave}
          </Button>
        ) : (
          onExitHome && (
            <Button
              onClick={onExitHome}
              disabled={busy}
              variant="outline"
              size="lg"
              className="w-1/3 gap-2 border-zinc-700 bg-transparent text-zinc-300 hover:text-indigo-300 hover:border-indigo-500/50 hover:bg-indigo-950/30"
              title={lobbyI18n.actions.exitHomeHint}
            >
              <Home className="w-4 h-4" /> {lobbyI18n.actions.exitHome}
            </Button>
          )
        )}
        <Button
          onClick={onStart}
          disabled={busy || disableStart || isDisconnecting}
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
      {isDisconnecting && onPause && (
        <Button
          onClick={onPause}
          disabled={busy}
          variant="outline"
          size="sm"
          className="w-full gap-2 border-amber-500/50 bg-amber-950/30 text-amber-300 hover:text-amber-200 hover:bg-amber-900/40"
        >
          {isPausing ? <Loader2 className="w-4 h-4 animate-spin" /> : <Pause className="w-4 h-4" />}{" "}
          {isPausing ? lobbyI18n.actions.pausing : lobbyI18n.actions.pauseGame}
        </Button>
      )}
      {onDiscard && (isPaused || isFinished) && (
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
      {!isWaiting && onExitHome && (
        <p className="text-center text-[11px] text-zinc-500">
          {lobbyI18n.actions.exitHomeHint}
        </p>
      )}
    </div>
  );
}
