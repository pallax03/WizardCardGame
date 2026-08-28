"use client";

import { useEffect, useMemo, useState } from "react";
import { MessageCircle } from "lucide-react";
import { AnimatePresence, MotionConfig, motion } from "motion/react";

import { useChat } from "../hooks/useChat";
import { ChatMessage } from "../types";
import { useLobbySession } from "@/features/lobby-session";
import { chatI18n } from "@/i18n/chat";
import { Badge } from "@/ui/components/badge";
import { Sheet, SheetContent, SheetTrigger } from "@/ui/components/sheet";
import { Skeleton } from "@/ui/components/skeleton";

import { ChatHeader } from "./ChatHeader";
import { ChatMessageList } from "./ChatMessageList";
import { ChatInput } from "./ChatInput";

export function ChatSheet() {
  const { playerId, lobby, connectedPlayerIds } = useLobbySession();
  const [isOpen, setIsOpen] = useState(false);
  const [activePrivateId, setActivePrivateId] = useState<number | null>(null);
  const [seenMessageCount, setSeenMessageCount] = useState(0);
  const [seenPrivateCount, setSeenPrivateCount] = useState<Record<number, number>>({});
  const { messages, sendMessage, connectionState } = useChat();
  const playersMap = useMemo(
    () => Object.fromEntries((lobby?.players ?? []).map((player) => [player.id, player.name])),
    [lobby?.players],
  );

  const chatMessages = useMemo(
    () => messages.filter((message): message is ChatMessage => message.type === "message"),
    [messages],
  );
  const privatePeers = useMemo(() => {
    if (playerId === null) return [];
    return Array.from(new Set(chatMessages
      .filter((message) => message.destinationId !== undefined)
      .map((message) => message.playerId === playerId ? message.destinationId : message.playerId)
      .filter((id): id is number => id !== undefined && id !== playerId)));
  }, [chatMessages, playerId]);
  const visibleMessages = useMemo(() => {
    if (activePrivateId === null) {
      return messages.filter((message) => message.type !== "message" || message.destinationId === undefined);
    }
    return chatMessages.filter((message) => message.destinationId !== undefined &&
      (message.playerId === activePrivateId || message.destinationId === activePrivateId));
  }, [activePrivateId, chatMessages, messages]);

  const unreadTotal = isOpen ? 0 : Math.max(0, messages.length - seenMessageCount);
  const privateUnread = (peerId: number) => {
    if (playerId === null) return 0;
    if (isOpen && activePrivateId === peerId) return 0;
    const received = chatMessages.filter((message) => message.playerId === peerId && message.destinationId === playerId).length;
    return Math.max(0, received - (seenPrivateCount[peerId] ?? 0));
  };
  const markPrivateSeen = (peerId: number) => {
    if (playerId === null) return;
    const received = chatMessages.filter((message) => message.playerId === peerId && message.destinationId === playerId).length;
    setSeenPrivateCount((current) => ({ ...current, [peerId]: received }));
  };
  const openPrivateChat = (peerId: number) => {
    setActivePrivateId(peerId);
    markPrivateSeen(peerId);
  };
  const handleOpenChange = (open: boolean) => {
    setIsOpen(open);
    setSeenMessageCount(messages.length);
    if (open && activePrivateId !== null) markPrivateSeen(activePrivateId);
  };

  useEffect(() => {
    const handler = (e: Event) => {
      const customEvent = e as CustomEvent<{ playerId: number }>;
      setIsOpen(true);
      setActivePrivateId(customEvent.detail.playerId);
      // We don't call markPrivateSeen here because it might be stale, 
      // but setActivePrivateId and setIsOpen are guaranteed to be stable.
    };
    window.addEventListener('open-private-chat', handler);
    return () => window.removeEventListener('open-private-chat', handler);
  }, []);
  useEffect(() => {
    if (isOpen && activePrivateId !== null) {
      markPrivateSeen(activePrivateId);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen, activePrivateId, messages.length]);

  if (playerId === null) {
    return <Skeleton className="fixed right-4 bottom-4 z-40 size-14 rounded-full sm:right-6 sm:bottom-6" />;
  }

  const privateName = activePrivateId === null ? null : playersMap[activePrivateId];
  const placeholder = activePrivateId === null ? chatI18n.placeholder : chatI18n.privatePlaceholder;

  return (
    <MotionConfig reducedMotion="user" transition={{ type: "spring", stiffness: 420, damping: 34 }}>
    <Sheet open={isOpen} onOpenChange={handleOpenChange}>
      <SheetTrigger
        render={<motion.button whileHover={{ scale: 1.06 }} whileTap={{ scale: 0.92 }} />}
        aria-label={chatI18n.open}
        className="fixed right-4 bottom-[max(1rem,env(safe-area-inset-bottom))] z-40 grid size-14 place-items-center rounded-full bg-indigo-500 text-white shadow-xl shadow-indigo-950/40 transition-colors hover:bg-indigo-400 sm:right-6 sm:bottom-6"
      >
        <MessageCircle className="size-6" />
        <AnimatePresence>
          {unreadTotal > 0 && <Badge render={<motion.span initial={{ opacity: 0, scale: 0.5 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.5 }} />} className="absolute -top-1 -right-1 min-w-5 border-2 border-zinc-950 bg-rose-500 px-1 text-white">{unreadTotal > 99 ? "99+" : unreadTotal}</Badge>}
        </AnimatePresence>
      </SheetTrigger>

      <SheetContent side="bottom" showCloseButton={false} className="inset-x-0 bottom-0 h-[min(88dvh,46rem)]! max-h-[calc(100dvh-env(safe-area-inset-top))] w-full origin-bottom overflow-hidden rounded-t-[1.75rem] border-white/10 bg-zinc-950/98 p-0 text-white shadow-2xl sm:right-6! sm:left-auto! sm:bottom-24 ssm:h-[min(72dvh,40rem)] sm:max-h-[calc(100dvh-7rem)] sm:w-104 sm:origin-bottom-right sm:rounded-[1.75rem] sm:border sm:data-ending-style:translate-x-8 sm:data-ending-style:translate-y-0 sm:data-starting-style:translate-x-8 sm:data-starting-style:translate-y-0">
          <ChatHeader
            activePrivateId={activePrivateId}
            privateName={privateName}
            connectionState={connectionState}
            connectedPlayerIds={connectedPlayerIds}
            privatePeers={privatePeers}
            playersMap={playersMap}
            privateUnread={privateUnread}
            onClosePrivate={() => { markPrivateSeen(activePrivateId as number); setActivePrivateId(null); }}
            onOpenPrivate={openPrivateChat}
          />

          <ChatMessageList
            messages={visibleMessages}
            playerId={playerId}
            activePrivateId={activePrivateId}
            playersMap={playersMap}
          />

          <ChatInput
            activePrivateId={activePrivateId}
            connectionState={connectionState}
            onSendMessage={(text) => sendMessage(text, activePrivateId ?? undefined)}
          />
      </SheetContent>
    </Sheet>
    </MotionConfig>
  );
}
