import { LobbyView } from "@/features/lobby/components/lobby-view";

interface LobbyPageProps {
  params: Promise<{
    id: string;
  }>;
  searchParams: Promise<{
    playerId?: string;
    playerName?: string;
  }>;
}

export default async function LobbyPage({ params, searchParams }: LobbyPageProps) {
  return (
    <main className="min-h-screen bg-slate-950 text-slate-100 p-6 flex items-center justify-center">
      <LobbyView/>
    </main>
  );
}