"use client";

import { useRouter } from "next/navigation";
import { useGameBoard } from "../hooks/useGameBoard";
import { GameActionControls } from "./GameActionControls";
import { GameHeader } from "./GameHeader";
import { GameScoreboard } from "./GameScoreboard";
import { GameTurnBanner } from "./GameTurnBanner";
import { PlayerHand } from "./PlayerHand";
import { TrickTable } from "./TrickTable";
import { TrumpArea } from "./TrumpArea";
import { Badge } from "@/ui/components/badge";
import { Button } from "@/ui/components/button";
import { Card as UiCard, CardContent, CardHeader, CardTitle, CardDescription } from "@/ui/components/card";
import { useState } from "react";

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

  // Mappatura posizioni radiale al tavolo stile Texas Hold'em
  const myIndex = players.findIndex((p) => p.id === playerId);
  const orderedPlayers =
    myIndex !== -1
      ? [...players.slice(myIndex), ...players.slice(0, myIndex)]
      : players;

  const positionStyles = [
    "bottom-[-20px] left-1/2 -translate-x-1/2 z-20", // Sud (You)
    "top-1/2 left-[-15px] -translate-y-1/2 z-10",     // Ovest
    "top-[-20px] left-1/2 -translate-x-1/2 z-10",    // Nord
    "top-1/2 right-[-15px] -translate-y-1/2 z-10",    // Est
    "top-8 left-8 z-10",                             // Nord-Ovest
    "top-8 right-8 z-10",                            // Nord-Est
  ];

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

      {/* 2. TAVOLO DA GIOCO TEXAS HOLD'EM (Poker Table Felt) */}
      <div className="relative w-full my-8 py-10 px-4 min-h-[580px] rounded-[120px] sm:rounded-[180px] bg-gradient-to-b from-emerald-900 via-emerald-800 to-emerald-950 border-[12px] border-amber-950/80 shadow-[inset_0_0_80px_rgba(0,0,0,0.8),0_20px_50px_rgba(0,0,0,0.6)] flex flex-col items-center justify-between overflow-hidden">
        
        {/* Linea interna decorativa del feltro */}
        <div className="absolute inset-4 rounded-[100px] sm:rounded-[160px] border-2 border-emerald-600/30 pointer-events-none flex items-center justify-center">
          <span className="text-emerald-900/20 text-6xl sm:text-8xl font-black uppercase tracking-widest select-none">
            WIZARD
          </span>
        </div>

        {/* --- GIOCATORI SEDUTI INTORNO AL TAVOLO --- */}
        {orderedPlayers.map((player, idx) => {
          const isCurrentTurn = gameState.currentTurn.playerId === player.id;
          const isMe = player.id === playerId;
          const playerBid = gameState.bids[player.id];
          const playerTricks = gameState.tricksWon[player.id] ?? 0;
          const isBot = Boolean(player.difficulty);
          const posClass = positionStyles[idx % positionStyles.length];

          return (
            <div key={player.id} className={`absolute ${posClass}`}>
              <UiCard
                className={`w-36 sm:w-44 transition-all duration-300 shadow-xl backdrop-blur-md ${
                  isCurrentTurn
                    ? "bg-amber-500/20 border-amber-400 ring-4 ring-amber-400/50 scale-105"
                    : "bg-zinc-900/90 border-zinc-700/80"
                }`}
              >
                <CardContent className="p-2 sm:p-3 text-center">
                  <div className="flex items-center justify-center gap-1 mb-1">
                    <span className="font-bold text-xs sm:text-sm text-white truncate max-w-[90px]">
                      {player.name}
                    </span>
                    {isMe && <Badge variant="secondary" className="text-[9px] px-1 py-0">TU</Badge>}
                    {isBot && <Badge variant="outline" className="text-[9px] px-1 py-0 text-zinc-400">BOT</Badge>}
                  </div>

                  <div className="flex justify-around items-center text-[11px] font-mono mt-1 pt-1 border-t border-zinc-800 text-zinc-300">
                    <div className="flex flex-col">
                      <span className="text-[9px] text-zinc-500 uppercase">Bid</span>
                      <span className="font-bold text-amber-400">{playerBid !== undefined ? playerBid : "-"}</span>
                    </div>
                    <div className="flex flex-col">
                      <span className="text-[9px] text-zinc-500 uppercase">Tricks</span>
                      <span className="font-bold text-emerald-400">{playerTricks}</span>
                    </div>
                  </div>

                  {isCurrentTurn && (
                    <div className="mt-1 text-[9px] font-extrabold text-amber-300 animate-pulse tracking-wider uppercase">
                      Turno Attivo
                    </div>
                  )}
                </CardContent>
              </UiCard>
            </div>
          );
        })}

        {/* --- CENTRO TAVOLO (Carte Giocate, Trump & Stato Turno) --- */}
        <div className="relative z-10 my-auto w-full max-w-3xl flex flex-col items-center gap-4">
          
          {/* Banner con lo stato del Turno */}
          <div className="w-full max-w-md">
            <GameTurnBanner
              isMyTurn={isMyTurn}
              turnPrompt={turnPrompt}
              lastError={gameState.lastError}
              actionStatus={actionStatus}
            />
          </div>

          <div className="flex flex-col md:flex-row items-center justify-center gap-4 w-full">
            {/* Area Briscola / Trump */}
            <div className="w-full md:w-auto min-w-[140px]">
              <TrumpArea
                trump={gameState.trump}
                effectiveTrumpColor={gameState.effectiveTrumpColor}
              />
            </div>

            {/* Tavolo delle Carte giocate nel Trick */}
            <div className="flex-1 w-full">
              <TrickTable
                table={gameState.table}
                playersMap={playersMap}
                myPlayerId={playerId}
                winningCard={gameState.winningCard}
                followingColor={gameState.followingColor}
                lastTrick={gameState.lastTrick}
              />
            </div>
          </div>
        </div>
      </div>

      {/* 3. MANO DEL GIOCATORE & CONTROLLI D'AZIONE (Schermo In Basso) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
        {/* Mano del Giocatore (2 Colonne) */}
        <div className="lg:col-span-2">
          <PlayerHand
            hand={gameState.hand}
            selectedCard={selectedCard}
            canPlay={canPlay}
            isCardPlayable={isCardPlayable}
            onSelectCard={setSelectedCard}
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
            selectedCard={selectedCard}
            onPlayCard={handlePlayCard}
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