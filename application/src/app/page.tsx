import { redirect } from 'next/navigation';
import { ArrowRight, Sparkles, Users } from 'lucide-react';
import { appI18n } from '@/i18n/game';
import { Badge } from '@/ui/components/badge';
import { Button } from '@/ui/components/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/ui/components/card';
import { Input } from '@/ui/components/input';

export default function LobbyCreate() {
    async function createLobby(formData: FormData) {
        "use server";
        
        const name = formData.get("name")?.toString();
        const customLobbyId = formData.get("lobby")?.toString();
        
        if (!name) return;

        const url = customLobbyId 
            ? `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/lobby/${customLobbyId}`
            : `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/lobby`;

        const res = await fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name, bot: null })
        });
        
        if (!res.ok) {
            throw new Error(appI18n.errors.createLobby);
        }
        
        const data = await res.json();

        redirect(`/lobby/${data.lobbyId}?playerId=${data.playerId}`);
    }
    return (
        <main className="relative grid min-h-dvh place-items-center overflow-hidden bg-zinc-950 px-4 py-10 text-white selection:bg-indigo-400/30">
            <div aria-hidden="true" className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_50%_-15%,rgba(99,102,241,0.28),transparent_42%)]" />
            <div aria-hidden="true" className="pointer-events-none absolute inset-0 opacity-[0.025] [background-image:linear-gradient(rgba(255,255,255,.7)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,.7)_1px,transparent_1px)] [background-size:48px_48px]" />

            <section className="relative z-10 flex w-full max-w-md flex-col items-center gap-7">
                <Badge variant="outline" className="gap-1.5 border-indigo-300/15 bg-indigo-400/8 px-3 text-indigo-200">
                    <Sparkles className="size-3" /> {appI18n.home.eyebrow}
                </Badge>
                <div className="space-y-3 text-center">
                    <h1 className="text-gradient-primary text-5xl font-bold sm:text-6xl">{appI18n.home.title}</h1>
                    <p className="mx-auto max-w-sm text-sm leading-relaxed text-zinc-400 sm:text-base">{appI18n.home.description}</p>
                </div>

                <Card className="w-full gap-5 border border-white/8 bg-zinc-900/70 py-6 shadow-2xl shadow-black/30 backdrop-blur-xl">
                    <CardHeader>
                        <div className="mb-2 grid size-10 place-items-center rounded-2xl bg-indigo-500/15 text-indigo-300"><Users className="size-5" /></div>
                        <CardTitle className="text-lg text-white">{appI18n.home.formTitle}</CardTitle>
                        <CardDescription className="text-zinc-400">{appI18n.home.formDescription}</CardDescription>
                    </CardHeader>
                    <CardContent>
                        <form action={createLobby} className="flex flex-col gap-3">
                            <Input type="text" name="name" placeholder={appI18n.home.namePlaceholder} aria-label={appI18n.home.namePlaceholder} required autoComplete="nickname" className="h-11 border-white/8 bg-white/5 px-4 text-white placeholder:text-zinc-600 focus-visible:border-indigo-400/50 focus-visible:ring-indigo-400/15" />
                            <Input type="text" name="lobby" placeholder={appI18n.home.lobbyPlaceholder} aria-label={appI18n.home.lobbyPlaceholder} autoComplete="off" className="h-11 border-white/8 bg-white/5 px-4 text-white placeholder:text-zinc-600 focus-visible:border-indigo-400/50 focus-visible:ring-indigo-400/15" />
                            <Button type="submit" size="lg" className="mt-2 h-11 w-full bg-indigo-500 text-white shadow-lg shadow-indigo-950/40 hover:bg-indigo-400">
                                {appI18n.home.createButton}<ArrowRight data-icon="inline-end" />
                            </Button>
                        </form>
                    </CardContent>
                </Card>
            </section>
        </main>
    );
}
