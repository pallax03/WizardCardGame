"use client";

import { useState } from "react";
import { Card, CardHeader, CardTitle } from "@/ui/components/card";
import { Button } from "@/ui/components/button";
import { Copy, Check, Share } from "lucide-react";
import { LobbyHeaderProps } from "../types";
import { t } from "@/ui/i18n/core";
const lobbyI18n = t("lobby");

export function LobbyHeader({ lobbyCode }: LobbyHeaderProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    if (!lobbyCode) return;
    navigator.clipboard.writeText(lobbyCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleShare = async () => {
    if (!lobbyCode) return;
    const shareUrl = `${window.location.origin}/?lobbyId=${lobbyCode}`;
    if (navigator.share) {
      try {
        await navigator.share({
          title: "Join my Wizard lobby!",
          url: shareUrl,
        });
      } catch (err) {
        console.error("Error sharing:", err);
      }
    } else {
      navigator.clipboard.writeText(shareUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <Card className="surface-card text-zinc-100">
      <CardHeader className="flex flex-row items-center justify-between py-4">
        <div>
          <CardTitle className="text-2xl sm:text-3xl font-extrabold tracking-tighter text-transparent bg-clip-text bg-linear-to-br from-indigo-200 via-purple-300 to-pink-300 drop-shadow-sm">
            {lobbyI18n.header.title}
          </CardTitle>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            className="gap-2 border-zinc-700 bg-transparent hover:bg-zinc-800 text-zinc-200 h-9"
            onClick={handleCopy}
            title={lobbyI18n.header.copyCodeButton}
          >
            {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4 text-zinc-400" />}
            <span className="text-xs">{lobbyI18n.header.copyCodeButton}</span>
          </Button>
          <Button
            variant="default"
            size="icon"
            className=" bg-transparent hover:bg-zinc-800 text-zinc-200 w-9 h-9"
            onClick={handleShare}
            title={lobbyI18n.header.shareTooltip}
          >
            <Share className="w-4 h-4 text-zinc-400" />
          </Button>
        </div>
      </CardHeader>
    </Card>
  );
}