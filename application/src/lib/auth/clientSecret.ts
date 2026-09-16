import { cookies } from "next/headers";

const cookieName = (lobbyId: string) => `wizard_secret_${lobbyId}`;
const cookieOpts = {
  httpOnly: true,
  sameSite: "lax" as const,
  secure: process.env.NODE_ENV === "production",
  path: "/",
  maxAge: 60 * 60 * 24 * 7,
};

export async function setClientSecretCookie(lobbyId: string, secret: string) {
  (await cookies()).set(cookieName(lobbyId), secret, cookieOpts);
}

export async function getClientSecretCookie(lobbyId: string) {
  return (await cookies()).get(cookieName(lobbyId))?.value;
}

export async function clearClientSecretCookie(lobbyId: string) {
  (await cookies()).delete(cookieName(lobbyId));
}

export async function authHeadersForLobby(lobbyId: string) {
  const secret = await getClientSecretCookie(lobbyId);
  return secret ? { Authorization: `Bearer ${secret}` } : {};
}
