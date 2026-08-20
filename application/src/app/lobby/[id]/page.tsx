import { gameI18n } from '@/i18n/game';
import { Sparkles } from 'lucide-react';
import { Card, CardDescription, CardHeader, CardTitle } from '@/ui/components/card';

export default async function LobbyPage({
    params,
}: {
    params: Promise<{ id: string }>;
}) {
    const { id } = await params;
    return (
        <main className="relative grid min-h-dvh place-items-center overflow-hidden bg-zinc-950 px-4 py-10 text-white selection:bg-indigo-400/30">
            <div aria-hidden="true" className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_50%_-10%,rgba(99,102,241,0.25),transparent_45%)]" />
            <section className="relative z-10 flex w-full max-w-lg flex-col items-center gap-7 text-center">
                <span className="grid size-14 place-items-center rounded-3xl border border-indigo-300/10 bg-indigo-500/15 text-indigo-300 shadow-xl shadow-indigo-950/30"><Sparkles className="size-6" /></span>
                <div className="space-y-3">
                    <h1 className="text-gradient-primary text-5xl font-bold sm:text-6xl">{gameI18n.lobby.title}</h1>
                    <p className="text-zinc-400">{gameI18n.lobby.description}</p>
                </div>
                <Card className="w-full gap-5 border border-white/8 bg-zinc-900/65 py-6 text-left shadow-2xl shadow-black/30 backdrop-blur-xl">
                    <CardHeader>
                        <div className="flex items-center justify-between gap-3">
                            <div>
                                <CardDescription className="text-xs font-medium tracking-wider text-zinc-500 uppercase">{gameI18n.lobby.subtitle}</CardDescription>
                                <CardTitle className="mt-1 font-mono text-2xl tracking-wider text-white">{id}</CardTitle>
                            </div>
                        </div>
                    </CardHeader>
                </Card>
            </section>
        </main>
    );
}
