export const errorMessages = {
  EMPTY_USERNAME: "Inserisci un nome utente per continuare.",
  EMPTY_LOBBY_ID: "Inserisci il codice della stanza.",
  ROOM_NOT_FOUND: "Stanza non trovata o piena.",
  CREATE_FAILED: "Errore durante la creazione della stanza.",
  ADD_BOT_FAILED: "Errore durante l'aggiunta del bot.",
  LEAVE_FAILED: "Errore durante l'uscita dalla stanza.",
  SERVER_ERROR: "Errore di risposta dal server backend.",
  CONNECTION_ERROR: "Impossibile connettersi al server backend.",
  UNKNOWN_ERROR: "Si è verificato un errore inatteso.",
} as const;

export type ErrorCode = keyof typeof errorMessages;

export function getErrorMessage(errorCode?: string | null): string {
  if (!errorCode) return "";
  return errorMessages[errorCode as ErrorCode] ?? errorMessages.UNKNOWN_ERROR;
}