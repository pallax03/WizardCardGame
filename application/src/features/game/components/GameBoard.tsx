"use client";

import { useMemo, useRef, useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Image from "next/image";
import { useGameBoard, TRICK_REVEAL_SECONDS } from "../hooks/useGameBoard";
import { cardEquals } from "../state/gameReducer";
import type { Card, CardColor } from "../types";
import { GameCardView } from "./GameCardView";
import { GameScoreboard } from "./GameScoreboard";
import { PlayerHand } from "./PlayerHand";
import { GameEndOverlay } from "./GameEndOverlay";
import { DisconnectOverlay } from "./DisconnectOverlay";
import { Badge } from "@/ui/components/badge";
import { Button } from "@/ui/components/button";
import { ArrowLeft, Lightbulb, Trophy, Target, CheckSquare, MessageCircle, AlertTriangle, Check, X } from "lucide-react";
import { t } from "@/ui/i18n/core";

const lobbyI18n = t("lobby");

function phaseLabel(status: string): string {
  const s = lobbyI18n.pausedSummary;
  switch (status) {
    case "CHOOSING_TRUMP": return s.phaseChoosingTrump;
    case "BIDDING": return s.phaseBidding;
    case "PLAYING": return s.phasePlaying;
    case "ROUND_SCORED": return s.phaseRoundScored;
    case "GAME_ENDED": return s.phaseEnded;
    default: return s.phaseWaiting;
  }
}

const getEffectiveColorCircle = (color: string) => {
  switch (color) {
    case "RED": return "bg-red-500";
    case "BLUE": return "bg-blue-500";
    case "GREEN": return "bg-emerald-500";
    case "YELLOW": return "bg-amber-500";
    default: return "";
  }
};

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
    turnTimerSeconds, turnTimerDuration,
    isCardPlayable,
    forbiddenBid,
    bidsTotal,
    bidWarning,
    bidError,
    actionError,
    lobbyWarning,
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
    hintedCard,
    hintedBid,
    isHintLoading,
    hintError,
    canRequestHint,
    requestHint,
  } = useGameBoard(customPlayerId);

  const [isCardDragging, setIsCardDragging] = useState(false);
  const [isReturning, setIsReturning] = useState(false);
  const [isPausing, setIsPausing] = useState(false);
  const [pauseError, setPauseError] = useState<string | null>(null);
  const [showScoreboard, setShowScoreboard] = useState(false);
  const [scoreboardPlayer, setScoreboardPlayer] = useState<number | undefined>(undefined);
  const [unreadChatCount, setUnreadChatCount] = useState(0);
  const tableRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const handler = (e: Event) => {
      const customEvent = e as CustomEvent<{ unreadTotal: number }>;
      setUnreadChatCount(customEvent.detail.unreadTotal);
    };
    window.addEventListener('chat-unread-change', handler);
    return () => window.removeEventListener('chat-unread-change', handler);
  }, []);

  const players = lobby?.players ?? [];
  const myIndex = players.findIndex((p) => p.id === playerId);
  const orderedPlayers = useMemo(
    () => (myIndex !== -1 ? [...players.slice(myIndex), ...players.slice(0, myIndex)] : players),
    [players, myIndex]
  );

  const isGameEnded = gameState.status === "GAME_ENDED";
  const isDisconnecting = lobby?.status === "DISCONNECTING";

  const sortedScoreboard = useMemo(() => {
    if (!gameState.scoreboard) return [];
    return Object.entries(gameState.scoreboard)
      .map(([pIdStr, entries]) => {
        const pId = Number(pIdStr);
        const playerObj = playersMap.get(pId);
        const playerName =
          typeof playerObj === "object" && playerObj !== null
            ? playerObj.name
            : playerObj ?? `Giocatore ${pId}`;

        let finalScore = 0;
        if (Array.isArray(entries) && entries.length > 0) {
          finalScore = Number(entries[entries.length - 1]?.score ?? 0);
        } else if (typeof entries === "number") {
          finalScore = entries;
        } else if (entries && typeof entries === "object" && "score" in entries) {
          finalScore = Number((entries as { score: unknown }).score ?? 0);
        }

        return { id: pId, name: playerName, score: Number.isNaN(finalScore) ? 0 : finalScore };
      })
      .sort((a, b) => b.score - a.score);
  }, [gameState.scoreboard, playersMap]);

  const handleReturnToLobby = async () => {
    if (isReturning) return;
    setIsReturning(true);
    try {
      const { discardPausedGameAction } = await import("@/features/lobby/api");
      await discardPausedGameAction(lobbyId);
    } catch {}
    router.push(lobbyId ? `/lobby/${lobbyId}` : "/");
  };

  const handleBackToLobby = async () => {
    if (isPausing || !lobbyId) return;
    setPauseError(null);
    if (lobby?.status === "IN_GAME" || lobby?.status === "DISCONNECTING") {
      setIsPausing(true);
      try {
        const { pauseGameAction } = await import("@/features/lobby/api");
        const { getErrorMessage } = await import("@/ui/i18n/errors");
        const result = await pauseGameAction(lobbyId);
        if (result.error) {
          setPauseError(getErrorMessage(result.error));
          setIsPausing(false);
          return;
        }
      } catch {
        setPauseError("Impossibile mettere in pausa la partita.");
        setIsPausing(false);
        return;
      }
      setIsPausing(false);
    }
    router.push(`/lobby/${lobbyId}`);
  };

  const offlineNames = useMemo(() => {
    const names = (lobby?.players ?? [])
      .filter((p) => !p.difficulty && p.isOnline === false)
      .map((p) => p.name);
    return names.length > 0 ? names.join(", ") : "Un giocatore";
  }, [lobby?.players]);
  const disconnectTimerSeconds = lobby?.configuration?.timer ?? 30;

  const handleDropCard = (card: Card) => {
    setSelectedCard(null);
    void handlePlayCard(card);
  };

  const effectiveTrumpColorClass = gameState.effectiveTrumpColor ? getEffectiveColorCircle(gameState.effectiveTrumpColor) : "";

  const openChat = () => {
    window.dispatchEvent(new CustomEvent('open-chat-global'));
  };

  return (
    <div className="relative flex flex-col min-h-[100dvh] w-full max-w-md sm:max-w-4xl lg:max-w-6xl mx-auto px-2 pb-6 space-y-3 sm:space-y-4 select-none overflow-hidden">
      <GameEndOverlay
        isGameEnded={isGameEnded}
        sortedScoreboard={sortedScoreboard}
        playerId={playerId}
        onReturnToLobby={() => void handleReturnToLobby()}
        isReturning={isReturning}
      />

      {isDisconnecting && (
        <DisconnectOverlay
          key={`${lobbyId}-${offlineNames}`}
          timerSeconds={disconnectTimerSeconds}
          offlineNames={offlineNames}
          isWorking={isPausing}
          onBackToLobby={() => void handleBackToLobby()}
        />
      )}

      {pauseError && (
        <div className="absolute top-16 left-1/2 -translate-x-1/2 z-50 p-2 rounded-xl border border-rose-700/60 bg-rose-950/90 text-rose-200 text-xs text-center shadow-xl backdrop-blur-md w-[90%] max-w-sm">
          <p className="font-semibold">⚠️ {pauseError}</p>
        </div>
      )}

      {/* TOP HEADER */}
      <div className="flex items-center justify-between mt-2 px-1">
        <div className="flex flex-1 items-center">
          <Button
            type="button"
            size="sm"
            variant="ghost"
            disabled={isPausing}
            onClick={() => void handleBackToLobby()}
            className="text-zinc-400 hover:text-white px-2 h-8"
          >
            <ArrowLeft className="size-4 mr-1" />
            <span className="text-xs hidden sm:inline">Ritorna alla Lobby</span>
          </Button>
        </div>

        <div className="flex flex-col items-center">
          <span className="text-xs font-bold text-white tracking-widest uppercase">
            {lobbyI18n.pausedSummary.round(revealedTrick ? revealedTrick.round : gameState.round)}
          </span>
          <span className="text-[10px] font-mono text-zinc-400 uppercase tracking-widest">
            {phaseLabel(gameState.status)}
          </span>
        </div>

        <div className="flex flex-1 items-center justify-end gap-2">
          {/* Global Chat Button */}
          <Button
            type="button"
            size="icon"
            variant="ghost"
            onClick={openChat}
            className={`h-8 w-8 rounded-full border transition-colors relative ${
              unreadChatCount > 0
                ? "bg-sky-500/10 border-sky-500/40 text-sky-400 hover:bg-sky-500/20 shadow-[0_0_15px_-3px_rgba(56,189,248,0.4)]"
                : "bg-zinc-900/60 border-zinc-800 text-zinc-400 hover:text-white"
            }`}
          >
            <MessageCircle className="size-4" />
            {unreadChatCount > 0 && (
              <span className="absolute -top-1 -right-1 flex h-3.5 min-w-[14px] items-center justify-center rounded-full bg-sky-500 px-1 text-[8px] font-bold text-white shadow-sm ring-2 ring-zinc-950">
                {unreadChatCount > 9 ? "9+" : unreadChatCount}
              </span>
            )}
          </Button>
        </div>
      </div>

      {/* PLAYER MATRIX TABLE */}
      <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 gap-2 sm:gap-3 w-full">
        {orderedPlayers.map((player) => {
          const isCurrentTurn = gameState.currentTurn.playerId === player.id;
          const isMe = player.id === playerId;
          const sourceBids = revealedTrick ? revealedTrick.bids : gameState.bids;
          const sourceTricks = revealedTrick ? revealedTrick.tricksSnapshot : gameState.tricksWon;
          const hasBid = sourceBids[player.id] !== undefined && sourceBids[player.id] !== null;
          const playerBid = hasBid ? sourceBids[player.id] : "-";
          const playerTricks = hasBid ? (sourceTricks[player.id] ?? 0) : "-";
          
          const bidColor = hasBid ? "text-amber-400 font-bold" : "text-zinc-500";
          const tricksColor = !hasBid 
            ? "text-zinc-500" 
            : ((sourceTricks[player.id] ?? 0) === sourceBids[player.id] ? "text-emerald-400 font-bold" : "text-red-500 font-bold");
          
          const lastScore = gameState.scoreboard?.[String(player.id)]?.at(-1);
          const score = lastScore ? lastScore.score : 0;
          const isBot = Boolean(player.difficulty);

          let timerPct = 0;
          if (isCurrentTurn && turnTimerSeconds !== null && turnTimerSeconds !== undefined) {
             const maxTime = turnTimerDuration ?? lobby?.configuration?.timer ?? 30;
             timerPct = Math.max(0, Math.min(100, (turnTimerSeconds / maxTime) * 100));
          }

          return (
            <button
              type="button"
              key={player.id}
              onClick={() => { setScoreboardPlayer(player.id); setShowScoreboard(true); }}
              className={`relative flex flex-col rounded-xl border transition-all overflow-hidden text-left cursor-pointer active:scale-95 ${
                isCurrentTurn 
                  ? "bg-sky-500/10 border-sky-500/40 ring-1 ring-sky-500/30 shadow-lg shadow-sky-900/20"
                  : isMe
                    ? "bg-zinc-800/80 border-zinc-500/50 hover:bg-zinc-700/80"
                    : "bg-zinc-900/60 border-zinc-800/80 hover:bg-zinc-800/80"
              }`}
            >
              <div className="p-1.5 pb-1 md:p-2 md:pb-1.5 w-full flex flex-col justify-between h-[58px] md:h-[68px]">
                <div className="flex flex-col gap-0 min-w-0 w-full">
                  <div className="flex items-center justify-between min-w-0">
                     <span className={`flex items-center gap-1 text-[10px] md:text-[13px] font-bold truncate ${isCurrentTurn ? 'text-sky-400' : 'text-zinc-100'}`}>
                       {(() => {
                         const isAllZero = sortedScoreboard.every(s => s.score === 0);
                         if (isAllZero) return null;
                         const rank = sortedScoreboard.findIndex(s => s.score === score) + 1;
                         if (rank === 1 && score > 0) {
                           return <Trophy className="size-3 md:size-3.5 text-amber-400 shrink-0" />;
                         }
                         return <span className="text-zinc-500 font-mono text-[9px] md:text-[11px]">{rank}#</span>;
                       })()}
                       <span className="truncate">{player.name}</span>
                     </span>
                     {isMe && <Badge variant="secondary" className="px-1 py-0 text-[8px] md:text-[9px] h-3 md:h-4 bg-zinc-700">TU</Badge>}
                  </div>
                  <span className="text-[8px] md:text-[10px] text-zinc-500 uppercase font-bold truncate min-h-[12px] md:min-h-[14px] leading-tight">
                    {Boolean(player.difficulty) ? (player.name.toLowerCase().includes("bot") ? (player.difficulty === "Dumb" ? "STUPIDO" : player.difficulty === "Prolog" ? "NORMALE" : player.difficulty) : "BOT") : ""}
                  </span>
                </div>
                
                <div className="flex justify-between items-center w-full text-[10px] sm:text-[11px] md:text-[13px] font-mono mt-auto">
                  {hasBid ? (
                    <div className="flex items-center gap-1 font-bold">
                      {(sourceTricks[player.id] ?? 0) === sourceBids[player.id] ? (
                        <span className="text-emerald-400 flex items-center gap-0.5">{sourceTricks[player.id] ?? 0}/{sourceBids[player.id]} <Check className="size-3 md:size-3.5" /></span>
                      ) : (sourceTricks[player.id] ?? 0) > sourceBids[player.id]! ? (
                        <span className="text-red-500 flex items-center gap-0.5">{sourceTricks[player.id] ?? 0}/{sourceBids[player.id]} <X className="size-3 md:size-3.5" /></span>
                      ) : (
                        <span><span className="text-zinc-400">{sourceTricks[player.id] ?? 0}</span><span className="text-zinc-600">/</span><span className="text-amber-400">{sourceBids[player.id]}</span></span>
                      )}
                    </div>
                  ) : (
                    <div className="text-zinc-600 font-bold">- / -</div>
                  )}
                  <div className="font-black text-white text-right shrink-0">
                    {score} <span className="text-[8px] md:text-[10px] text-zinc-500 font-normal">Pts</span>
                  </div>
                </div>
              </div>
              
              {/* Timer Border (SVG Full) */}
              {isCurrentTurn && (
                <svg className="absolute inset-0 w-full h-full pointer-events-none z-10" style={{ borderRadius: "12px" }}>
                  <rect
                    x="0" y="0" width="100%" height="100%"
                    rx="12" ry="12"
                    fill="none"
                    stroke={turnTimerSeconds && turnTimerSeconds <= 5 ? "#ef4444" : "#38bdf8"}
                    strokeWidth="4"
                    pathLength="100"
                    strokeDasharray="100"
                    strokeDashoffset={100 - timerPct}
                    className="transition-all duration-1000 ease-linear drop-shadow-md"
                  />
                </svg>
              )}
            </button>
          );
        })}
      </div>

      {/* ERROR / WARNING OVERLAYS */}
      {(gameState.lastError || bidError || actionError || lobbyWarning) && (
        <div className="w-full flex justify-center z-40 my-1 animate-in slide-in-from-top-2">
           <div className="flex items-center gap-2 max-w-sm bg-zinc-950/90 border border-rose-500/60 rounded-xl px-3 py-2 shadow-2xl backdrop-blur-md">
             <AlertTriangle className="size-4 text-rose-400 shrink-0" />
             <span className="text-[10px] font-semibold text-rose-200">
               {gameState.lastError ?? bidError ?? actionError ?? lobbyWarning}
             </span>
           </div>
        </div>
      )}

      {/* PLAY AREA (THE TABLE) */}
      <div 
        ref={tableRef}
        className={`relative flex-1 min-h-[220px] w-full max-w-3xl mx-auto flex flex-col items-center justify-start pt-2 rounded-3xl transition-all duration-300 ${
          isCardDragging ? "bg-emerald-950/20 border-2 border-dashed border-emerald-500/40 ring-4 ring-emerald-500/10" : "bg-transparent border-2 border-transparent"
        }`}
      >
        {isCardDragging && (
          <div className="absolute inset-0 flex items-center justify-center pointer-events-none z-0">
            <span className="text-xl sm:text-2xl font-black uppercase tracking-widest text-emerald-500/20 animate-pulse">
              Rilascia Carta
            </span>
          </div>
        )}

        <div className="absolute inset-2 sm:inset-4 rounded-full border border-zinc-800/20 pointer-events-none flex flex-col items-center justify-center z-0">
          <Image src="/wizard_logo.svg" alt="Wizard Logo" width={180} height={75} className="opacity-5 grayscale" />
        </div>

        {/* Trump In Table */}
        {!isGameEnded && (
          <div className="flex flex-col items-center gap-2 z-10 mb-4 bg-zinc-900/40 p-2 sm:p-3 rounded-xl backdrop-blur-sm border border-zinc-800/50 shadow-lg">
            <span className="text-[9px] font-bold uppercase tracking-widest text-zinc-500">
              Briscola
            </span>
            {gameState.trump && "card" in gameState.trump && gameState.trump.card ? (
              <div className="flex flex-col sm:flex-row items-center gap-2">
                <div className="scale-75 origin-center -my-3 sm:my-0">
                  <GameCardView card={gameState.trump.card} effectiveColor={gameState.effectiveTrumpColor || undefined} size="sm" isClickable={false} />
                </div>
                {/* se giocato un wizard, must decide color */}
                {gameState.trump.card.type === "Wizard" && !gameState.effectiveTrumpColor && !canChooseTrump && (
                  <span className="text-[10px] text-zinc-400 font-bold">Da decidere</span>
                )}
                {/* INLINE TRUMP CHOOSER */}
                {isMyTurn && canChooseTrump && (
                  <div className="flex gap-1.5 sm:ml-2">
                    {["Red", "Yellow", "Green", "Blue"].map((c) => {
                       const bg = c === "Red" ? "bg-rose-500 hover:bg-rose-400" :
                                  c === "Yellow" ? "bg-amber-500 hover:bg-amber-400" :
                                  c === "Green" ? "bg-emerald-500 hover:bg-emerald-400" : "bg-blue-500 hover:bg-blue-400";
                       return (
                         <button
                           type="button"
                           key={c}
                           disabled={isSubmitting}
                           onClick={() => { setSelectedColor(c as CardColor); handleChooseTrump(c as CardColor); }}
                           className={`size-8 rounded-full shadow-lg border-2 border-zinc-900 transition-transform active:scale-90 ${bg}`}
                           title={`Scegli ${c}`}
                         />
                       );
                    })}
                  </div>
                )}
              </div>
            ) : (
              <div className="flex flex-col items-center gap-2">
                <span className="text-[10px] font-bold text-zinc-400">Nessuna (No Trump)</span>
                {/* INLINE TRUMP CHOOSER for NO TRUMP (if somehow it's a wizard played first on no trump game? Usually Wizard sets trump on its own) */}
                {isMyTurn && canChooseTrump && (
                  <div className="flex gap-1.5">
                    {["Red", "Yellow", "Green", "Blue"].map((c) => {
                       const bg = c === "Red" ? "bg-rose-500 hover:bg-rose-400" :
                                  c === "Yellow" ? "bg-amber-500 hover:bg-amber-400" :
                                  c === "Green" ? "bg-emerald-500 hover:bg-emerald-400" : "bg-blue-500 hover:bg-blue-400";
                       return (
                         <button
                           type="button"
                           key={c}
                           disabled={isSubmitting}
                           onClick={() => { setSelectedColor(c as CardColor); handleChooseTrump(c as CardColor); }}
                           className={`size-8 rounded-full shadow-lg border-2 border-zinc-900 transition-transform active:scale-90 ${bg}`}
                           title={`Scegli ${c}`}
                         />
                       );
                    })}
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        <div className="flex flex-wrap items-center justify-center gap-2 sm:gap-4 z-10 p-2 mt-auto mb-auto">
          {orderedPlayers.map((player) => {
            const tableSource = revealedTrick ? revealedTrick.entries : gameState.table;
            const played = tableSource.find((entry: any) => entry.playerId === player.id);
            if (!played) return null;
            
            const isWinning = revealedTrick ? player.id === revealedTrick.winnerId : Boolean(gameState.winningCard && cardEquals(played.card, gameState.winningCard));
            const playerNameShort = player.name.substring(0, 3).toUpperCase();
            
            return (
              <div key={player.id} className="relative flex flex-col items-center animate-in zoom-in-95 duration-200">
                <div className={`transition-all relative ${isWinning ? 'scale-105 shadow-xl z-10' : 'scale-95 opacity-90'}`}>
                  <GameCardView card={played.card} size="sm" isClickable={false} />
                  {isWinning && (
                    <div className="absolute -top-3 -right-3 z-30 bg-amber-500 rounded-full p-1 shadow-lg shadow-amber-500/50 border border-amber-300 animate-bounce">
                      <Trophy className="size-3.5 text-zinc-950" />
                    </div>
                  )}
                </div>
                <div className={`absolute -bottom-2 sm:-bottom-3 px-1.5 py-0.5 rounded-md text-[8px] font-black tracking-wider border shadow-md z-20 ${
                  isWinning ? "bg-white text-black border-zinc-300" : "bg-zinc-800 text-zinc-300 border-zinc-600"
                }`}>
                  {player.name.length > 6 ? player.name.substring(0, 6) + '.' : player.name}
                </div>
              </div>
            );
          })}
        </div>
        
      </div>

      {/* REVEALED TRICK OVERLAY & BIDDING CHIPS (ABOVE HAND) */}
      <div className="w-full flex justify-center items-end relative z-[80] mt-auto min-h-[48px]">
        {revealedTrick ? (
          <div className="w-full flex justify-center animate-in slide-in-from-bottom-4 fade-in duration-300 mb-2">
            <div className="flex items-center gap-3 bg-zinc-900/95 backdrop-blur-md border border-zinc-700/80 px-4 py-2 sm:px-6 sm:py-2.5 rounded-full shadow-[0_0_30px_rgba(0,0,0,0.6)]">
              <span className="text-xs sm:text-sm font-black tracking-widest text-white uppercase flex items-center gap-1.5 whitespace-nowrap">
                <Trophy className="size-4 text-amber-400" /> {(() => {
                  const wId = revealedTrick.winnerId;
                  const p = lobby?.players.find(x => x.id === wId);
                  if (!p) return playersMap.get(wId)?.name ?? `P${wId}`;
                  const isRealBot = p.name.toLowerCase().includes("bot");
                  const diff2 = p.difficulty === 'Dumb' ? 'Stupido' : p.difficulty === 'Prolog' ? 'Normale' : p.difficulty;
                  return p.difficulty ? `${p.name} ${isRealBot ? diff2 : 'BOT'}` : p.name;
                })()}
              </span>
              <div className="w-px h-4 bg-zinc-600 hidden sm:block"></div>
              <div className="flex gap-3 text-[10px] sm:text-xs font-bold whitespace-nowrap items-center">
                {(() => {
                  const rBid = revealedTrick.bids[revealedTrick.winnerId];
                  const rTrick = revealedTrick.tricksSnapshot[revealedTrick.winnerId] ?? 0;
                  const hasBid = rBid !== undefined && rBid !== null;
                  
                  if (!hasBid) return <div className="text-zinc-600 font-bold">- / -</div>;
                  
                  if (rTrick === rBid) {
                    return <span className="text-emerald-400 flex items-center gap-0.5">{rTrick}/{rBid} <Check className="size-3.5 sm:size-4" /></span>;
                  } else if (rTrick > rBid) {
                    return <span className="text-red-500 flex items-center gap-0.5">{rTrick}/{rBid} <X className="size-3.5 sm:size-4" /></span>;
                  } else {
                    return <span><span className="text-zinc-400">{rTrick}</span><span className="text-zinc-600 mx-0.5">/</span><span className="text-amber-400">{rBid}</span></span>;
                  }
                })()}
              </div>
              <div className="w-px h-4 bg-zinc-600"></div>
              <div className="flex items-center gap-1">
                <span className="font-mono text-xs sm:text-sm font-black text-white">{revealSecondsLeft}s</span>
              </div>
            </div>
          </div>
        ) : isMyTurn && canBid && (
          <div className="w-full sm:max-w-xl animate-in slide-in-from-bottom-4 mb-2 flex flex-col gap-2">
            <div className="flex flex-wrap gap-2 justify-center px-1 pb-1">
              {Array.from({ length: gameState.round + 1 }, (_, i) => {
                const isForbidden = forbiddenBid !== null && forbiddenBid !== undefined && i === forbiddenBid;
                const isHinted = hintedBid === i;
                const isSelected = bidInput === i;
                
                return (
                  <button
                    type="button"
                    key={i}
                    disabled={isSubmitting || isForbidden}
                    onClick={() => {
                      if (isSelected) {
                        handlePlaceBid(i);
                      } else {
                        setBidInput(i);
                      }
                    }}
                    className={`relative flex items-center justify-center w-10 h-10 rounded-full font-black text-sm transition-all active:scale-90 border-2 ${
                      isForbidden ? "opacity-30 bg-zinc-900 border-rose-900 text-rose-500 cursor-not-allowed" 
                      : isSelected ? (isHinted ? "bg-white text-black border-purple-500 shadow-lg shadow-purple-500/40 scale-110 z-10" : "bg-white text-black border-white shadow-lg shadow-white/20 scale-110 z-10")
                      : isHinted ? "bg-purple-500/20 text-purple-300 border-purple-500 hover:bg-purple-500/40"
                      : "bg-zinc-800 text-zinc-300 border-zinc-600 hover:bg-zinc-700 hover:text-white"
                    }`}
                  >
                    {i}
                    {isHinted && (
                      <span className="absolute -top-1 -right-1 grid size-4 place-items-center rounded-full bg-purple-500 text-white shadow-md">
                        <Lightbulb className="size-2.5 fill-current" />
                      </span>
                    )}
                  </button>
                );
              })}
            </div>
          </div>
        )}
      </div>

      {/* PLAYER HAND & HINT BUTTON */}
      <div className="w-full relative z-20 pb-4 flex justify-center items-end">
        {/* HINT BUTTON BOTTOM LEFT */}
        {canRequestHint && (
          <div className="absolute right-2 sm:right-4 bottom-4 z-40 flex flex-col items-start gap-1">
            {hintError && (
              <div className="text-[10px] font-semibold text-purple-200 bg-purple-950/90 border border-purple-500/60 rounded-xl px-2.5 py-1 shadow-lg">
                ⚠️ {hintError}
              </div>
            )}
            <Button
              type="button"
              size="icon"
              disabled={isSubmitting || isHintLoading}
              onClick={() => void requestHint()}
              title="Suggerimento"
              className="size-10 sm:size-12 rounded-full bg-gradient-to-r from-purple-600 to-purple-500 hover:from-purple-500 hover:to-purple-400 text-white shadow-xl shadow-purple-500/20 border-2 border-purple-400/50 transition-all hover:scale-105 active:scale-95"
            >
              <Lightbulb className={`size-4 sm:size-5 ${isHintLoading ? "animate-spin" : ""}`} />
            </Button>
          </div>
        )}

        <PlayerHand
          hand={gameState.hand}
          selectedCard={selectedCard}
          canPlay={canPlay}
          isSubmitting={isSubmitting}
          isCardPlayable={isCardPlayable}
          onSelectCard={setSelectedCard}
          hintedCard={hintedCard}
          dropZoneRef={tableRef}
          onDropCard={handleDropCard}
          onDragStateChange={setIsCardDragging}
        />
      </div>

      {/* MODALS */}
      {showScoreboard && (
        <div
          className="fixed inset-0 z-[100] flex items-center justify-center bg-black/80 backdrop-blur-sm p-4 animate-in fade-in duration-200"
          onClick={() => setShowScoreboard(false)}
        >
          <div className="w-full max-w-lg" onClick={(e) => e.stopPropagation()}>
            <GameScoreboard players={lobby?.players}
              scoreboard={gameState.scoreboard}
              playersMap={playersMap}
              myPlayerId={playerId}
              initialSelectedPlayerId={scoreboardPlayer}
              onClose={() => setShowScoreboard(false)}
            />
          </div>
        </div>
      )}
    </div>
  );
}
