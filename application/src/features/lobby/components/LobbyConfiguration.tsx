"use client";

import { useEffect, useState } from "react";
import { Button } from "@/ui/components/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/ui/components/card";
import { Input } from "@/ui/components/input";
import { Settings2, Loader2 } from "lucide-react";
import { t } from "@/ui/i18n/core";

const lobbyI18n = t("lobby");

interface LobbyConfigurationProps {
  timer: number;
  maxStrikes: number;
  canEdit: boolean;
  isSaving: boolean;
  onSave: (timer: number, maxStrikes: number) => void;
}

export function LobbyConfiguration({ timer, maxStrikes, canEdit, isSaving, onSave }: LobbyConfigurationProps) {
  const [timerInput, setTimerInput] = useState(String(timer));
  const [strikesInput, setStrikesInput] = useState(String(maxStrikes));

  useEffect(() => {
    const nextTimer = String(timer);
    const nextStrikes = String(maxStrikes);
    queueMicrotask(() => {
      setTimerInput(nextTimer);
      setStrikesInput(nextStrikes);
    });
  }, [timer, maxStrikes]);

  const parsedTimer = Number.parseInt(timerInput, 10);
  const parsedStrikes = Number.parseInt(strikesInput, 10);
  const valid =
    Number.isInteger(parsedTimer) && parsedTimer >= 15 && parsedTimer <= 90 &&
    Number.isInteger(parsedStrikes) && parsedStrikes >= 1 && parsedStrikes <= 3;
  const dirty = parsedTimer !== timer || parsedStrikes !== maxStrikes;

  return (
    <Card className="surface-card text-zinc-100">
      <CardHeader className="pb-3">
        <CardTitle className="text-sm font-semibold text-white flex items-center gap-2">
          <Settings2 className="w-4 h-4 text-zinc-400" />
          {lobbyI18n.config.title}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="grid grid-cols-2 gap-3">
          <label className="space-y-1.5">
            <span className="text-xs font-medium text-zinc-400">{lobbyI18n.config.timer}</span>
            <Input
              type="number"
              min={15}
              max={90}
              value={timerInput}
              disabled={!canEdit || isSaving}
              onChange={(e) => setTimerInput(e.target.value)}
              className="bg-zinc-950/60 border-zinc-800 text-zinc-100 h-10"
            />
          </label>
          <label className="space-y-1.5">
            <span className="text-xs font-medium text-zinc-400">{lobbyI18n.config.maxStrikes}</span>
            <Input
              type="number"
              min={1}
              max={3}
              value={strikesInput}
              disabled={!canEdit || isSaving}
              onChange={(e) => setStrikesInput(e.target.value)}
              className="bg-zinc-950/60 border-zinc-800 text-zinc-100 h-10"
            />
          </label>
        </div>
        <p className="text-[11px] text-zinc-500">{lobbyI18n.config.hint}</p>
        {canEdit && (
          <Button
            size="sm"
            disabled={!valid || !dirty || isSaving}
            onClick={() => onSave(parsedTimer, parsedStrikes)}
            className="w-full gap-2"
          >
            {isSaving && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
            {isSaving ? lobbyI18n.config.saving : lobbyI18n.config.save}
          </Button>
        )}
      </CardContent>
    </Card>
  );
}
