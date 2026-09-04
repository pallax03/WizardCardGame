"use client";

import { useGameBoard } from "../hooks/useGameBoard";
import { GameActionControls } from "./GameActionControls";
import { GameEventLog } from "./GameEventLog";
import { GameHeader } from "./GameHeader";
import { GameScoreboard } from "./GameScoreboard";
import { GameTurnBanner } from "./GameTurnBanner";
import { ManualApiTester } from "./ManualApiTester";
import { PlayerHand } from "./PlayerHand";
import { PlayersRoundStatus } from "./PlayersRoundStatus";
import { TrickTable } from "./TrickTable";
import { TrumpArea } from "./TrumpArea";

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
    handleChooseTrump,
    handlePlaceBid,
    handlePlayCard,
  } = useGameBoard(customPlayerId);

  return (
    <div className="w-full max-w-6xl mx-auto space-y-6 pb-16">
      {/* 1. Header & Status Bar */}
      <GameHeader
        lobbyId={lobbyId}
        playerId={playerId}
        connectionState={connectionState}
        round={gameState.round}
        status={gameState.status}
      />

      {/* 2. Main Turn Banner */}
      <GameTurnBanner
        isMyTurn={isMyTurn}
        turnPrompt={turnPrompt}
        lastError={gameState.lastError}
        actionStatus={actionStatus}
      />

      {/* 3. Grid: Trump Info + Players Overview */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <TrumpArea
          trump={gameState.trump}
          effectiveTrumpColor={gameState.effectiveTrumpColor}
        />
        <PlayersRoundStatus
          players={lobby?.players ?? []}
          currentTurnPlayerId={gameState.currentTurn.playerId}
          myPlayerId={playerId}
          bids={gameState.bids}
          tricksWon={gameState.tricksWon}
        />
      </div>

      {/* 4. Table / Trick in Progress */}
      <TrickTable
        table={gameState.table}
        playersMap={playersMap}
        myPlayerId={playerId}
        winningCard={gameState.winningCard}
        followingColor={gameState.followingColor}
        lastTrick={gameState.lastTrick}
      />

      {/* 5. Player's Hand */}
      <PlayerHand
        hand={gameState.hand}
        selectedCard={selectedCard}
        canPlay={canPlay}
        isCardPlayable={isCardPlayable}
        onSelectCard={setSelectedCard}
      />

      {/* 6. Contextual Action Panel */}
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

      {/* 7. Direct API Tester / Manual Buttons */}
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

      {/* 8. Scoreboard */}
      <GameScoreboard
        scoreboard={gameState.scoreboard}
        playersMap={playersMap}
        myPlayerId={playerId}
      />

      {/* 9. Real-time Event Log */}
      <GameEventLog gameEvents={gameEvents} />
    </div>
  );
}
