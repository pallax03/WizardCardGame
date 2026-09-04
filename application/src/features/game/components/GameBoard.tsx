"use client";

import { useState } from "react";
import { Badge } from "@/ui/components/badge";
import { Button } from "@/ui/components/button";
import { Card as UiCard, CardContent, CardHeader, CardTitle } from "@/ui/components/card";
import { useGameBoard } from "../hooks/useGameBoard";
import { GameCardView } from "./GameCardView";
import { cardEquals, cardToString } from "../state/gameReducer";
import type { CardColor } from "../types";

interface GameBoardProps {
  customPlayerId?: number;
}

const TRUMP_COLORS: CardColor[] = ["Red", "Yellow", "Green", "Blue"];

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

  const [showEventLog, setShowEventLog] = useState<boolean>(false);
  const [showManualTester, setShowManualTester] = useState<boolean>(true);

  const playersList = lobby?.players ?? [];

  return (
    <div className="w-full max-w-6xl mx-auto space-y-6 pb-16">
      {/* 1. Header & Status Bar */}
      <div className="flex flex-wrap items-center justify-between gap-4 p-4 rounded-2xl bg-zinc-900/80 border border-zinc-800 shadow-lg">
        <div className="flex flex-wrap items-center gap-3">
          <h1 className="text-xl font-bold tracking-tight text-white">
            Wizard Game Board
          </h1>
          <Badge variant="outline" className="text-xs bg-zinc-800 border-zinc-700">
            Lobby: <span className="font-mono ml-1 text-indigo-400">{lobbyId}</span>
          </Badge>
          <Badge variant="outline" className="text-xs bg-zinc-800 border-zinc-700">
            Player: <span className="font-mono ml-1 text-emerald-400">#{playerId}</span>
          </Badge>
          <Badge
            variant={connectionState === "open" ? "default" : "destructive"}
            className="text-xs"
          >
            WS: {connectionState}
          </Badge>
        </div>

        <div className="flex items-center gap-2">
          <Badge variant="secondary" className="text-xs px-3 py-1 font-semibold">
            Round: {gameState.round}
          </Badge>
          <Badge
            variant="default"
            className="text-xs px-3 py-1 font-semibold bg-indigo-600/80"
          >
            Phase: {gameState.status}
          </Badge>
        </div>
      </div>

      {/* 2. Main Turn Banner */}
      <div
        className={`p-4 rounded-2xl border text-center transition-all ${
          isMyTurn
            ? "bg-indigo-950/50 border-indigo-500/80 shadow-indigo-900/20 shadow-lg animate-pulse"
            : "bg-zinc-900/60 border-zinc-800 text-zinc-300"
        }`}
      >
        <p className="text-xs uppercase tracking-widest text-zinc-400 font-semibold mb-1">
          Turn Status
        </p>
        <p className="text-lg font-bold text-white">{turnPrompt}</p>
        {gameState.lastError && (
          <p className="mt-2 text-sm text-rose-400 font-medium bg-rose-950/40 py-1 px-3 rounded-lg inline-block border border-rose-800/40">
            {gameState.lastError}
          </p>
        )}
        {actionStatus && (
          <p className="mt-2 text-xs text-amber-300 font-mono">
            {actionStatus}
          </p>
        )}
      </div>

      {/* 3. Grid: Trump Info + Players Overview */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Trump Card Area */}
        <UiCard className="bg-zinc-900/60 border-zinc-800">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-semibold uppercase tracking-wider text-zinc-400">
              Trump Card
            </CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col items-center justify-center gap-3">
            {gameState.trump ? (
              <div className="flex flex-col items-center gap-2">
                {"card" in gameState.trump && gameState.trump.card && (
                  <GameCardView card={gameState.trump.card} size="sm" isClickable={false} />
                )}
                <div className="text-xs text-center text-zinc-300">
                  <p className="font-semibold">{gameState.trump.type}</p>
                  {gameState.effectiveTrumpColor && (
                    <span
                      className={`inline-block mt-1 px-2.5 py-0.5 rounded font-bold text-xs ${
                        gameState.effectiveTrumpColor === "Red"
                          ? "bg-rose-900 text-rose-200 border border-rose-700"
                          : gameState.effectiveTrumpColor === "Blue"
                          ? "bg-blue-900 text-blue-200 border border-blue-700"
                          : gameState.effectiveTrumpColor === "Green"
                          ? "bg-emerald-900 text-emerald-200 border border-emerald-700"
                          : "bg-amber-900 text-amber-200 border border-amber-700"
                      }`}
                    >
                      Trump: {gameState.effectiveTrumpColor}
                    </span>
                  )}
                </div>
              </div>
            ) : (
              <p className="text-zinc-500 text-sm italic py-4">No trump dealt yet</p>
            )}
          </CardContent>
        </UiCard>

        {/* Players & Bids Overview */}
        <UiCard className="bg-zinc-900/60 border-zinc-800 md:col-span-2">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-semibold uppercase tracking-wider text-zinc-400">
              Players & Round Status
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              {playersList.map((player) => {
                const isCurrentTurn = gameState.currentTurn.playerId === player.id;
                const isMe = player.id === playerId;
                const playerBid = gameState.bids[player.id];
                const tricksWon = gameState.tricksWon[player.id] ?? 0;
                const isBot = Boolean(player.difficulty);

                return (
                  <div
                    key={player.id}
                    className={`p-3 rounded-xl border transition-all ${
                      isCurrentTurn
                        ? "bg-indigo-950/40 border-indigo-500 ring-1 ring-indigo-500"
                        : "bg-zinc-950/40 border-zinc-800"
                    }`}
                  >
                    <div className="flex items-center justify-between text-xs mb-1.5">
                      <span className="font-bold text-white truncate max-w-[90px]">
                        {player.name} {isMe && "(You)"}
                      </span>
                      {isBot && (
                        <span className="text-[10px] bg-zinc-800 text-zinc-400 px-1 rounded">
                          Bot
                        </span>
                      )}
                    </div>

                    <div className="flex items-center justify-between text-xs text-zinc-400 font-mono">
                      <span>Bid: {playerBid !== undefined ? playerBid : "-"}</span>
                      <span>Tricks: {tricksWon}</span>
                    </div>

                    {isCurrentTurn && (
                      <div className="mt-1.5 text-[10px] font-semibold text-indigo-400 animate-pulse uppercase">
                        Current Turn
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </CardContent>
        </UiCard>
      </div>

      {/* 4. Table / Trick in Progress */}
      <UiCard className="bg-zinc-900/60 border-zinc-800">
        <CardHeader className="pb-2 flex flex-row items-center justify-between">
          <CardTitle className="text-sm font-semibold uppercase tracking-wider text-zinc-400">
            Trick Table
          </CardTitle>
          {gameState.followingColor && (
            <Badge variant="outline" className="text-xs">
              Lead Color: {gameState.followingColor}
            </Badge>
          )}
        </CardHeader>
        <CardContent>
          {gameState.table.length > 0 ? (
            <div className="flex flex-wrap gap-4 items-center justify-center min-h-36 p-4 bg-zinc-950/40 rounded-xl border border-zinc-800/80">
              {gameState.table.map((entry, index) => {
                const playerName =
                  playersMap.get(entry.playerId)?.name ?? `Player ${entry.playerId}`;
                const isWinning =
                  gameState.winningCard && cardEquals(entry.card, gameState.winningCard);

                return (
                  <div key={index} className="flex flex-col items-center gap-1.5">
                    <span className="text-xs font-semibold text-zinc-300">
                      {playerName} {entry.playerId === playerId && "(You)"}
                    </span>
                    <GameCardView card={entry.card} size="md" isClickable={false} />
                    {isWinning && (
                      <span className="text-[10px] bg-amber-500/20 text-amber-300 border border-amber-500/40 px-1.5 py-0.5 rounded font-bold">
                        Winning Card
                      </span>
                    )}
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center py-10 bg-zinc-950/20 rounded-xl border border-dashed border-zinc-800 text-zinc-500 text-sm">
              <span>No cards played in this trick yet</span>
              {gameState.lastTrick && (
                <div className="mt-2 text-xs text-zinc-400 text-center">
                  <span>
                    Last trick won by{" "}
                    <strong>
                      {playersMap.get(gameState.lastTrick.winnerId)?.name ??
                        `Player ${gameState.lastTrick.winnerId}`}
                    </strong>{" "}
                    ({gameState.lastTrick.cards.map(cardToString).join(", ")})
                  </span>
                </div>
              )}
            </div>
          )}
        </CardContent>
      </UiCard>

      {/* 5. Player's Hand */}
      <UiCard className="bg-zinc-900/60 border-zinc-800">
        <CardHeader className="pb-2 flex flex-row items-center justify-between">
          <CardTitle className="text-sm font-semibold uppercase tracking-wider text-zinc-400">
            Your Hand ({gameState.hand.length} cards)
          </CardTitle>
          {canPlay && (
            <span className="text-xs text-indigo-400 font-semibold animate-pulse">
              Select a card to play
            </span>
          )}
        </CardHeader>
        <CardContent>
          {gameState.hand.length > 0 ? (
            <div className="flex flex-wrap gap-3 items-center justify-center p-4 bg-zinc-950/40 rounded-xl border border-zinc-800/80 min-h-36">
              {gameState.hand.map((card, index) => {
                const isSelected = cardEquals(card, selectedCard);
                const isLegal = isCardPlayable(card);

                return (
                  <GameCardView
                    key={index}
                    card={card}
                    size="lg"
                    isSelected={isSelected}
                    isLegal={canPlay ? isLegal : true}
                    isClickable={canPlay && isLegal}
                    onClick={() => {
                      if (canPlay && isLegal) {
                        setSelectedCard(isSelected ? null : card);
                      }
                    }}
                  />
                );
              })}
            </div>
          ) : (
            <div className="text-center py-8 text-zinc-500 text-sm">
              Your hand is empty (waiting for next round cards)
            </div>
          )}
        </CardContent>
      </UiCard>

      {/* 6. Contextual Action Panel (Shows appropriate UI when it's your turn) */}
      <UiCard className="bg-zinc-900/80 border-indigo-500/30">
        <CardHeader className="pb-2">
          <CardTitle className="text-sm font-semibold uppercase tracking-wider text-zinc-300">
            Current Action Controls
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* A. Choose Trump Color */}
          {canChooseTrump && (
            <div className="p-4 rounded-xl bg-indigo-950/40 border border-indigo-500/50 space-y-3">
              <p className="text-sm font-semibold text-white">
                Choose Trump Color:
              </p>
              <div className="flex flex-wrap gap-2">
                {TRUMP_COLORS.map((color) => (
                  <Button
                    key={color}
                    size="lg"
                    variant={selectedColor === color ? "confirming" : "outline"}
                    disabled={isSubmitting}
                    onClick={() => {
                      setSelectedColor(color);
                      void handleChooseTrump(color);
                    }}
                  >
                    {color}
                  </Button>
                ))}
              </div>
            </div>
          )}

          {/* B. Place Bid */}
          {canBid && (
            <div className="p-4 rounded-xl bg-indigo-950/40 border border-indigo-500/50 space-y-3">
              <p className="text-sm font-semibold text-white">
                Place your Bid for Round {gameState.round}:
              </p>
              <div className="flex flex-wrap items-center gap-2">
                {Array.from({ length: gameState.round + 1 }, (_, i) => (
                  <Button
                    key={i}
                    size="sm"
                    variant={bidInput === i ? "confirming" : "outline"}
                    disabled={isSubmitting}
                    onClick={() => setBidInput(i)}
                  >
                    {i}
                  </Button>
                ))}
                <Button
                  size="default"
                  variant="primary"
                  disabled={isSubmitting}
                  onClick={() => void handlePlaceBid(bidInput)}
                  className="ml-auto"
                >
                  Confirm Bid ({bidInput})
                </Button>
              </div>
            </div>
          )}

          {/* C. Play Card */}
          {canPlay && (
            <div className="p-4 rounded-xl bg-indigo-950/40 border border-indigo-500/50 flex flex-wrap items-center justify-between gap-4">
              <div>
                <p className="text-sm font-semibold text-white">
                  Play Card:
                </p>
                <p className="text-xs text-zinc-400">
                  {selectedCard
                    ? `Selected: ${cardToString(selectedCard)}`
                    : "Click one of the legal cards in your hand above"}
                </p>
              </div>
              <Button
                size="lg"
                variant="primary"
                disabled={!selectedCard || isSubmitting}
                onClick={() => {
                  if (selectedCard) void handlePlayCard(selectedCard);
                }}
              >
                Play Selected Card
              </Button>
            </div>
          )}

          {!isMyTurn && (
            <p className="text-xs text-zinc-500 italic">
              Controls will activate when it is your turn.
            </p>
          )}
        </CardContent>
      </UiCard>

      {/* 7. Direct API / Manual Tester (Original 3 Buttons + Edge Case Testing) */}
      <UiCard className="bg-zinc-900/60 border-zinc-800">
        <CardHeader
          className="pb-2 cursor-pointer flex flex-row items-center justify-between"
          onClick={() => setShowManualTester((prev) => !prev)}
        >
          <CardTitle className="text-sm font-semibold uppercase tracking-wider text-zinc-400">
            Direct API Tester / Manual Buttons {showManualTester ? "▲" : "▼"}
          </CardTitle>
          <span className="text-xs text-zinc-500">
            Use to test backend calls directly
          </span>
        </CardHeader>
        {showManualTester && (
          <CardContent className="space-y-4 pt-2">
            <p className="text-xs text-zinc-400">
              These buttons trigger direct calls to the backend endpoints (`choose`, `place`, `play`)
              using currently selected values or sensible defaults. Useful for testing invalid moves,
              playing out of turn, or completing moves rapidly.
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              {/* Play Card Button */}
              <div className="p-3 bg-zinc-950/60 border border-zinc-800 rounded-xl space-y-2">
                <span className="text-xs font-semibold text-zinc-300">Play Card</span>
                <p className="text-[11px] text-zinc-500">
                  Card: {selectedCard ? cardToString(selectedCard) : "Blue 7 (Default)"}
                </p>
                <Button
                  variant="primary"
                  size="default"
                  className="w-full"
                  disabled={isSubmitting}
                  onClick={() =>
                    handlePlayCard(
                      selectedCard ?? { type: "Standard", color: "Blue", rank: 7 }
                    )
                  }
                >
                  Play Card
                </Button>
              </div>

              {/* Place Bid Button */}
              <div className="p-3 bg-zinc-950/60 border border-zinc-800 rounded-xl space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-zinc-300">Place Bid</span>
                  <input
                    type="number"
                    min={0}
                    max={20}
                    value={bidInput}
                    onChange={(e) => setBidInput(Math.max(0, Number(e.target.value)))}
                    className="w-14 bg-zinc-900 border border-zinc-700 rounded px-1.5 py-0.5 text-xs text-white text-right font-mono"
                  />
                </div>
                <p className="text-[11px] text-zinc-500">Bid amount: {bidInput}</p>
                <Button
                  variant="primary"
                  size="default"
                  className="w-full"
                  disabled={isSubmitting}
                  onClick={() => handlePlaceBid(bidInput)}
                >
                  Place Bid
                </Button>
              </div>

              {/* Choose Trump Color Button */}
              <div className="p-3 bg-zinc-950/60 border border-zinc-800 rounded-xl space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-zinc-300">Choose Trump</span>
                  <select
                    value={selectedColor}
                    onChange={(e) => setSelectedColor(e.target.value as CardColor)}
                    className="bg-zinc-900 border border-zinc-700 rounded px-1.5 py-0.5 text-xs text-white"
                  >
                    {TRUMP_COLORS.map((c) => (
                      <option key={c} value={c}>
                        {c}
                      </option>
                    ))}
                  </select>
                </div>
                <p className="text-[11px] text-zinc-500">Color: {selectedColor}</p>
                <Button
                  variant="primary"
                  size="default"
                  className="w-full"
                  disabled={isSubmitting}
                  onClick={() => handleChooseTrump(selectedColor)}
                >
                  Choose Trump Color
                </Button>
              </div>
            </div>
          </CardContent>
        )}
      </UiCard>

      {/* 8. Scoreboard (if round scored or game finished) */}
      {gameState.scoreboard && (
        <UiCard className="bg-zinc-900/60 border-zinc-800">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-semibold uppercase tracking-wider text-zinc-400">
              Scoreboard
            </CardTitle>
          </CardHeader>
          <CardContent className="overflow-x-auto">
            <table className="w-full text-left text-xs font-mono">
              <thead>
                <tr className="border-b border-zinc-800 text-zinc-400">
                  <th className="py-2 px-3">Player</th>
                  <th className="py-2 px-3">Round</th>
                  <th className="py-2 px-3">Bid</th>
                  <th className="py-2 px-3">Score</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/60">
                {Object.entries(gameState.scoreboard).map(([pIdStr, entries]) => {
                  const pId = Number(pIdStr);
                  const pName = playersMap.get(pId)?.name ?? `Player ${pId}`;
                  const lastEntry = entries[entries.length - 1];

                  return (
                    <tr key={pIdStr} className={pId === playerId ? "bg-indigo-950/20 font-bold" : ""}>
                      <td className="py-2 px-3 text-white">
                        {pName} {pId === playerId && "(You)"}
                      </td>
                      <td className="py-2 px-3 text-zinc-400">{lastEntry?.round ?? "-"}</td>
                      <td className="py-2 px-3 text-zinc-400">{lastEntry?.bid ?? "-"}</td>
                      <td className="py-2 px-3 text-emerald-400">{lastEntry?.score ?? "-"}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </CardContent>
        </UiCard>
      )}

      {/* 9. Real-time Event Log (Live EventMessage Inspector) */}
      <UiCard className="bg-zinc-900/60 border-zinc-800">
        <CardHeader
          className="pb-2 cursor-pointer flex flex-row items-center justify-between"
          onClick={() => setShowEventLog((prev) => !prev)}
        >
          <div className="flex items-center gap-2">
            <CardTitle className="text-sm font-semibold uppercase tracking-wider text-zinc-400">
              Received Event Messages ({gameEvents.length}) {showEventLog ? "▲" : "▼"}
            </CardTitle>
            <span className="text-[10px] bg-zinc-800 text-zinc-400 px-1.5 py-0.5 rounded">
              WebSocket stream
            </span>
          </div>
          <span className="text-xs text-indigo-400">
            {showEventLog ? "Hide Log" : "Inspect Events"}
          </span>
        </CardHeader>
        {showEventLog && (
          <CardContent className="space-y-2 pt-2">
            <p className="text-xs text-zinc-400">
              Displays all `EventMessage` received over the WebSocket in real-time.
              Click to view event details.
            </p>
            <div className="max-h-72 overflow-y-auto space-y-2 p-3 bg-zinc-950 rounded-xl border border-zinc-800/80 font-mono text-xs">
              {gameEvents.length === 0 ? (
                <p className="text-zinc-500 italic">No events received yet</p>
              ) : (
                gameEvents
                  .slice()
                  .reverse()
                  .map((ev, index) => (
                    <div
                      key={index}
                      className="p-2 rounded bg-zinc-900/80 border border-zinc-800 flex flex-col gap-1"
                    >
                      <div className="flex items-center justify-between text-zinc-400 text-[11px]">
                        <span className="font-bold text-indigo-300">
                          {ev.event.type} &rarr; {ev.event.action}
                        </span>
                        <span className="text-zinc-500">
                          {new Date(ev.timestamp).toLocaleTimeString()}
                        </span>
                      </div>
                      <pre className="text-[10px] text-zinc-300 overflow-x-auto whitespace-pre-wrap">
                        {JSON.stringify(ev.event, null, 2)}
                      </pre>
                    </div>
                  ))
              )}
            </div>
          </CardContent>
        )}
      </UiCard>
    </div>
  );
}
