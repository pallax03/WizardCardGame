import { useState, useEffect, useCallback, useRef } from "react";
import { useRouter } from "next/navigation";
import { AnyMessage, ChatMessage } from "../types";

export function useChat(lobbyId: string | null, playerId: number | null) {
    const router = useRouter();
    const [messages, setMessages] = useState<AnyMessage[]>([]);
    const [connectionState, setConnectionState] = useState<"connecting" | "open" | "closed">("connecting");
    const ws = useRef<WebSocket | null>(null);

    useEffect(() => {
        if (!lobbyId || playerId === null || playerId === undefined || isNaN(playerId)) return;

        const url = `${process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:5002'}/lobby/${lobbyId}/player/${playerId}`;
        const socket = new WebSocket(url);

        socket.onopen = () => setConnectionState("open");

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

        socket.onerror = () => {
            setConnectionState("closed");
        };

        socket.onclose = (e) => {
            setConnectionState("closed");
            if (e.code !== 1000) {
                router.replace("/");
            }
        };

        ws.current = socket;

        return () => {
            socket.close();
            ws.current = null;
        };
    }, [lobbyId, playerId, router]);

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
        if (destinationId !== undefined) {
            setMessages((prev) => [...prev, payload]);
        }
    }, [playerId]);

    return { messages, sendMessage, connectionState };
}
