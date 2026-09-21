"use client";

import { Button } from "@/ui/components/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/ui/components/card";
import { Settings2, Loader2 } from "lucide-react";
import { t } from "@/ui/i18n/core";

const lobbyI18n = t("lobby");

const TIMER_PRESETS = [15, 20, 30, 45, 60, 90];
const STRIKES_PRESETS = [1, 2, 3];

interface LobbyConfigurationProps {
  timer: number;
  maxStrikes: number;
  canEdit: boolean;
  isSaving: boolean;
  onSave: (timer: number, maxStrikes: number) => void;
}

export function LobbyConfiguration({ timer, maxStrikes, canEdit, isSaving, onSave }: LobbyConfigurationProps) {
  const disabled = !canEdit || isSaving;

  return (
    <Card className="surface-card text-zinc-100">
      <CardHeader className="pb-3">
        <CardTitle className="text-sm font-semibold text-white flex items-center gap-2">
          <Settings2 className="w-4 h-4 text-zinc-400" />
          {lobbyI18n.config.title}
          {isSaving && <Loader2 className="w-3.5 h-3.5 animate-spin text-zinc-400" />}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="space-y-1.5">
          <p className="text-xs font-medium text-zinc-400">{lobbyI18n.config.timer}</p>
          <div className="flex flex-wrap gap-1.5">
            {TIMER_PRESETS.map((preset) => (
              <Button
                key={preset}
                type="button"
                size="sm"
                variant={preset === timer ? "default" : "outline"}
                disabled={disabled}
                onClick={() => onSave(preset, maxStrikes)}
                className={
                  preset === timer
                    ? "h-7 px-2.5 text-xs font-bold"
                    : "h-7 px-2.5 text-xs border-zinc-700 bg-transparent text-zinc-300 hover:text-white hover:border-zinc-500"
                }
              >
                {preset}s
              </Button>
            ))}
          </div>
        </div>
        <div className="space-y-1.5">
          <p className="text-xs font-medium text-zinc-400">{lobbyI18n.config.maxStrikes}</p>
          <div className="flex flex-wrap gap-1.5">
            {STRIKES_PRESETS.map((preset) => (
              <Button
                key={preset}
                type="button"
                size="sm"
                variant={preset === maxStrikes ? "default" : "outline"}
                disabled={disabled}
                onClick={() => onSave(timer, preset)}
                className={
                  preset === maxStrikes
                    ? "h-7 px-2.5 text-xs font-bold"
                    : "h-7 px-2.5 text-xs border-zinc-700 bg-transparent text-zinc-300 hover:text-white hover:border-zinc-500"
                }
              >
                {preset}
              </Button>
            ))}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
