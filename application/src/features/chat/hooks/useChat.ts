import { useState, useEffect, useCallback, useRef } from "react";
import { AnyMessage, ChatMessage } from "../types";

export function useChat(lobbyId: string | null, playerId: number | null) {
    const [messages, setMessages] = useState<AnyMessage[]>([]);
    const ws = useRef<WebSocket | null>(null);

    useEffect(() => {
        if (!lobbyId || playerId === null || playerId === undefined || isNaN(playerId)) return;

        // In environments like Docker or K8s, NEXT_PUBLIC_WS_URL can be set at build/run time.
        // It allows dynamic URLs (e.g. wss://api.domain.com in production) instead of hardcoding.
        const url = `${process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:5002'}/lobby/${lobbyId}/player/${playerId}`;
        const socket = new WebSocket(url);

        socket.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                if (data.type === "message") {
                    setMessages((prev) => [...prev, data]);
                } else if (data.type === "system") {
                    setMessages((prev) => [...prev, { ...data, timestamp: data.timestamp || new Date().toISOString() }]);
                } else if (data.event) {
                    setMessages((prev) => [...prev, {
                        type: "event",
                        event: data.event,
                        timestamp: new Date().toISOString()
                    }]);
                }
            } catch (err) {
                console.error("Failed to parse ws message", err);
            }
        };

        ws.current = socket;

        return () => {
            socket.close();
            ws.current = null;
        };
    }, [lobbyId, playerId]);

    const sendMessage = useCallback((text: string, destinationId?: number) => {
        if (!ws.current || ws.current.readyState !== WebSocket.OPEN) return;
        if (playerId === null || playerId === undefined || isNaN(playerId)) return;

        const payload: ChatMessage = {
            type: "message",
            playerId: playerId!,
            text,
            destinationId,
            timestamp: new Date().toISOString()
        };
        ws.current.send(JSON.stringify(payload));
    }, [playerId]);

    return { messages, sendMessage };
}
