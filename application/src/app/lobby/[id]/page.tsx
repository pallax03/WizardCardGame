export default async function LobbyPage({
    params,
}: {
    params: Promise<{ id: string }>;
}) {
    const { id } = await params;
    return (
        <main className="min-h-screen bg-zinc-950 text-white flex flex-col items-center justify-center p-8 selection:bg-purple-500/30">
            <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-indigo-900/20 via-zinc-950 to-zinc-950"></div>

            <div className="relative z-10 flex flex-col items-center gap-6">
                <h1 className="text-6xl md:text-8xl font-extrabold tracking-tighter text-transparent bg-clip-text bg-gradient-to-br from-indigo-300 via-purple-300 to-pink-300 drop-shadow-sm">
                    Wizard Lobby
                </h1>
                <p className="text-xl md:text-2xl text-zinc-400 font-light tracking-wide max-w-2xl text-center">
                    Lobby: {id}
                </p>

            </div>
        </main>
    );
}
