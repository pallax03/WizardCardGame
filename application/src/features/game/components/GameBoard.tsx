"use client";

import { useGameBoard } from "../hooks/useGameBoard";
import { GameActionControls } from "./GameActionControls";
import { GameEventLog } from "./GameEventLog";
import { GameHeader } from "./GameHeader";
import { GameScoreboard } from "./GameScoreboard";
import { GameTurnBanner } from "./GameTurnBanner";
import { ManualApiTester } from "./ManualApiTester";
import { PlayerHand } from "./PlayerHand";
import { TrickTable } from "./TrickTable";
import { TrumpArea } from "./TrumpArea";
import { Badge } from "@/ui/components/badge";
import { Card as UiCard, CardContent } from "@/ui/components/card";

interface GameBoardProps {
  customPlayerId?: number;
}

export function GameBoard({ customPlayerId }: GameBoardProps) {
  const {
    lobbyId,
    playerId,
    connectionState,
    lobby,
    playersMap,
    gameState,
    gameEvents,
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
  // Ordiniamo i player partendo dal client locale (in basso / Sud)
  const myIndex = players.findIndex((p) => p.id === playerId);
  const orderedPlayers =
    myIndex !== -1
      ? [...players.slice(myIndex), ...players.slice(0, myIndex)]
      : players;

  // Stili per disporre i player attorno al tavolo ovale
  const positionStyles = [
    "bottom-[-20px] left-1/2 -translate-x-1/2 z-20", // Sud (You)
    "top-1/2 left-[-15px] -translate-y-1/2 z-10",     // Ovest
    "top-[-20px] left-1/2 -translate-x-1/2 z-10",    // Nord
    "top-1/2 right-[-15px] -translate-y-1/2 z-10",    // Est
    "top-8 left-8 z-10",                             // Nord-Ovest (in caso di 5+ giocatori)
    "top-8 right-8 z-10",                            // Nord-Est (in caso di 6+ giocatori)
  ];

  return (
    <div className="w-full max-w-7xl mx-auto space-y-6 pb-20 px-2 sm:px-4">
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

        <ManualApiTester
          selectedCard={selectedCard}
          onPlayCard={handlePlayCard}
          bidInput={bidInput}
          onSetBidInput={setBidInput}
          onPlaceBid={handlePlaceBid}
          selectedColor={selectedColor}
          onSetSelectedColor={setSelectedColor}
          onChooseTrump={handleChooseTrump}
          isSubmitting={isSubmitting}
        />

        <GameEventLog gameEvents={gameEvents} />
      </div>
    </div>
  );
}