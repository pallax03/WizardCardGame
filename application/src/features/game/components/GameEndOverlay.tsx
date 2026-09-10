"use client";

import Confetti from "react-confetti";
import { useWindowSize } from "react-use";
import { Badge } from "@/ui/components/badge";
import { Button } from "@/ui/components/button";
import { Card as UiCard, CardContent, CardHeader, CardTitle, CardDescription } from "@/ui/components/card";

export interface ScoreItem {
  id: number;
  name: string;
  score: number;
}

interface GameEndOverlayProps {
  isGameEnded: boolean;
  sortedScoreboard: ScoreItem[];
  playerId?: number;
  onReturnToLobby: () => void;
}

export function GameEndOverlay({
  isGameEnded,
  sortedScoreboard,
  playerId,
  onReturnToLobby,
}: GameEndOverlayProps) {
  const { width, height } = useWindowSize();

  if (!isGameEnded) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4 animate-in fade-in duration-300">
      <Confetti
        width={width || 1920}
        height={height || 1080}
        numberOfPieces={350}
        recycle={false}
        style={{ zIndex: 60, position: "fixed", top: 0, left: 0 }}
      />
      <UiCard className="w-full max-w-lg bg-zinc-900/95 border-2 border-amber-500/80 shadow-[0_0_50px_rgba(245,158,11,0.25)] text-center overflow-hidden z-50">
        <CardHeader className="bg-gradient-to-b from-amber-500/10 to-transparent pb-4 border-b border-zinc-800">
          <Badge
            variant="outline"
            className="w-fit mx-auto mb-2 border-amber-500/50 text-amber-400 bg-amber-500/10 px-3 py-0.5 text-xs font-semibold uppercase tracking-wider"
          >
            Partita Conclusa
          </Badge>
          <CardTitle className="text-3xl font-black text-amber-400 tracking-wider uppercase">
            🏆 Risultati Finali
          </CardTitle>
          <CardDescription className="text-zinc-400 text-sm mt-1">
            Ecco la classifica finale della partita
          </CardDescription>
        </CardHeader>

        <CardContent className="p-6 space-y-6">
          {sortedScoreboard.length > 0 && (
            <div className="space-y-2 bg-zinc-950/60 rounded-xl p-3 border border-zinc-800/80">
              {sortedScoreboard.map((item, index) => {
                const isWinner = index === 0;
                const isMe = item.id === playerId;

                return (
                  <div
                    key={item.id}
                    className={`flex items-center justify-between px-4 py-2.5 rounded-lg transition-all ${
                      isWinner
                        ? "bg-amber-500/20 border border-amber-500/40 text-amber-300 font-bold"
                        : isMe
                        ? "bg-zinc-800/80 text-white border border-zinc-700"
                        : "bg-zinc-900/50 text-zinc-300"
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <span
                        className={`w-6 h-6 flex items-center justify-center rounded-full text-xs font-black ${
                          isWinner
                            ? "bg-amber-400 text-zinc-950"
                            : "bg-zinc-800 text-zinc-400"
                        }`}
                      >
                        {index + 1}
                      </span>
                      <span className="text-sm font-semibold truncate max-w-[180px]">
                        {item.name} {isMe && "(Tu)"}
                      </span>
                    </div>
                    <span className="font-mono font-extrabold text-base">
                      {item.score} <span className="text-xs font-normal text-zinc-500">pt</span>
                    </span>
                  </div>
                );
              })}
            </div>
          )}

          <Button
            onClick={onReturnToLobby}
            className="w-full bg-amber-500 hover:bg-amber-600 text-zinc-950 font-black py-6 text-base tracking-wide uppercase transition-all shadow-lg hover:shadow-amber-500/25"
          >
            Torna Alla Lobby
          </Button>
        </CardContent>
      </UiCard>
    </div>
  );
}