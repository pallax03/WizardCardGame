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
    action: 'joined' | 'left';
    timestamp: string; // added by frontend when receiving if not provided
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
