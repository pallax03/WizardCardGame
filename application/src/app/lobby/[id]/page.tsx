import { appI18n } from '@/i18n/game';
import { MessageCircle, Sparkles, Users } from 'lucide-react';
import { Badge } from '@/ui/components/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/ui/components/card';

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
                    <h1 className="text-gradient-primary text-5xl font-bold sm:text-6xl">{appI18n.lobby.title}</h1>
                    <p className="text-zinc-400">La lobby è pronta. Condividi il codice e attendi gli altri giocatori.</p>
                </div>
                <Card className="w-full gap-5 border border-white/8 bg-zinc-900/65 py-6 text-left shadow-2xl shadow-black/30 backdrop-blur-xl">
                    <CardHeader>
                        <div className="flex items-center justify-between gap-3">
                            <div>
                                <CardDescription className="text-xs font-medium tracking-wider text-zinc-500 uppercase">{appI18n.lobby.subtitle}</CardDescription>
                                <CardTitle className="mt-1 font-mono text-2xl tracking-wider text-white">{id}</CardTitle>
                            </div>
                            <Badge className="gap-1.5 bg-emerald-400/10 text-emerald-300"><span className="size-1.5 rounded-full bg-emerald-400" /> Online</Badge>
                        </div>
                    </CardHeader>
                    <CardContent className="grid gap-3 sm:grid-cols-2">
                        <div className="flex items-center gap-3 rounded-2xl bg-white/4 p-3"><span className="grid size-9 place-items-center rounded-xl bg-white/5 text-zinc-400"><Users className="size-4" /></span><div><p className="text-sm font-medium text-zinc-200">Lobby condivisa</p><p className="text-xs text-zinc-500">Gioca con i tuoi amici</p></div></div>
                        <div className="flex items-center gap-3 rounded-2xl bg-white/4 p-3"><span className="grid size-9 place-items-center rounded-xl bg-indigo-500/10 text-indigo-300"><MessageCircle className="size-4" /></span><div><p className="text-sm font-medium text-zinc-200">Chat integrata</p><p className="text-xs text-zinc-500">Lobby e messaggi privati</p></div></div>
                    </CardContent>
                </Card>
                <p className="text-xs text-zinc-600">Apri la chat dal pulsante in basso a destra.</p>
            </section>
        </main>
    );
}
