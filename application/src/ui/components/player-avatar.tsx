"use client";

import { Avatar, AvatarFallback } from "@/ui/components/avatar";
import { Bot as BotIcon, LockKeyhole } from "lucide-react";
import { Popover, PopoverClose, PopoverContent, PopoverDescription, PopoverHeader, PopoverTitle, PopoverTrigger } from "@/ui/components/popover";
import { Button } from "@/ui/components/button";
import { t } from "@/ui/i18n/core";
const chatI18n = t("chat");

export interface PlayerAvatarProps {
  playerId: number;
  name: string;
  isBot?: boolean;
  isMe?: boolean;
  isOnline?: boolean;
  showDot?: boolean;
  className?: string;
}

export function PlayerAvatar({
  playerId,
  name,
  isBot = false,
  isMe = false,
  isOnline = false,
  showDot = false,
  className = "h-10 w-10"
}: PlayerAvatarProps) {
  const fallbackText = `P${playerId}`;
  
  const openPrivateChat = () => {
    window.dispatchEvent(new CustomEvent('open-private-chat', { detail: { playerId } }));
  };

  const avatar = (
    <div className="relative inline-block shrink-0">
      <Avatar className={`${className} ${isBot ? "border border-cyan-500/50" : ""} ${isMe ? "ring-2 ring-indigo-400/60" : "ring-2 ring-transparent transition hover:ring-indigo-400/60"}`}>
        <AvatarFallback className={isBot ? "bg-cyan-950 text-cyan-400" : isMe ? "bg-indigo-500/15 text-indigo-200" : "bg-zinc-800 text-zinc-300 font-semibold"}>
          {isBot ? <BotIcon className="w-1/2 h-1/2" /> : <span className="text-[10px]">{fallbackText}</span>}
        </AvatarFallback>
      </Avatar>
      {showDot && (
        <span
          className={`absolute -bottom-0.5 -right-0.5 z-10 block h-3 w-3 rounded-full ring-2 ring-zinc-900 ${
            isOnline ? "bg-emerald-500" : "bg-slate-500"
          }`}
        />
      )}
    </div>
  );

  if (isMe || isBot) {
    return avatar;
  }

  return (
    <Popover>
      <PopoverTrigger aria-label={chatI18n.actionsFor(name)} className="shrink-0 rounded-full outline-none focus-visible:ring-2 focus-visible:ring-indigo-400">
        {avatar}
      </PopoverTrigger>
      <PopoverContent side="top" align="start" className="w-64 gap-3 rounded-2xl border border-white/8 bg-zinc-900 p-3 text-white">
        <PopoverHeader>
          <PopoverTitle className="text-sm">{name}</PopoverTitle>
          <PopoverDescription className="text-xs text-zinc-400">{chatI18n.privateDescription}</PopoverDescription>
        </PopoverHeader>
        <PopoverClose render={<Button className="w-full bg-indigo-500 text-white hover:bg-indigo-400" onClick={openPrivateChat} />}>
          <LockKeyhole className="w-4 h-4 mr-2" /> {chatI18n.startPrivate}
        </PopoverClose>
      </PopoverContent>
    </Popover>
  );
}
