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
  const showDiscardMain = (isPaused || isFinished) && onDiscard;

  const discardLabel =
    discardMode === "finished"
      ? lobbyI18n.actions.discardFinished
      : lobbyI18n.actions.discardPaused;

  return (
    <div className="flex flex-col gap-4">
      {/* Primary Action Button (Start/Resume) */}
      <Button
        onClick={onStart}
        disabled={busy || disableStart || isDisconnecting}
        variant="default"
        className="w-full h-12 rounded-xl font-bold cursor-pointer transition-colors"
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

      {/* Secondary Action Button (Discard/Leave/Home) */}
      {showDiscardMain ? (
        <Button
          onClick={handleDiscardClick}
          disabled={busy}
          variant={confirmingDiscard ? "destructive" : "secondary"}
          className={`w-full h-12 rounded-xl font-bold cursor-pointer transition-colors ${!confirmingDiscard ? 'text-red-400 hover:text-red-300' : ''}`}
        >
          {isDiscarding ? (
            <Loader2 className="w-5 h-5 animate-spin" />
          ) : confirmingDiscard ? (
            <TriangleAlert className="w-5 h-5" />
          ) : (
            <Trash2 className="w-5 h-5" />
          )}{" "}
          {isDiscarding ? (
            lobbyI18n.actions.discarding
          ) : confirmingDiscard ? (
            lobbyI18n.actions.confirmShort
          ) : (
            discardLabel
          )}
        </Button>
      ) : isWaiting ? (
        <Button
          onClick={handleLeaveClick}
          disabled={busy}
          variant={confirmingLeave ? "destructive" : "outline"}
          className="w-full h-12 rounded-xl font-bold cursor-pointer transition-colors"
        >
          {isLeaving ? <Loader2 className="w-5 h-5 animate-spin" /> : <LogOut className="w-5 h-5" />}{" "}
          {isLeaving ? (
            lobbyI18n.actions.leaving
          ) : confirmingLeave ? (
            lobbyI18n.actions.confirmShort
          ) : (
            lobbyI18n.actions.leave
          )}
        </Button>
      ) : (
        onExitHome && (
          <Button
            onClick={onExitHome}
            disabled={busy}
            variant="outline"
            className="w-full h-12 rounded-xl font-bold cursor-pointer transition-colors"
          >
            <Home className="w-5 h-5" /> {lobbyI18n.actions.exitHome}
          </Button>
        )
      )}

      {/* Pausing Button when disconnecting */}
      {isDisconnecting && onPause && (
        <Button
          onClick={onPause}
          disabled={busy}
          variant="outline"
          className="w-full h-12 rounded-xl font-bold border-amber-500/50 bg-amber-950/30 text-amber-300 hover:text-amber-200 hover:bg-amber-900/40 cursor-pointer"
        >
          {isPausing ? <Loader2 className="w-5 h-5 animate-spin" /> : <Pause className="w-5 h-5" />}{" "}
          {isPausing ? lobbyI18n.actions.pausing : lobbyI18n.actions.pauseGame}
        </Button>
      )}
    </div>
  );
}
