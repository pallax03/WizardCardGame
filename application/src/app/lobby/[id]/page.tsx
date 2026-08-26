import { LobbyView } from "@/features/lobby/components/lobby-view";

export default async function LobbyPage() {
  return (
    <main className="min-h-screen bg-slate-950 text-slate-100 p-6 flex items-center justify-center">
      <LobbyView/>
    </main>
  );
}