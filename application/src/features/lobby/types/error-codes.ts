export const LOBBY_ERROR_LIST = [
  "EMPTY_USERNAME",
  "EMPTY_LOBBY_ID",
  "LOBBY_NOT_FOUND",
  "CREATE_FAILED",
  "ADD_BOT_FAILED",
  "LEAVE_FAILED",
  "SERVER_ERROR",
  "CONNECTION_ERROR",
] as const;

export type LobbyErrorCode = (typeof LOBBY_ERROR_LIST)[number];

export const LOBBY_ERRORS = Object.fromEntries(
  LOBBY_ERROR_LIST.map((code) => [code, code])
) as Record<LobbyErrorCode, LobbyErrorCode>;