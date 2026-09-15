export type ChatMessage = {
    type: 'message';
    playerId: number;
    destinationId?: number;
    text: string;
    timestamp: string;
};

export type SystemMessage = {
    type: 'system';
    playerId: number;
    action: 'joined' | 'left' | 'online' | 'offline' | 'paused' | 'resumed' | 'afk_replaced' | 'config_updated' | (string & {});
    timestamp: string;
};

export type EventMessage = {
    type: 'event';
    event: {
        type: string;
        action: string;
        playerId?: number;
        destinationId?: number;
        fields?: Record<string, unknown>;
    };
    timestamp: string;
};

export type AnyMessage = ChatMessage | SystemMessage | EventMessage;
