import { LobbyPlayer } from "@/features/lobby-session/types";

export interface LobbyViewProps {
  maxPlayers?: number;
}

export interface LobbyHeaderProps {
  roomCode: string;
}

export interface PlayerListProps {
  players: LobbyPlayer[];
  maxPlayers: number;
  currentUserId: number | string | null;
  connectedPlayerIds: (number | string)[];
  activeBotSlot: number | null;
  isAddingBot: boolean;
  removingBotId: number | string | null;
  onSelectBotSlot: (slotIndex: number | null) => void;
  onAddBot: (difficulty: string) => Promise<void>;
  onRemoveBot: (botId: number) => Promise<void>;
}

export interface PlayerCardProps {
  player: LobbyPlayer;
  isMe: boolean;
  isBot: boolean;
  isOnline: boolean;
  isRemoving: boolean;
  onRemoveBot: (botId: number) => Promise<void>;
}

export interface EmptySlotProps {
  isSelecting: boolean;
  isAddingBot: boolean;
  onOpenSelect: () => void;
  onCloseSelect: () => void;
  onAddBot: (difficulty: string) => Promise<void>;
}

export interface LobbyActionsProps {
  isLeaving: boolean;
  onLeave: () => void;
  onStart: () => void;
}