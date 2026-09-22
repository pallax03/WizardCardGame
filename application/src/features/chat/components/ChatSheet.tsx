"use client";

import { useEffect, useMemo, useState } from "react";
import { MessageCircle } from "lucide-react";
import { AnimatePresence, MotionConfig, motion } from "motion/react";

import { usePathname } from "next/navigation";
import { useChat } from "../hooks/useChat";
import { ChatMessage } from "../types";
import { useLobbySession } from "@/features/lobby-session";
import { isBotPlayer } from "@/features/lobby-session/presence";
import { t } from "@/ui/i18n/core";
const chatI18n = t("chat");
import { Badge } from "@/ui/components/badge";
import { Sheet, SheetContent, SheetTrigger } from "@/ui/components/sheet";
import { Skeleton } from "@/ui/components/skeleton";

import { ChatHeader } from "./ChatHeader";
import { ChatMessageList } from "./ChatMessageList";
import { ChatInput } from "./ChatInput";

export function ChatSheet() {
  const pathname = usePathname();
  const isGameRoute = pathname?.endsWith("/game");
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
  const botIds = useMemo(
    () =>
      new Set(
        (lobby?.players ?? [])
          .filter((player) => isBotPlayer(player))
          .map((player) => player.id),
      ),
    [lobby?.players],
  );
  const feedMessages = useMemo(
    () =>
      messages.filter(
        (message) =>
          message.type !== "event" &&
          (message.type !== "system" ||
            (!botIds.has(message.playerId) && message.playerId !== playerId)),
      ),
    [messages, botIds, playerId],
  );
  const humanConnectedPlayerIds = useMemo(
    () => connectedPlayerIds.filter((id) => !botIds.has(id)),
    [connectedPlayerIds, botIds],
  );

  const chatMessages = useMemo(
    () => feedMessages.filter((message): message is ChatMessage => message.type === "message"),
    [feedMessages],
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
      return feedMessages.filter((message) => message.type !== "message" || message.destinationId === undefined);
    }
    return chatMessages.filter((message) => message.destinationId !== undefined &&
      (message.playerId === activePrivateId || message.destinationId === activePrivateId));
  }, [activePrivateId, chatMessages, feedMessages]);

  const unreadTotal = isOpen ? 0 : Math.max(0, chatMessages.length - seenMessageCount);
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
    setSeenMessageCount(chatMessages.length);
    if (open && activePrivateId !== null) markPrivateSeen(activePrivateId);
  };

  useEffect(() => {
    const handler = () => {
      setIsOpen(true);
      setActivePrivateId(null);
    };
    window.addEventListener('open-chat-global', handler);
    return () => window.removeEventListener('open-chat-global', handler);
  }, []);

  useEffect(() => {
    const handler = (e: Event) => {
      const customEvent = e as CustomEvent<{ playerId: number }>;
      setIsOpen(true);
      setActivePrivateId(customEvent.detail.playerId);
    };
    window.addEventListener('open-private-chat', handler);
    return () => window.removeEventListener('open-private-chat', handler);
  }, []);
  useEffect(() => {
    if (isOpen && activePrivateId !== null) {
      setTimeout(() => markPrivateSeen(activePrivateId), 0);
    }
  }, [isOpen, activePrivateId, chatMessages.length]);

  if (playerId === null) {
    return <Skeleton className="fixed right-4 bottom-4 z-[90] size-14 rounded-full sm:right-6 sm:bottom-6" />;
  }

  const privateName = activePrivateId === null ? null : playersMap[activePrivateId];

  return (
    <MotionConfig reducedMotion="user" transition={{ type: "spring", stiffness: 420, damping: 34 }}>
    <Sheet open={isOpen} onOpenChange={handleOpenChange}>
      {/* Chat is opened via header button (open-chat-global event), no floating FAB */}

      <SheetContent side="bottom" showCloseButton={false} className="inset-x-0 bottom-0 h-[min(88dvh,46rem)]! max-h-[calc(100dvh-env(safe-area-inset-top))] w-full origin-bottom overflow-hidden rounded-t-[1.75rem] border-white/10 bg-zinc-950/98 p-0 text-white shadow-2xl sm:right-6! sm:left-auto! sm:bottom-24 ssm:h-[min(72dvh,40rem)] sm:max-h-[calc(100dvh-7rem)] sm:w-104 sm:origin-bottom-right sm:rounded-[1.75rem] sm:border sm:data-ending-style:translate-x-8 sm:data-ending-style:translate-y-0 sm:data-starting-style:translate-x-8 sm:data-starting-style:translate-y-0">
          <ChatHeader
            activePrivateId={activePrivateId}
            privateName={privateName}
            connectionState={connectionState}
            connectedPlayerIds={humanConnectedPlayerIds}
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
            botIds={botIds}
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
