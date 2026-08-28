"use client";

import { useEffect, useRef } from "react";
import { Check, LockKeyhole, MessageCircle } from "lucide-react";
import { AnimatePresence, motion } from "motion/react";
import { chatI18n } from "@/i18n/chat";
import { Badge } from "@/ui/components/badge";
import { Bubble, BubbleContent, BubbleGroup } from "@/ui/components/bubble";
import { ScrollArea } from "@/ui/components/scroll-area";
import { ChatMessage, EventMessage, SystemMessage, AnyMessage } from "../types";
import { PlayerAvatar } from "@/ui/components/player-avatar";

interface ChatMessageListProps {
  messages: Array<AnyMessage>;
  playerId: number;
  activePrivateId: number | null;
  playersMap: Record<number, string>;
}

export function ChatMessageList({
  messages,
  playerId,
  activePrivateId,
  playersMap,
}: ChatMessageListProps) {
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages.length, activePrivateId]);

  // "L'idea e' quello di togliere i messaggi di Entrato e abbandonato in lobby per i giocatore corrente."
  const filteredMessages = messages.filter((message) => {
    if (message.type === "system" && message.playerId === playerId) {
      return false;
    }
    return true;
  });

  return (
    <ScrollArea className="min-h-0 flex-1 bg-[radial-gradient(circle_at_top,rgba(99,102,241,0.07),transparent_38%)]">
      <BubbleGroup className="min-h-full gap-3 px-3 py-5 sm:px-4">
        <AnimatePresence mode="popLayout" initial={false}>
          {filteredMessages.length === 0 && (
            <motion.div key={`empty-${activePrivateId ?? "lobby"}`} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }} className="m-auto flex max-w-56 flex-col items-center gap-3 py-16 text-center">
              <span className="grid size-11 place-items-center rounded-full bg-white/5 text-zinc-500">
                {activePrivateId === null ? <MessageCircle /> : <LockKeyhole />}
              </span>
              <p className="text-sm text-zinc-500">{activePrivateId === null ? chatI18n.emptyLobby : chatI18n.emptyPrivate}</p>
            </motion.div>
          )}
          {filteredMessages.map((message, index) => {
            if (message.type === "system") {
              const name = playersMap[message.playerId] ?? `${chatI18n.fallbackPlayer} ${message.playerId}`;
              return (
                <motion.div layout initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }} key={`${message.timestamp}-${index}`} className="flex justify-center py-1">
                  <Badge variant="secondary" className="bg-white/5 text-[10px] font-normal text-zinc-500">
                    {name} {message.action === "joined" || message.action === "online" ? chatI18n.joined : chatI18n.left}
                  </Badge>
                </motion.div>
              );
            }
            if (message.type === "event") {
              return (
                <motion.div layout initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }} key={`${message.timestamp}-${index}`} className="flex justify-center py-1">
                  <Badge variant="outline" className="border-indigo-400/10 bg-indigo-400/5 text-[10px] font-normal text-indigo-300/70">
                    {message.event.action}{message.event.playerId !== undefined ? ` · P${message.event.playerId}` : ""}
                  </Badge>
                </motion.div>
              );
            }
            
            const isMe = message.playerId === playerId;
            const name = playersMap[message.playerId] ?? `${chatI18n.fallbackPlayer} ${message.playerId}`;
            
            return (
              <motion.div layout initial={{ opacity: 0, y: 10, scale: 0.98 }} animate={{ opacity: 1, y: 0, scale: 1 }} exit={{ opacity: 0, scale: 0.98 }} key={`${message.timestamp}-${message.playerId}-${index}`} className={`flex w-full items-end gap-2 ${isMe ? "justify-end pl-10" : "justify-start pr-6"}`}>
                {!isMe && (
                  <PlayerAvatar playerId={message.playerId} name={name} />
                )}
                <div className={`flex min-w-0 max-w-full flex-col ${isMe ? "items-end" : "flex-1 items-start"}`}>
                  <span className="mb-1 px-1 text-[10px] font-medium text-zinc-500">{name}</span>
                  <Bubble align={isMe ? "end" : "start"} variant={isMe ? "default" : "secondary"} className="max-w-full">
                    <BubbleContent className={isMe ? "bg-indigo-500 text-white" : "bg-zinc-800/90 text-zinc-100"}>{message.text}</BubbleContent>
                  </Bubble>
                  {isMe && <span className="mt-1 flex items-center gap-0.5 px-1 text-[9px] text-zinc-600"><Check className="size-2.5" /> {chatI18n.sent}</span>}
                </div>
                {isMe && (
                  <PlayerAvatar playerId={message.playerId} name={name} isMe />
                )}
              </motion.div>
            );
          })}
        </AnimatePresence>
        <div ref={messagesEndRef} />
      </BubbleGroup>
    </ScrollArea>
  );
}
