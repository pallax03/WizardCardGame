import { LobbyView } from "@/features/lobby/components/LobbyView";
import { cookies } from "next/headers";
import { getGameI18n } from "@/i18n/game";

export async function generateMetadata() {
  const cookieStore = await cookies();
  const lang = cookieStore.get("wizard_lang")?.value === "en" ? "en" : "it";
  const i18n = getGameI18n(lang);
  return {
    title: i18n.metadata.lobbyTitle,
  };
}
export default async function LobbyPage() {
  return (
    <main className="app-page p-6 flex items-center justify-center">
      <LobbyView/>
    </main>
  );
}