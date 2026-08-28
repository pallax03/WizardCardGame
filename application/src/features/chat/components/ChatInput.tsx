"use client";

import { SubmitEvent, useState } from "react";
import { Send } from "lucide-react";
import { chatI18n } from "@/i18n/chat";
import { Button } from "@/ui/components/button";
import { Input } from "@/ui/components/input";

interface ChatInputProps {
  activePrivateId: number | null;
  connectionState: string;
  onSendMessage: (text: string) => void;
}

export function ChatInput({ activePrivateId, connectionState, onSendMessage }: ChatInputProps) {
  const [input, setInput] = useState("");
  const placeholder = activePrivateId === null ? chatI18n.placeholder : chatI18n.privatePlaceholder;

  const handleSend = (event: SubmitEvent<HTMLFormElement>) => {
    event.preventDefault();
    const text = input.trim();
    if (!text) return;
    onSendMessage(text);
    setInput("");
  };

  return (
    <form onSubmit={handleSend} className="flex shrink-0 items-center gap-2 border-t border-white/8 bg-zinc-950 px-3 pt-3 pb-[max(.75rem,env(safe-area-inset-bottom))]">
      <Input
        value={input}
        onChange={(event) => setInput(event.target.value)}
        placeholder={placeholder}
        aria-label={placeholder}
        autoComplete="off"
        className="h-11 rounded-full border-white/8 bg-white/5 px-4 text-base text-white placeholder:text-zinc-600 focus-visible:border-indigo-400/50 focus-visible:ring-indigo-400/15 sm:text-sm"
      />
      <Button
        type="submit"
        size="icon-lg"
        disabled={!input.trim() || connectionState !== "open"}
        aria-label={chatI18n.send}
        className="size-11 rounded-full bg-indigo-500 text-white shadow-lg shadow-indigo-950/30 hover:bg-indigo-400"
      >
        <Send className="size-4" />
      </Button>
    </form>
  );
}
