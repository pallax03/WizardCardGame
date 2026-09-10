"use client";

import { useRouter } from "next/navigation";
import { useGameBoard, TRICK_REVEAL_SECONDS } from "../hooks/useGameBoard";
import { cardEquals } from "../state/gameReducer";
import type { Card } from "../types";
import { GameActionControls } from "./GameActionControls";
import { GameCardView } from "./GameCardView";
import { GameHeader } from "./GameHeader";
import { GameScoreboard } from "./GameScoreboard";
import { GameTurnBanner } from "./GameTurnBanner";
import { PlayerHand } from "./PlayerHand";
import { TrumpArea } from "./TrumpArea";
import { Badge } from "@/ui/components/badge";
import { Button } from "@/ui/components/button";
import { Card as UiCard, CardContent, CardHeader, CardTitle, CardDescription } from "@/ui/components/card";
import { useRef, useState } from "react";
import Confetti from "react-confetti";
import { useWindowSize } from "react-use";

interface GameBoardProps {
  customPlayerId?: number;
}

export function GameBoard({ customPlayerId }: GameBoardProps) {
  const router = useRouter();
  const {
    lobbyId,
    playerId,
    connectionState,
    lobby,
    playersMap,
    gameState,
    revealedTrick,
    revealSecondsLeft,
    isMyTurn,
    canChooseTrump,
    canBid,
    canPlay,
    turnPrompt,
    isCardPlayable,
    selectedCard,
    setSelectedCard,
    bidInput,
    setBidInput,
    selectedColor,
    setSelectedColor,
    isSubmitting,
    actionStatus,
    isRestoring,
    snapshotError,
    refreshSnapshot,
    handleChooseTrump,
    handlePlaceBid,
    handlePlayCard,
  } = useGameBoard(customPlayerId);

  const players = lobby?.players ?? [];

  // Il player corrente sta sempre in basso (indice 0), gli altri seguono in senso orario.
  const myIndex = players.findIndex((p) => p.id === playerId);
  const orderedPlayers =
    myIndex !== -1
      ? [...players.slice(myIndex), ...players.slice(0, myIndex)]
      : players;

  // Seggiolini a cavallo del bordo (stile poker): il badge sta sul rail,
  // la carta giocata sta sempre tra badge e centro (davanti al player).
  // dir = direzione flex del seggiolino, cardFirst = carta verso il centro.
  type SeatLayout = {
    pos: string;
    dir: "flex-col" | "flex-row";
    cardFirst: boolean;
  };

  const SEAT_BOTTOM: SeatLayout = {
    pos: "bottom-0 left-1/2 -translate-x-1/2 translate-y-1/4",
    dir: "flex-col",
    cardFirst: true,
  };
  const SEAT_TOP: SeatLayout = {
    pos: "top-0 left-1/2 -translate-x-1/2 -translate-y-1/4",
    dir: "flex-col",
    cardFirst: false,
  };

  const SEAT_PRESETS: Record<number, SeatLayout[]> = {
    2: [
      SEAT_BOTTOM,
      SEAT_TOP,
    ],
    3: [
      SEAT_BOTTOM,
      { pos: "left-0 top-[38%] -translate-x-1/4 -translate-y-1/2", dir: "flex-row", cardFirst: false },
      { pos: "right-0 top-[38%] translate-x-1/4 -translate-y-1/2", dir: "flex-row", cardFirst: true },
    ],
    4: [
      SEAT_BOTTOM,
      { pos: "left-0 top-1/2 -translate-x-1/4 -translate-y-1/2", dir: "flex-row", cardFirst: false },
      SEAT_TOP,
      { pos: "right-0 top-1/2 translate-x-1/4 -translate-y-1/2", dir: "flex-row", cardFirst: true },
    ],
    5: [
      SEAT_BOTTOM,
      { pos: "left-0 top-[55%] -translate-x-1/4 -translate-y-1/2", dir: "flex-row", cardFirst: false },
      { pos: "left-[24%] top-0 -translate-x-1/4 -translate-y-1/4", dir: "flex-col", cardFirst: false },
      { pos: "left-[76%] top-0 translate-x-1/4 -translate-y-1/4", dir: "flex-col", cardFirst: false },
      { pos: "right-0 top-[55%] translate-x-1/4 -translate-y-1/2", dir: "flex-row", cardFirst: true },
    ],
    6: [
      SEAT_BOTTOM,
      { pos: "left-0 top-[55%] -translate-x-1/4 -translate-y-1/2", dir: "flex-row", cardFirst: false },
      { pos: "left-[20%] top-0 -translate-x-1/4 -translate-y-1/4", dir: "flex-col", cardFirst: false },
      SEAT_TOP,
      { pos: "left-[80%] top-0 translate-x-1/4 -translate-y-1/4", dir: "flex-col", cardFirst: false },
      { pos: "right-0 top-[55%] translate-x-1/4 -translate-y-1/2", dir: "flex-row", cardFirst: true },
    ],
  };

  const seatLayout = (idx: number, total: number): SeatLayout => {
    const preset = SEAT_PRESETS[Math.min(Math.max(total, 2), 6)] ?? SEAT_PRESETS[6];
    return preset[idx % preset.length];
  };

  const handleReturnToLobby = () => {
    // Naviga alla vista della lobby o alla pagina principale/selezione stanze
    if (lobbyId) {
      router.push(`/lobby/${lobbyId}`);
    } else {
      router.push("/");
    }
  };

    // 1. Aggiungi uno stato locale per il testing
  const [forceGameEnded, setForceGameEnded] = useState(false);

  // Drop-zone per il drag & drop delle carte: il tavolo da gioco.
  // (La carta trascinata vive in un portal su document.body, quindi e'
  // sempre sopra al tavolo senza bisogno di z-index nella pagina.)
  const tableRef = useRef<HTMLDivElement | null>(null);
  const [isCardDragging, setIsCardDragging] = useState(false);

  const handleDropCard = (card: Card) => {
    setSelectedCard(null);
    void handlePlayCard(card);
  };

  const { width, height } = useWindowSize();

  // 2. Aggiorna il controllo dello stato Ended
  const isGameEnded = gameState.status === "GAME_ENDED" || forceGameEnded;

  const sortedScoreboard = gameState.scoreboard
    ? Object.entries(gameState.scoreboard)
        .map(([pIdStr, entries]) => {
          const pId = Number(pIdStr);
          const playerObj = playersMap.get(pId);
          const playerName =
            typeof playerObj === "object" && playerObj !== null
              ? playerObj.name
              : playerObj ?? `Giocatore ${pId}`;

          // Se entries è un array (storico del tabellone), prendiamo il punteggio dell'ultimo round
          let finalScore = 0;
          if (Array.isArray(entries) && entries.length > 0) {
            const lastEntry = entries[entries.length - 1];
            finalScore = Number(lastEntry?.score ?? 0);
          } else if (typeof entries === "number") {
            finalScore = entries;
          } else if (entries && typeof entries === "object" && "score" in entries) {
            finalScore = Number((entries as { score: unknown }).score ?? 0);
          }

          return {
            id: pId,
            name: playerName,
            score: Number.isNaN(finalScore) ? 0 : finalScore,
          };
        })
        .sort((a, b) => b.score - a.score)
    : [];

  return (
    <div className="relative w-full max-w-7xl mx-auto space-y-6 pb-20 px-2 sm:px-4">
      {isGameEnded && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4 animate-in fade-in duration-300">
          <Confetti
            width={width}
            height={height}
            numberOfPieces={400}
            recycle={false} // Ferma la generazione dopo la prima caduta
            style={{ zIndex: 60, position: "fixed" }}
          />
          <UiCard className="w-full max-w-lg bg-zinc-900/95 border-2 border-amber-500/80 shadow-[0_0_50px_rgba(245,158,11,0.25)] text-center overflow-hidden">
            <CardHeader className="bg-gradient-to-b from-amber-500/10 to-transparent pb-4 border-b border-zinc-800">
              <Badge variant="outline" className="w-fit mx-auto mb-2 border-amber-500/50 text-amber-400 bg-amber-500/10 px-3 py-0.5 text-xs font-semibold uppercase tracking-wider">
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
              {/* Tabella Classifica Finale */}
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

              {/* Bottone per tornare alla lobby */}
              <Button
                onClick={handleReturnToLobby}
                className="w-full bg-amber-500 hover:bg-amber-600 text-zinc-950 font-black py-6 text-base tracking-wide uppercase transition-all shadow-lg hover:shadow-amber-500/25"
              >
                Torna Alla Lobby
              </Button>
            </CardContent>
          </UiCard>
        </div>
      )}

      {/* 1. Header & Stato Connessione */}
      <GameHeader
        lobbyId={lobbyId}
        playerId={playerId}
        connectionState={connectionState}
        round={gameState.round}
        status={gameState.status}
      />

      {/* 1b. Ripristino snapshot dal backend (reload/riconnessione) */}
      {isRestoring && (
        <div className="p-3 rounded-2xl border border-amber-400/50 bg-amber-950/60 text-amber-200 text-sm font-semibold text-center animate-pulse">
          🔄 Ripristino stato partita dal server...
        </div>
      )}
      {snapshotError && !isRestoring && (
        <div className="p-3 rounded-2xl border border-rose-700/60 bg-rose-950/60 text-rose-200 text-sm text-center space-y-2">
          <p className="font-semibold">⚠️ {snapshotError}</p>
          <button
            type="button"
            onClick={() => void refreshSnapshot("manual")}
            className="px-4 py-1.5 rounded-xl bg-rose-500 text-white text-xs font-bold uppercase tracking-wider hover:bg-rose-400 transition-colors"
          >
            Riprova sincronizzazione
          </button>
        </div>
      )}

      {/* 2b. Stato turno sopra il tavolo (il centro resta libero per la briscola) */}
      <div className="mx-auto w-full max-w-md">
        <GameTurnBanner
          isMyTurn={isMyTurn}
          turnPrompt={turnPrompt}
          lastError={gameState.lastError}
          actionStatus={actionStatus}
        />
      </div>

      {/* 2. TAVOLO DA GIOCO TEXAS HOLD'EM (Poker Table Felt) + DROP-ZONE CARTE */}
      <div
        ref={tableRef}
        className={`relative w-full my-10 py-10 px-4 min-h-[560px] sm:min-h-[600px] rounded-[120px] sm:rounded-[180px] bg-gradient-to-b from-emerald-900 via-emerald-800 to-emerald-950 border-[12px] border-amber-950/80 shadow-[inset_0_0_80px_rgba(0,0,0,0.8),0_20px_50px_rgba(0,0,0,0.6)] transition-shadow duration-200 ${isCardDragging ? "ring-4 ring-emerald-300/80" : ""}`}
      >

        {/* Hint mentre trascini una carta */}
        {isCardDragging && (
          <div className="pointer-events-none absolute top-5 left-1/2 z-30 -translate-x-1/2 animate-pulse rounded-full border border-emerald-300/60 bg-emerald-500/20 px-4 py-1.5 text-[11px] font-black tracking-widest whitespace-nowrap text-emerald-100 uppercase">
            Rilascia qui per giocare
          </div>
        )}

        {/* Linea interna decorativa del feltro */}
        <div className="absolute inset-4 rounded-[100px] sm:rounded-[160px] border-2 border-emerald-600/30 pointer-events-none flex items-center justify-center">
          <span className="text-emerald-900/20 text-6xl sm:text-8xl font-black uppercase tracking-widest select-none">
            WIZARD
          </span>
        </div>

        {/* --- CENTRO TAVOLO: BRISCOLA --- */}
        <div className="absolute top-1/2 left-1/2 z-10 flex -translate-x-1/2 -translate-y-1/2 flex-col items-center gap-2">
          <TrumpArea
            trump={gameState.trump}
            effectiveTrumpColor={gameState.effectiveTrumpColor}
          />
          {gameState.followingColor && (
            <span className="rounded-full border border-emerald-600/40 bg-emerald-950/70 px-2.5 py-0.5 text-[10px] font-bold text-emerald-300">
              Seme di mano: {gameState.followingColor}
            </span>
          )}
        </div>

        {/* --- REVEAL DI FINE PRESA: tavolo congelato + conto alla rovescia --- */}
        {revealedTrick && (
          <div className="pointer-events-none absolute top-1/2 left-1/2 z-30 flex w-max max-w-[92%] -translate-x-1/2 -translate-y-1/2 flex-col items-center gap-2 rounded-3xl border border-amber-400/60 bg-zinc-950/95 p-3 shadow-2xl backdrop-blur-md sm:p-4">
            <span className="rounded-full border border-amber-400/50 bg-amber-500/15 px-3 py-0.5 text-[11px] font-black tracking-wider text-amber-300 uppercase">
              ★ Presa di {playersMap.get(revealedTrick.winnerId)?.name ?? `Giocatore ${revealedTrick.winnerId}`}
            </span>
            <div className="flex max-w-full flex-wrap items-start justify-center gap-2">
              {revealedTrick.entries.map((entry, index) => {
                const entryName = playersMap.get(entry.playerId)?.name ?? `Giocatore ${entry.playerId}`;
                const entryWinning = Boolean(
                  revealedTrick.winningCard && cardEquals(entry.card, revealedTrick.winningCard),
                );
                return (
                  <div key={`${entry.playerId}-${index}`} className="flex flex-col items-center gap-1">
                    <div className={entryWinning ? "rounded-xl ring-2 ring-amber-400 ring-offset-2 ring-offset-zinc-950" : ""}>
                      <GameCardView card={entry.card} size="sm" isClickable={false} />
                    </div>
                    <span className={`max-w-16 truncate text-[9px] font-bold ${entryWinning ? "text-amber-300" : "text-zinc-400"}`}>
                      {entryName}
                    </span>
                  </div>
                );
              })}
            </div>
            <span className="animate-pulse font-mono text-[11px] font-bold text-zinc-300">
              Si continua tra {revealSecondsLeft}s…
            </span>
            <div className="h-1 w-full overflow-hidden rounded-full bg-white/10">
              <div
                className="h-full rounded-full bg-amber-400 transition-[width] duration-1000 ease-linear"
                style={{ width: `${Math.max(0, Math.min(100, (revealSecondsLeft / TRICK_REVEAL_SECONDS) * 100))}%` }}
              />
            </div>
          </div>
        )}

        {/* --- GIOCATORI SUL RAIL, CARTA GIOCATA DAVANTI AL PLAYER --- */}
        {orderedPlayers.map((player, idx) => {
          const isCurrentTurn = gameState.currentTurn.playerId === player.id;
          const isMe = player.id === playerId;
          const playerBid = gameState.bids[player.id];
          const playerTricks = gameState.tricksWon[player.id] ?? 0;
          const isBot = Boolean(player.difficulty);
          const seat = seatLayout(idx, orderedPlayers.length);
          const played = gameState.table.find((entry) => entry.playerId === player.id);
          const isWinning = Boolean(
            played && gameState.winningCard && cardEquals(played.card, gameState.winningCard),
          );
          // Lo slot vuoto si vede solo a trick in corso (segnale "deve ancora giocare").
          const showSlot = played !== undefined || gameState.table.length > 0;

          const badge = (
            <div
              className={`flex items-center gap-2 rounded-2xl border px-2 py-1.5 shadow-xl backdrop-blur-md transition-all duration-300 sm:px-2.5 sm:py-2 ${
                isCurrentTurn
                  ? "border-amber-400 bg-amber-500/20 ring-2 ring-amber-400/60"
                  : "border-zinc-700/80 bg-zinc-900/90"
              }`}
            >
              <span
                className={`grid size-8 shrink-0 place-items-center rounded-full text-xs font-black ${
                  isMe ? "bg-amber-400 text-zinc-950" : "bg-zinc-700 text-zinc-100"
                }`}
              >
                {player.name.charAt(0).toUpperCase()}
              </span>
              <span className="min-w-0">
                <span className="flex items-center gap-1">
                  <span className="max-w-[84px] truncate text-xs font-bold text-white sm:max-w-[110px]">
                    {player.name}
                  </span>
                  {isMe && <Badge variant="secondary" className="px-1 py-0 text-[9px]">TU</Badge>}
                  {isBot && <Badge variant="outline" className="px-1 py-0 text-[9px] text-zinc-400">BOT</Badge>}
                </span>
                <span className="mt-0.5 flex items-center gap-2 font-mono text-[10px] text-zinc-400">
                  <span>Bid <strong className="text-amber-400">{playerBid !== undefined ? playerBid : "-"}</strong></span>
                  <span>Trick <strong className="text-emerald-400">{playerTricks}</strong></span>
                </span>
                {isCurrentTurn && (
                  <span className="mt-0.5 block animate-pulse text-[9px] font-extrabold tracking-wider text-amber-300 uppercase">
                    {isMe ? "▶ tocca a te" : "▶ turno"}
                  </span>
                )}
              </span>
            </div>
          );

          const slot = showSlot ? (
            played ? (
              <div className="flex origin-center scale-[0.8] flex-col items-center gap-1 sm:scale-100">
                <div className={isWinning ? "rounded-xl ring-2 ring-amber-400 ring-offset-2 ring-offset-emerald-900" : ""}>
                  <GameCardView card={played.card} size="md" isClickable={false} />
                </div>
                {isWinning && (
                  <span className="animate-pulse rounded border border-amber-400/60 bg-amber-500/20 px-1.5 py-0.5 text-[9px] font-black tracking-wider text-amber-300 uppercase">
                    ★ in testa
                  </span>
                )}
              </div>
            ) : (
              <div className="grid h-28 w-20 origin-center scale-[0.8] place-items-center rounded-xl border-2 border-dashed border-white/15 bg-black/25 sm:scale-100">
                <span className="animate-pulse text-lg text-white/25">…</span>
              </div>
            )
          ) : null;

          return (
            <div key={player.id} className={`absolute z-20 ${seat.pos}`}>
              <div className={`flex items-center gap-2 sm:gap-3 ${seat.dir}`}>
                {seat.cardFirst ? (<>{slot}{badge}</>) : (<>{badge}{slot}</>)}
              </div>
            </div>
          );
        })}
      </div>

      {/* 3. MANO DEL GIOCATORE & CONTROLLI D'AZIONE (Schermo In Basso) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
        {/* Mano del Giocatore (2 Colonne) */}
        <div className="lg:col-span-2">
          <PlayerHand
            hand={gameState.hand}
            selectedCard={selectedCard}
            canPlay={canPlay}
            isSubmitting={isSubmitting}
            isCardPlayable={isCardPlayable}
            onSelectCard={setSelectedCard}
            dropZoneRef={tableRef}
            onDropCard={handleDropCard}
            onDragStateChange={setIsCardDragging}
          />
        </div>

        {/* Controlli Azione Diretti (1 Colonna) */}
        <div>
          <GameActionControls
            isMyTurn={isMyTurn}
            canChooseTrump={canChooseTrump}
            canBid={canBid}
            canPlay={canPlay}
            round={gameState.round}
            selectedColor={selectedColor}
            onSelectColor={setSelectedColor}
            onChooseTrump={handleChooseTrump}
            bidInput={bidInput}
            onSelectBid={setBidInput}
            onPlaceBid={handlePlaceBid}
            isSubmitting={isSubmitting}
          />
        </div>
      </div>

      {/* 4. PANNELLI SECONDARI E TOOL UTILI */}
      <div className="space-y-6 pt-4 border-t border-zinc-800">
        <GameScoreboard
          scoreboard={gameState.scoreboard}
          playersMap={playersMap}
          myPlayerId={playerId}
        />

        <div className="flex gap-2 items-center p-4 bg-zinc-900 border border-dashed border-amber-500/50 rounded-xl">
          <span className="text-xs font-mono text-amber-400">DEV TOOL:</span>
          <Button
            size="sm"
            variant="outline"
            className="border-amber-500 text-amber-400 hover:bg-amber-500/20"
            onClick={() => setForceGameEnded((prev) => !prev)}
          >
            {forceGameEnded ? "Disattiva Modal fine partita" : "⚡ Simula Modal GAME_ENDED"}
          </Button>
        </div>
      </div>
    </div>
  );
}