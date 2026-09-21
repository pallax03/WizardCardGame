import { LobbyPlayer } from "@/features/lobby-session/types";

export interface LobbyViewProps {
  maxPlayers?: number;
}

export interface LobbyHeaderProps {
  lobbyCode: string;
}

export interface PlayerListProps {
  players: LobbyPlayer[];
  maxPlayers: number;
  currentUserId: number | null;
  connectedPlayerIds: number[];
  activeBotSlot: number | null;
  isAddingBot: boolean;
  removingBotId: number | null;
  canManagePlayers: boolean;
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
  canRemove: boolean;
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
  isStarting: boolean;
  onLeave: () => void;
  onStart: () => void;
  isResuming?: boolean;
  isDiscarding?: boolean;
  onDiscard?: () => void;
  disableStart?: boolean;
  discardMode?: "paused" | "finished";
  status?: "WAITING" | "IN_GAME" | "DISCONNECTING" | "PAUSED" | "FINISHED";
  isPausing?: boolean;
  onPause?: () => void;
  onExitHome?: () => void;
}