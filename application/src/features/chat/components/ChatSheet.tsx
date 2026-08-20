"use client";

import { FormEvent, useEffect, useMemo, useRef, useState } from "react";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { ArrowLeft, Check, LockKeyhole, MessageCircle, Send, Users, X } from "lucide-react";

import { useChat } from "../hooks/useChat";
import { ChatMessage } from "../types";
import { getLobbyState } from "@/features/lobby/hooks/useLobby";
import { chatI18n } from "@/i18n/chat";
import { Avatar, AvatarFallback } from "@/ui/components/avatar";
import { Badge } from "@/ui/components/badge";
import { Bubble, BubbleContent, BubbleGroup } from "@/ui/components/bubble";
import { Button } from "@/ui/components/button";
import { Input } from "@/ui/components/input";
import { Popover, PopoverClose, PopoverContent, PopoverDescription, PopoverHeader, PopoverTitle, PopoverTrigger } from "@/ui/components/popover";
import { ScrollArea } from "@/ui/components/scroll-area";
import { Sheet, SheetClose, SheetContent, SheetDescription, SheetHeader, SheetTitle, SheetTrigger } from "@/ui/components/sheet";
import { Skeleton } from "@/ui/components/skeleton";

function initials(name: string) {
  return name.split(/\s+/).slice(0, 2).map((part) => part[0]).join("").toUpperCase();
}

export function ChatSheet() {
  const params = useParams();
  const searchParams = useSearchParams();
  const router = useRouter();
  const lobbyId = params.id as string;
  const [playerId, setPlayerId] = useState<number | null>(null);
  const [playersMap, setPlayersMap] = useState<Record<number, string>>({});
  const [input, setInput] = useState("");
  const [isOpen, setIsOpen] = useState(false);
  const [activePrivateId, setActivePrivateId] = useState<number | null>(null);
  const [seenMessageCount, setSeenMessageCount] = useState(0);
  const [seenPrivateCount, setSeenPrivateCount] = useState<Record<number, number>>({});
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const { messages, sendMessage, connectionState } = useChat(lobbyId, playerId);

  useEffect(() => {
    const urlPlayerId = searchParams.get("playerId");
    const storedPlayerId = localStorage.getItem("wizard_playerId");
    const storedLobbyId = localStorage.getItem("wizard_lobbyId");
    const candidate = urlPlayerId ?? (storedLobbyId === lobbyId ? storedPlayerId : null);
    const resolvedPlayerId = candidate === null ? Number.NaN : Number.parseInt(candidate, 10);
    if (Number.isNaN(resolvedPlayerId)) {
      router.replace("/");
      return;
    }
    if (urlPlayerId) {
      localStorage.setItem("wizard_playerId", urlPlayerId);
      localStorage.setItem("wizard_lobbyId", lobbyId);
      router.replace(`/lobby/${lobbyId}`);
    }
    getLobbyState(lobbyId)
      .then((data) => {
        if (!data?.players) {
          router.replace("/");
          return;
        }
        setPlayerId(resolvedPlayerId);
        setPlayersMap(Object.fromEntries(data.players.map((player: { id: number; name: string }) => [player.id, player.name])));
      })
      .catch(() => router.replace("/"));
  }, [lobbyId, router, searchParams]);

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
    setInput("");
  };
  const handleOpenChange = (open: boolean) => {
    setIsOpen(open);
    setSeenMessageCount(messages.length);
    if (open && activePrivateId !== null) markPrivateSeen(activePrivateId);
  };
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [visibleMessages.length, activePrivateId]);

  const handleSend = (event: FormEvent) => {
    event.preventDefault();
    const text = input.trim();
    if (!text) return;
    sendMessage(text, activePrivateId ?? undefined);
    setInput("");
  };

  if (playerId === null) {
    return <Skeleton className="fixed right-4 bottom-4 z-40 size-14 rounded-full sm:right-6 sm:bottom-6" />;
  }

  const privateName = activePrivateId === null ? null : playersMap[activePrivateId];
  const placeholder = activePrivateId === null ? chatI18n.placeholder : chatI18n.privatePlaceholder;

  return (
    <Sheet open={isOpen} onOpenChange={handleOpenChange}>
      <SheetTrigger aria-label="Apri la chat" className="fixed right-4 bottom-[max(1rem,env(safe-area-inset-bottom))] z-40 grid size-14 place-items-center rounded-full bg-indigo-500 text-white shadow-xl shadow-indigo-950/40 transition hover:scale-105 hover:bg-indigo-400 active:scale-95 sm:right-6 sm:bottom-6">
        <MessageCircle className="size-6" />
        {unreadTotal > 0 && <Badge className="absolute -top-1 -right-1 min-w-5 border-2 border-zinc-950 bg-rose-500 px-1 text-white">{unreadTotal > 99 ? "99+" : unreadTotal}</Badge>}
      </SheetTrigger>

      <SheetContent side="bottom" showCloseButton={false} className="inset-x-0 bottom-0 h-[min(88dvh,46rem)] w-full overflow-hidden rounded-t-[1.75rem] border-white/10 bg-zinc-950/98 p-0 text-white shadow-2xl sm:inset-x-auto sm:right-6 sm:bottom-24 sm:h-[min(72dvh,40rem)] sm:w-[26rem] sm:rounded-[1.75rem] sm:border">
        <SheetHeader className="gap-0 border-b border-white/8 px-4 py-3">
          <div className="flex min-w-0 items-center gap-2">
            {activePrivateId !== null ? (
              <Button type="button" variant="ghost" size="icon" aria-label={chatI18n.backToLobby} className="rounded-full text-zinc-400 hover:bg-white/8 hover:text-white" onClick={() => { markPrivateSeen(activePrivateId); setActivePrivateId(null); setInput(""); }}>
                <ArrowLeft />
              </Button>
            ) : (
              <span className="grid size-8 shrink-0 place-items-center rounded-full bg-indigo-500/15 text-indigo-300"><Users className="size-4" /></span>
            )}
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <SheetTitle className="truncate text-sm font-semibold text-white">{activePrivateId === null ? chatI18n.title : privateName ?? `${chatI18n.fallbackPlayer} ${activePrivateId}`}</SheetTitle>
                {activePrivateId !== null && <Badge variant="secondary" className="h-4 gap-1 bg-violet-500/15 px-1.5 text-[10px] text-violet-300"><LockKeyhole className="size-2.5!" /> privata</Badge>}
              </div>
              <SheetDescription className="flex items-center gap-1.5 truncate text-[11px] text-zinc-500">
                <span className={`size-1.5 rounded-full ${connectionState === "open" ? "bg-emerald-400" : "bg-amber-400"}`} />
                {activePrivateId === null ? `${Object.keys(playersMap).length} giocatori · ${connectionState === "open" ? chatI18n.connected : chatI18n.connecting}` : `${chatI18n.privateWith} ${privateName ?? chatI18n.fallbackPlayer}`}
              </SheetDescription>
            </div>
            <SheetClose aria-label="Chiudi la chat" className="grid size-8 shrink-0 place-items-center rounded-full text-zinc-500 transition hover:bg-white/8 hover:text-white focus-visible:ring-2 focus-visible:ring-indigo-400 focus-visible:outline-none"><X className="size-4" /></SheetClose>
          </div>

          {activePrivateId === null && privatePeers.length > 0 && (
            <div className="mt-3 flex items-center gap-2 overflow-x-auto pb-0.5">
              <span className="shrink-0 text-[10px] font-medium tracking-wider text-zinc-600 uppercase">{chatI18n.privateMessages}</span>
              {privatePeers.map((peerId) => {
                const name = playersMap[peerId] ?? `${chatI18n.fallbackPlayer} ${peerId}`;
                const unread = privateUnread(peerId);
                return <Button key={peerId} type="button" variant="secondary" size="xs" className="relative shrink-0 rounded-full bg-white/6 text-zinc-300 hover:bg-white/10 hover:text-white" onClick={() => openPrivateChat(peerId)}><LockKeyhole className="size-3" />{name}{unread > 0 && <Badge className="h-4 min-w-4 bg-rose-500 px-1 text-[9px] text-white">{unread}</Badge>}</Button>;
              })}
            </div>
          )}
        </SheetHeader>

        <ScrollArea className="min-h-0 flex-1 bg-[radial-gradient(circle_at_top,rgba(99,102,241,0.07),transparent_38%)]">
          <BubbleGroup className="min-h-full gap-3 px-3 py-5 sm:px-4">
            {visibleMessages.length === 0 && <div className="m-auto flex max-w-56 flex-col items-center gap-3 py-16 text-center"><span className="grid size-11 place-items-center rounded-full bg-white/5 text-zinc-500">{activePrivateId === null ? <MessageCircle /> : <LockKeyhole />}</span><p className="text-sm text-zinc-500">{activePrivateId === null ? chatI18n.emptyLobby : chatI18n.emptyPrivate}</p></div>}
            {visibleMessages.map((message, index) => {
              if (message.type === "system") {
                const name = playersMap[message.playerId] ?? `${chatI18n.fallbackPlayer} ${message.playerId}`;
                return <div key={`${message.timestamp}-${index}`} className="flex justify-center py-1"><Badge variant="secondary" className="bg-white/5 text-[10px] font-normal text-zinc-500">{name} {message.action === "joined" ? chatI18n.joined : chatI18n.left}</Badge></div>;
              }
              if (message.type === "event") {
                return <div key={`${message.timestamp}-${index}`} className="flex justify-center py-1"><Badge variant="outline" className="border-indigo-400/10 bg-indigo-400/5 text-[10px] font-normal text-indigo-300/70">{message.event.action}{message.event.playerId !== undefined ? ` · P${message.event.playerId}` : ""}</Badge></div>;
              }
              const isMe = message.playerId === playerId;
              const name = playersMap[message.playerId] ?? `${chatI18n.fallbackPlayer} ${message.playerId}`;
              return (
                <div key={`${message.timestamp}-${message.playerId}-${index}`} className={`flex w-full items-end gap-2 ${isMe ? "justify-end pl-10" : "justify-start pr-6"}`}>
                  {!isMe && (
                    <Popover>
                      <PopoverTrigger aria-label={`Azioni per ${name}`} className="shrink-0 rounded-full outline-none focus-visible:ring-2 focus-visible:ring-indigo-400">
                        <Avatar className="cursor-pointer ring-2 ring-transparent transition hover:ring-indigo-400/60"><AvatarFallback className="bg-zinc-800 text-[10px] font-semibold text-zinc-300">{initials(name)}</AvatarFallback></Avatar>
                      </PopoverTrigger>
                      <PopoverContent side="top" align="start" className="w-64 gap-3 rounded-2xl border border-white/8 bg-zinc-900 p-3 text-white">
                        <PopoverHeader><PopoverTitle className="text-sm">{name}</PopoverTitle><PopoverDescription className="text-xs text-zinc-400">I messaggi privati restano separati dalla lobby.</PopoverDescription></PopoverHeader>
                        <PopoverClose render={<Button className="w-full bg-indigo-500 text-white hover:bg-indigo-400" />} onClick={() => openPrivateChat(message.playerId)}><LockKeyhole /> {chatI18n.startPrivate}</PopoverClose>
                      </PopoverContent>
                    </Popover>
                  )}
                  <div className={`flex min-w-0 max-w-full flex-col ${isMe ? "items-end" : "flex-1 items-start"}`}>
                    {!isMe && <span className="mb-1 px-1 text-[10px] font-medium text-zinc-500">{name}</span>}
                    <Bubble align={isMe ? "end" : "start"} variant={isMe ? "default" : "secondary"} className="max-w-full">
                      <BubbleContent className={isMe ? "bg-indigo-500 text-white" : "bg-zinc-800/90 text-zinc-100"}>{message.text}</BubbleContent>
                    </Bubble>
                    {isMe && <span className="mt-1 flex items-center gap-0.5 px-1 text-[9px] text-zinc-600"><Check className="size-2.5" /> inviato</span>}
                  </div>
                </div>
              );
            })}
            <div ref={messagesEndRef} />
          </BubbleGroup>
        </ScrollArea>

        <form onSubmit={handleSend} className="flex shrink-0 items-center gap-2 border-t border-white/8 bg-zinc-950 px-3 pt-3 pb-[max(.75rem,env(safe-area-inset-bottom))]">
          <Input value={input} onChange={(event) => setInput(event.target.value)} placeholder={placeholder} aria-label={placeholder} autoComplete="off" className="h-11 rounded-full border-white/8 bg-white/5 px-4 text-base text-white placeholder:text-zinc-600 focus-visible:border-indigo-400/50 focus-visible:ring-indigo-400/15 sm:text-sm" />
          <Button type="submit" size="icon-lg" disabled={!input.trim() || connectionState !== "open"} aria-label={chatI18n.send} className="size-11 rounded-full bg-indigo-500 text-white shadow-lg shadow-indigo-950/30 hover:bg-indigo-400"><Send className="size-4" /></Button>
        </form>
      </SheetContent>
    </Sheet>
  );
}
