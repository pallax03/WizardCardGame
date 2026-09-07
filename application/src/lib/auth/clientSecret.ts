import { cookies } from "next/headers";

const COOKIE_PREFIX = "wizard_secret_";
const COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 7;

function cookieName(lobbyId: string): string {
  const safeLobbyId = lobbyId.replace(/[^A-Za-z0-9_-]/g, "_");
  return `${COOKIE_PREFIX}${safeLobbyId}`;
}

function cookieOptions() {
  return {
    httpOnly: true as const,
    sameSite: "lax" as const,
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: COOKIE_MAX_AGE_SECONDS,
  };
}

export async function setClientSecretCookie(lobbyId: string, secret: string): Promise<void> {
  const store = await cookies();
  store.set(cookieName(lobbyId), secret, cookieOptions());
}

export async function getClientSecretCookie(lobbyId: string): Promise<string | undefined> {
  const store = await cookies();
  return store.get(cookieName(lobbyId))?.value;
}

export async function clearClientSecretCookie(lobbyId: string): Promise<void> {
  const store = await cookies();
  store.delete(cookieName(lobbyId));
}

export async function authHeadersForLobby(lobbyId: string): Promise<Record<string, string>> {
  const secret = await getClientSecretCookie(lobbyId);
  if (!secret) return {};
  return { Authorization: `Bearer ${secret}` };
}
