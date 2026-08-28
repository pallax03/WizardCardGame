import { t } from "./core";
import { it } from "./dictionaries";

export const errorMessages = t("errors");
export type ErrorCode = keyof typeof it.errors;

export function getErrorMessage(errorCode?: string | null): string {
  if (!errorCode) return "";
  return errorMessages[errorCode as ErrorCode] ?? errorMessages.UNKNOWN_ERROR;
}