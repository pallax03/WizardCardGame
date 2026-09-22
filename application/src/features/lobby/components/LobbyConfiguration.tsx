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
    <div className="flex flex-col gap-4 pt-3 border-t border-zinc-800/60 mt-1">
      
      {/* Timer Section */}
      <div className="flex flex-col gap-1.5">
        <span className="text-[10px] font-bold uppercase tracking-widest text-zinc-500">
          {lobbyI18n.config.timer}
        </span>
        <div className="flex gap-1.5 overflow-x-auto hide-scrollbar pb-1">
          {TIMER_PRESETS.map((preset) => (
            <Button
              key={preset}
              type="button"
              variant={preset === timer ? "default" : "ghost"}
              disabled={disabled}
              onClick={() => onSave(preset, maxStrikes)}
              className={`h-7 px-3.5 text-xs rounded-full font-bold shrink-0 transition-colors ${preset === timer ? 'bg-zinc-200 text-black hover:bg-white' : 'bg-zinc-900/50 text-zinc-400 border border-zinc-700/50 hover:text-zinc-200 hover:bg-zinc-800/80'}`}
            >
              {preset}s
            </Button>
          ))}
        </div>
      </div>
      
      {/* Strikes Section */}
      <div className="flex flex-col gap-1.5">
        <div className="flex justify-between items-center">
          <span className="text-[10px] font-bold uppercase tracking-widest text-zinc-500">
            {lobbyI18n.config.maxStrikes}
          </span>
          {isSaving && <Loader2 className="w-3 h-3 animate-spin text-zinc-500" />}
        </div>
        <div className="flex gap-1.5 overflow-x-auto hide-scrollbar pb-1">
          {STRIKES_PRESETS.map((preset) => (
            <Button
              key={preset}
              type="button"
              variant={preset === maxStrikes ? "default" : "ghost"}
              disabled={disabled}
              onClick={() => onSave(timer, preset)}
              className={`h-7 px-3.5 text-xs rounded-full font-bold shrink-0 transition-colors ${preset === maxStrikes ? 'bg-zinc-200 text-black hover:bg-white' : 'bg-zinc-900/50 text-zinc-400 border border-zinc-700/50 hover:text-zinc-200 hover:bg-zinc-800/80'}`}
            >
              {preset === 1 ? "Nessuno" : preset === 2 ? "Default" : "Max"}
            </Button>
          ))}
        </div>
        <span className="text-[9px] text-zinc-500 leading-tight">
          Max auto-play if you don't take any action before time runs out.
        </span>
      </div>
      
    </div>
  );
}
