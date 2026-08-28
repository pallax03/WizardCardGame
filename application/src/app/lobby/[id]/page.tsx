import { LobbyView } from "@/features/lobby/components/LobbyView";

export default async function LobbyPage() {
  return (
    <main className="app-page p-6 flex items-center justify-center">
      <LobbyView/>
    </main>
  );
}