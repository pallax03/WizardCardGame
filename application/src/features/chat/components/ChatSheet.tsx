"use client";

import { useEffect, useState } from "react";
import { useParams, useSearchParams, useRouter } from "next/navigation";
import { MessageCircle } from "lucide-react";
import { useChat } from "../hooks/useChat";
import { BubbleGroup, Bubble, BubbleContent } from "@/ui/components/bubble";
import { Input } from "@/ui/components/input";
import { Button } from "@/ui/components/button";
import { ScrollArea } from "@/ui/components/scroll-area";
import { Avatar, AvatarFallback } from "@/ui/components/avatar";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/ui/components/sheet";
import { Skeleton } from "@/ui/components/skeleton";

import { chatI18n } from "@/i18n/chat";

import { getLobbyState } from "@/features/lobby/hooks/useLobby";

export function ChatSheet() {
  const params = useParams();
  const searchParams = useSearchParams();
  const router = useRouter();

  const lobbyId = params.id as string;
  const [playerId, setPlayerId] = useState<number | null>(null);
  const [playersMap, setPlayersMap] = useState<Record<number, string>>({});
  
  const { messages, sendMessage } = useChat(lobbyId, playerId);
  const [input, setInput] = useState("");

  useEffect(() => {
    getLobbyState(lobbyId)
      .then(data => {
        if (data && data.players) {
          const map: Record<number, string> = {};
          data.players.forEach((p: { id: number, name: string }) => map[p.id] = p.name);
          setPlayersMap(map);
        }
      })
      .catch(console.error);

    // 2. Check URL for playerId
    const urlPlayerId = searchParams.get("playerId");
    if (urlPlayerId) {
      const parsedId = parseInt(urlPlayerId, 10);
      setTimeout(() => setPlayerId(parsedId), 0);
      localStorage.setItem("wizard_playerId", urlPlayerId);
      localStorage.setItem("wizard_lobbyId", lobbyId);
      // Clean up URL
      router.replace(`/lobby/${lobbyId}`);
    } else {
      // 3. Check localStorage
      const localId = localStorage.getItem("wizard_playerId");
      const localLobby = localStorage.getItem("wizard_lobbyId");
      if (localId && localLobby === lobbyId) {
        setTimeout(() => setPlayerId(parseInt(localId, 10)), 0);
      }
    }
  }, [lobbyId, searchParams, router]);

  const handleSend = (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim()) return;
    sendMessage(input);
    setInput("");
  };

  if (playerId === null || playerId === undefined) {
    return (
      <div className="fixed bottom-6 right-6 flex flex-col items-center gap-2 z-50">
        <Skeleton className="h-14 w-14 rounded-full shadow-lg bg-zinc-800" />
        <span className="text-[10px] text-zinc-500 font-medium tracking-wide bg-zinc-950/80 px-2 py-1 rounded-md">{chatI18n.loading}</span>
      </div>
    );
  }

  return (
    <Sheet>
      <SheetTrigger
        className="fixed bottom-6 right-6 h-14 w-14 rounded-full shadow-lg bg-indigo-600 hover:bg-indigo-500 text-white border-0 flex items-center justify-center cursor-pointer z-50 transition-transform hover:scale-105 active:scale-95"
      >
        <MessageCircle className="h-6 w-6" />
      </SheetTrigger>
      <SheetContent className="w-full sm:max-w-md flex flex-col h-full bg-zinc-950 border-zinc-800 text-white p-0 z-50">
        <SheetHeader className="p-4 border-b border-zinc-800">
          <SheetTitle className="text-white text-xl">{chatI18n.title}</SheetTitle>
        </SheetHeader>
        
        <div className="flex-1 min-h-0 relative">
          <ScrollArea className="h-full px-4 py-4">
            <BubbleGroup className="pb-4">
              {messages.map((msg, idx) => {
                if (msg.type === "system") {
                  const pName = playersMap[msg.playerId] || `${chatI18n.fallbackPlayer} ${msg.playerId}`;
                  const actionText = msg.action === "joined" ? chatI18n.joined : chatI18n.left;
                  return (
                    <div key={idx} className="flex justify-center mb-4">
                      <span className="text-xs text-zinc-500 italic bg-zinc-900/50 px-3 py-1 rounded-full">
                        {pName} {actionText}
                      </span>
                    </div>
                  );
                }
                
                if (msg.type === "event") {
                  return (
                    <div key={idx} className="flex justify-center mb-4">
                      <span className="text-xs text-indigo-500/70 italic bg-indigo-900/20 px-3 py-1 rounded-full border border-indigo-900/30">
                        {msg.event.action} - P{msg.event.playerId}
                      </span>
                    </div>
                  );
                }

                // ChatMessage
                const isMe = msg.playerId === playerId;
                const pName = playersMap[msg.playerId] || `${chatI18n.fallbackPlayer} ${msg.playerId}`;
                const initials = pName.substring(0, 2).toUpperCase();
                
                return (
                  <div key={idx} className={`flex gap-2 mb-4 ${isMe ? "justify-end" : "justify-start"}`}>
                    {!isMe && (
                      <Avatar className="h-8 w-8 mt-auto">
                        <AvatarFallback className="bg-zinc-800 text-xs text-zinc-400" title={pName}>
                          {initials}
                        </AvatarFallback>
                      </Avatar>
                    )}
                    <div className={`flex flex-col ${isMe ? "items-end" : "items-start"}`}>
                      {!isMe && <span className="text-[10px] text-zinc-500 mb-1 ml-1">{pName}</span>}
                      <Bubble align={isMe ? "end" : "start"} variant={isMe ? "default" : "secondary"}>
                        <BubbleContent>{msg.text}</BubbleContent>
                      </Bubble>
                    </div>
                  </div>
                );
              })}
            </BubbleGroup>
          </ScrollArea>
        </div>

        <div className="p-4 border-t border-zinc-800 bg-zinc-950">
          <form onSubmit={handleSend} className="flex w-full gap-2">
            <Input 
              value={input} 
              onChange={(e) => setInput(e.target.value)} 
              placeholder={chatI18n.placeholder} 
              className="flex-1 bg-zinc-900 border-zinc-800 text-white placeholder:text-zinc-500"
            />
            <Button type="submit" disabled={!input.trim()} className="bg-indigo-600 hover:bg-indigo-500 text-white">
              {chatI18n.send}
            </Button>
          </form>
        </div>
      </SheetContent>
    </Sheet>
  );
}
