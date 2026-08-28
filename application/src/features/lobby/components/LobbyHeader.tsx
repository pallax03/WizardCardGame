"use client";

import { useState } from "react";
import { Card, CardHeader, CardTitle } from "@/ui/components/card";
import { Button } from "@/ui/components/button";
import { Copy, Check } from "lucide-react";
import { LobbyHeaderProps } from "../types";
import { lobbyI18n } from "@/ui/i18n/lobby";

export function LobbyHeader({ roomCode }: LobbyHeaderProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    if (!roomCode) return;
    navigator.clipboard.writeText(roomCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <Card className="bg-slate-900 border-slate-800 text-slate-100">
      <CardHeader className="flex flex-row items-center justify-between py-4">
        <div>
          <CardTitle className="text-lg sm:text-xl font-bold text-white">
            Stanza #{roomCode.slice(0, 8)}
          </CardTitle>
        </div>
        <Button
          variant="outline"
          size="sm"
          className="gap-2 border-slate-700 bg-slate-800/50 hover:bg-slate-800 text-slate-200"
          onClick={handleCopy}
        >
          {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4 text-slate-400" />}
          <span className="text-xs">{lobbyI18n.header.copyCodeButton}</span>
        </Button>
      </CardHeader>
    </Card>
  );
}