"use client";

import { useLobbySession } from "@/features/lobby-session";

// Semantic chat hook: it exposes chat data without owning the shared socket.
export function useChat() {
  const { messages, sendMessage, connectionState } = useLobbySession();
  return { messages, sendMessage, connectionState };
}
