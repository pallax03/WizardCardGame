import { redirect } from 'next/navigation';

export default function LobbyCreate() {
    async function createLobby(formData: FormData) {
        "use server";
        
        const name = formData.get("name")?.toString();
        if (!name) return;

        const res = await fetch(`${process.env.NEXT_PUBLIC_BACKEND_URL}/api/lobby`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name, bot: null })
        });
        
        if (!res.ok) {
            throw new Error("Failed to create lobby");
        }
        
        const data = await res.json();

        redirect(`/lobby/${data.lobbyId}?playerId=${data.playerId}`);
    }

    return (
        <main className="min-h-screen bg-zinc-950 text-white flex flex-col items-center justify-center p-8 selection:bg-purple-500/30">
            <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-indigo-900/20 via-zinc-950 to-zinc-950"></div>

            <div className="relative z-10 flex flex-col items-center gap-6">
                <h1 className="text-6xl md:text-8xl font-extrabold tracking-tighter text-transparent bg-clip-text bg-gradient-to-br from-indigo-300 via-purple-300 to-pink-300 drop-shadow-sm">
                    Wizard Lobby
                </h1>
                
                <form action={createLobby} className="flex flex-col gap-4 w-full max-w-sm mt-8">
                    <input 
                        type="text" 
                        name="name"
                        placeholder="Il tuo nome" 
                        required
                        className="px-4 py-3 rounded-xl bg-white/5 border border-white/10 focus:border-purple-500/50 focus:outline-none transition-colors"
                    />
                    <button 
                        type="submit"
                        className="px-8 py-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 shadow-lg shadow-indigo-500/25 transition-all duration-300 font-medium tracking-wide"
                    >
                        Crea Partita
                    </button>
                </form>
            </div>
        </main>
    );
}
