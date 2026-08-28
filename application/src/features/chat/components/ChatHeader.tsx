"use client";

import { ArrowLeft, LockKeyhole, Users, X } from "lucide-react";
import { AnimatePresence, motion } from "motion/react";
import { chatI18n } from "@/i18n/chat";
import { Badge } from "@/ui/components/badge";
import { Button } from "@/ui/components/button";
import { SheetClose, SheetDescription, SheetHeader, SheetTitle } from "@/ui/components/sheet";

interface ChatHeaderProps {
  activePrivateId: number | null;
  privateName: string | null;
  connectionState: string;
  connectedPlayerIds: number[];
  privatePeers: number[];
  playersMap: Record<number, string>;
  privateUnread: (peerId: number) => number;
  onClosePrivate: () => void;
  onOpenPrivate: (peerId: number) => void;
}

export function ChatHeader({
  activePrivateId,
  privateName,
  connectionState,
  connectedPlayerIds,
  privatePeers,
  playersMap,
  privateUnread,
  onClosePrivate,
  onOpenPrivate,
}: ChatHeaderProps) {
  return (
    <SheetHeader className="shrink-0 gap-0 border-b border-white/8 px-4 py-3">
      <div className="flex min-w-0 items-center gap-2">
        {activePrivateId !== null ? (
          <Button type="button" variant="ghost" size="icon" aria-label={chatI18n.backToLobby} className="rounded-full text-zinc-400 hover:bg-white/8 hover:text-white" onClick={onClosePrivate}>
            <ArrowLeft />
          </Button>
        ) : (
          <span className="grid size-8 shrink-0 place-items-center rounded-full bg-indigo-500/15 text-indigo-300"><Users className="size-4" /></span>
        )}
        <div className="min-w-0 flex-1">
          <AnimatePresence mode="wait" initial={false}>
            <motion.div key={activePrivateId ?? "lobby"} initial={{ opacity: 0, x: activePrivateId === null ? -8 : 8 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: activePrivateId === null ? -8 : 8 }} transition={{ duration: 0.16 }} className="flex items-center gap-2">
              <SheetTitle className="truncate text-sm font-semibold text-white">{activePrivateId === null ? chatI18n.title : privateName ?? `${chatI18n.fallbackPlayer} ${activePrivateId}`}</SheetTitle>
              {activePrivateId !== null && <Badge variant="secondary" className="h-4 gap-1 bg-violet-500/15 px-1.5 text-[10px] text-violet-300"><LockKeyhole className="size-2.5!" /> {chatI18n.privateBadge}</Badge>}
            </motion.div>
          </AnimatePresence>
          <SheetDescription className="flex items-center gap-1.5 truncate text-[11px] text-zinc-500">
            <span className={`size-1.5 rounded-full ${connectionState === "open" ? "bg-emerald-400" : "bg-amber-400"}`} />
            {activePrivateId === null ? `${chatI18n.playersOnline(connectedPlayerIds.length)} · ${connectionState === "open" ? chatI18n.connected : chatI18n.connecting}` : `${chatI18n.privateWith} ${privateName ?? chatI18n.fallbackPlayer}`}
          </SheetDescription>
        </div>
        <SheetClose aria-label={chatI18n.close} className="grid size-8 shrink-0 place-items-center rounded-full text-zinc-500 transition hover:bg-white/8 hover:text-white focus-visible:ring-2 focus-visible:ring-indigo-400 focus-visible:outline-none"><X className="size-4" /></SheetClose>
      </div>

      {activePrivateId === null && privatePeers.length > 0 && (
        <div className="mt-3 flex items-center gap-2 overflow-x-auto pb-0.5">
          <span className="shrink-0 text-[10px] font-medium tracking-wider text-zinc-600 uppercase">{chatI18n.privateMessages}</span>
          {privatePeers.map((peerId) => {
            const name = playersMap[peerId] ?? `${chatI18n.fallbackPlayer} ${peerId}`;
            const unread = privateUnread(peerId);
            return <Button key={peerId} type="button" variant="secondary" size="xs" className="relative shrink-0 rounded-full bg-white/6 text-zinc-300 hover:bg-white/10 hover:text-white" onClick={() => onOpenPrivate(peerId)}><LockKeyhole className="size-3" />{name}{unread > 0 && <Badge className="absolute -top-1 -right-1 h-4 min-w-4 bg-rose-500 px-1 text-[9px] text-white">{unread}</Badge>}</Button>;
          })}
        </div>
      )}
    </SheetHeader>
  );
}
