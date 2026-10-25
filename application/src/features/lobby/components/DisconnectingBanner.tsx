"use client";

import { useEffect, useState } from "react";
import { Hourglass } from "lucide-react";
import { t } from "@/ui/i18n/core";

const lobbyI18n = t("lobby");

interface DisconnectingBannerProps {
  timerSeconds: number;
}

export function DisconnectingBanner({ timerSeconds }: DisconnectingBannerProps) {
  const total = Number.isFinite(timerSeconds) && timerSeconds > 0 ? timerSeconds : 30;
  const [secondsLeft, setSecondsLeft] = useState(total);

  useEffect(() => {
    const next = total;
    queueMicrotask(() => setSecondsLeft(next));
  }, [total]);

  useEffect(() => {
    if (secondsLeft <= 0) return;
    const id = setTimeout(() => setSecondsLeft((s) => Math.max(0, s - 1)), 1000);
    return () => clearTimeout(id);
  }, [secondsLeft]);

  return (
    <div className="bg-amber-500/10 border border-amber-500/30 text-amber-300 px-4 py-3 rounded-md text-sm text-center space-y-1">
      <p className="font-semibold flex items-center justify-center gap-2">
        <Hourglass className="w-4 h-4 animate-pulse" />
        {lobbyI18n.disconnectingTimer(secondsLeft)}
      </p>
      <p className="text-xs text-amber-300/80">{lobbyI18n.disconnectingNotice}</p>
      <div className="h-1 w-full overflow-hidden rounded-full bg-amber-500/20">
        <div
          className="h-full rounded-full bg-amber-400 transition-[width] duration-1000 ease-linear"
          style={{ width: `${Math.max(0, Math.min(100, (secondsLeft / total) * 100))}%` }}
        />
      </div>
    </div>
  );
}
