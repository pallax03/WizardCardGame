"use client";

import { Button } from "@/ui/components/button";
import { lobbyI18n } from "@/ui/i18n/lobby";
import { LogOut, Check, Loader2 } from "lucide-react";
import { LobbyActionsProps } from "../types";

export function LobbyActions({ isLeaving, onLeave, onStart }: LobbyActionsProps) {
  return (
    <div className="flex gap-3">
      <Button
        onClick={onLeave}
        disabled={isLeaving}
        variant="destructive"
        size="lg"
        className="w-1/3 gap-2"
      >
        {isLeaving ? <Loader2 className="w-4 h-4 animate-spin" /> : <LogOut className="w-4 h-4" />} {lobbyI18n.actions.leave}
      </Button>
      <Button
        onClick={onStart}
        size="lg"
        className="w-2/3 bg-emerald-600 hover:bg-emerald-500 text-white font-bold gap-2 shadow-lg shadow-emerald-950/40"
      >
        <Check className="w-5 h-5" /> {lobbyI18n.actions.startGame}
      </Button>
    </div>
  );
}