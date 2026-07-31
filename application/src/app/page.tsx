import Link from "next/link";

export default function Home() {
  return (
    <main className="min-h-screen bg-zinc-950 text-white flex flex-col items-center justify-center p-8 selection:bg-purple-500/30">
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-indigo-900/20 via-zinc-950 to-zinc-950"></div>

      <div className="relative z-10 flex flex-col items-center gap-6">
        <h1 className="text-6xl md:text-8xl font-extrabold tracking-tighter text-transparent bg-clip-text bg-gradient-to-br from-indigo-300 via-purple-300 to-pink-300 drop-shadow-sm">
          Wizard
        </h1>
        <p className="text-xl md:text-2xl text-zinc-400 font-light tracking-wide max-w-2xl text-center">
          Lobby in costruzione...
        </p>

        <div className="mt-8 flex gap-4">
          <Link
            href="/lobby/create"
            className="px-8 py-3 rounded-full bg-white/10 hover:bg-white/15 border border-white/10 backdrop-blur-sm transition-all duration-300 font-medium text-sm tracking-wide flex items-center justify-center"
          >
            Crea Partita
          </Link>
          <Link
            href="/lobby/1"
            className="px-8 py-3 rounded-full bg-indigo-600 hover:bg-indigo-500 shadow-lg shadow-indigo-500/25 transition-all duration-300 font-medium text-sm tracking-wide flex items-center justify-center"
          >
            Unisciti
          </Link>
        </div>
      </div>
    </main>
  );
}

