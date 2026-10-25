"use client";

import { useState } from "react";
import { Button } from "@/ui/components/button";
import { Copy, Check, Share, Settings } from "lucide-react";
import { LobbyHeaderProps } from "../types";
import { t } from "@/ui/i18n/core";
const lobbyI18n = t("lobby");

interface LobbyHeaderPropsExtended extends LobbyHeaderProps {
  children?: React.ReactNode;
  hideShare?: boolean;
}

export function LobbyHeader({ lobbyCode, children, hideShare }: LobbyHeaderPropsExtended) {
  const [copied, setCopied] = useState(false);
  const [showConfig, setShowConfig] = useState(false);

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
    <div className="flex flex-col gap-3">
      <div className="flex flex-row items-center justify-between">
        <div className="flex items-center gap-2 sm:gap-3">
          {children && (
            <Button
              variant="ghost"
              size="icon"
              onClick={() => setShowConfig(!showConfig)}
              className="h-8 w-8 text-zinc-400 hover:text-white"
            >
              <Settings className="w-4 h-4" />
            </Button>
          )}
          <h2 className="text-2xl sm:text-3xl font-extrabold tracking-tighter text-transparent bg-clip-text bg-linear-to-br from-white via-zinc-200 to-zinc-400 drop-shadow-sm">
            {lobbyI18n.header.title}
          </h2>
        </div>
        {!hideShare && (
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              className="gap-2 border-zinc-700 bg-transparent hover:bg-zinc-800 text-zinc-200 h-9"
              onClick={handleCopy}
              title={lobbyI18n.header.copyCodeButton}
            >
              {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4 text-zinc-400" />}
              <span className="text-xs hidden sm:inline">{lobbyI18n.header.copyCodeButton}</span>
            </Button>
            <Button
              variant="default"
              size="icon"
              className="bg-transparent border border-zinc-700 hover:bg-zinc-800 text-zinc-200 w-9 h-9"
              onClick={handleShare}
              title={lobbyI18n.header.shareTooltip}
            >
              <Share className="w-4 h-4 text-zinc-400" />
            </Button>
          </div>
        )}
      </div>
      {children && showConfig && (
        <div className="animate-in fade-in slide-in-from-top-2">
          {children}
        </div>
      )}
    </div>
  );
}