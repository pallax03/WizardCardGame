"use client";

import { useRef, useState, type RefObject } from "react";
import { createPortal } from "react-dom";
import { animate, motion, useMotionValue, type AnimationPlaybackControls } from "motion/react";
import { Card as UiCard, CardContent, CardHeader, CardTitle } from "@/ui/components/card";
import { GameCardView } from "./GameCardView";
import { cardEquals, cardToString } from "../state/gameReducer";
import type { Card } from "../types";

interface PlayerHandProps {
  hand: Card[];
  selectedCard: Card | null;
  canPlay: boolean;
  isSubmitting?: boolean;
  isCardPlayable: (card: Card) => boolean;
  onSelectCard: (card: Card | null) => void;
  /** Ref del tavolo: la carta viene giocata se rilasciata dentro i suoi bounds. */
  dropZoneRef?: RefObject<HTMLDivElement | null>;
  onDropCard?: (card: Card) => void;
  onDragStateChange?: (dragging: boolean) => void;
}

type OverlayOrigin = { left: number; top: number };

interface DraggableHandCardProps {
  card: Card;
  isSelected: boolean;
  isLegal: boolean;
  draggable: boolean;
  dropZoneRef?: RefObject<HTMLDivElement | null>;
  onToggleSelect: () => void;
  onDropCard?: (card: Card) => void;
  onDragStateChange?: (dragging: boolean) => void;
}

/**
 * Carta trascinabile con overlay su document.body: mentre trascini, la carta
 * vive in un portal fixed z-[100], fuori da ogni stacking context della pagina
 * (backdrop-blur, transform, ...), quindi resta sempre sopra al tavolo.
 */
function DraggableHandCard({
  card,
  isSelected,
  isLegal,
  draggable,
  dropZoneRef,
  onToggleSelect,
  onDropCard,
  onDragStateChange,
}: DraggableHandCardProps) {
  const x = useMotionValue(0);
  const y = useMotionValue(0);
  const [overlayOrigin, setOverlayOrigin] = useState<OverlayOrigin | null>(null);
  const slotRef = useRef<HTMLDivElement | null>(null);
  const gestureRef = useRef<{
    pointerId: number;
    startX: number;
    startY: number;
    dragging: boolean;
  } | null>(null);
  const returnAnimsRef = useRef<AnimationPlaybackControls[]>([]);
  const dragSeqRef = useRef(0);

  const stopReturnAnims = () => {
    returnAnimsRef.current.forEach((controls) => controls.stop());
    returnAnimsRef.current = [];
  };

  const isOverDropZone = (point: { x: number; y: number }) => {
    const zone = dropZoneRef?.current?.getBoundingClientRect();
    if (!zone) return false;
    return point.x >= zone.left && point.x <= zone.right && point.y >= zone.top && point.y <= zone.bottom;
  };

  const handlePointerDown = (event: React.PointerEvent<HTMLDivElement>) => {
    if (!draggable) return;
    if (event.pointerType === "mouse" && event.button !== 0) return;
    stopReturnAnims();
    x.set(0);
    y.set(0);
    setOverlayOrigin(null);
    slotRef.current?.setPointerCapture(event.pointerId);
    gestureRef.current = {
      pointerId: event.pointerId,
      startX: event.clientX,
      startY: event.clientY,
      dragging: false,
    };
  };

  const handlePointerMove = (event: React.PointerEvent<HTMLDivElement>) => {
    const gesture = gestureRef.current;
    if (!gesture || event.pointerId !== gesture.pointerId) return;
    const dx = event.clientX - gesture.startX;
    const dy = event.clientY - gesture.startY;
    if (!gesture.dragging) {
      if (Math.hypot(dx, dy) < 8) return;
      const rect = slotRef.current?.getBoundingClientRect();
      if (!rect) return;
      gesture.dragging = true;
      dragSeqRef.current += 1;
      setOverlayOrigin({ left: rect.left, top: rect.top });
      onDragStateChange?.(true);
    }
    x.set(dx);
    y.set(dy);
  };

  const finishGesture = (clientX: number, clientY: number, cancelled: boolean) => {
    const gesture = gestureRef.current;
    gestureRef.current = null;
    if (!gesture) return;
    // Tocco senza movimento = tap = seleziona/deseleziona.
    if (!gesture.dragging) {
      onToggleSelect();
      return;
    }
    onDragStateChange?.(false);
    if (!cancelled && isOverDropZone({ x: clientX, y: clientY })) {
      setOverlayOrigin(null);
      x.set(0);
      y.set(0);
      onDropCard?.(card);
      return;
    }
    // Rilascio fuori dal tavolo: l'overlay torna elastico al suo posto.
    const seq = dragSeqRef.current;
    const done = () => {
      if (dragSeqRef.current !== seq) return;
      setOverlayOrigin(null);
      x.set(0);
      y.set(0);
    };
    stopReturnAnims();
    returnAnimsRef.current = [
      animate(x, 0, { type: "spring", stiffness: 500, damping: 35 }),
      animate(y, 0, { type: "spring", stiffness: 500, damping: 35, onComplete: done }),
    ];
  };

  return (
    <>
      <motion.div
        ref={slotRef}
        whileHover={draggable ? { y: -6 } : undefined}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={(event) => finishGesture(event.clientX, event.clientY, false)}
        onPointerCancel={(event) => finishGesture(event.clientX, event.clientY, true)}
        onDoubleClick={() => {
          if (draggable) onDropCard?.(card);
        }}
        onKeyDown={(event) => {
          if (!draggable) return;
          if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            onToggleSelect();
          }
        }}
        role="button"
        tabIndex={draggable ? 0 : -1}
        aria-label={`${cardToString(card)}${isLegal ? "" : " (non giocabile)"}. Trascina sul tavolo per giocarla.`}
        className={`${draggable ? "cursor-grab touch-none active:cursor-grabbing" : ""}${overlayOrigin ? " opacity-0" : ""}`}
      >
        <GameCardView
          card={card}
          size="lg"
          isSelected={isSelected}
          isLegal={isLegal}
          isClickable={false}
        />
      </motion.div>
      {overlayOrigin &&
        createPortal(
          <motion.div
            aria-hidden
            className="pointer-events-none fixed z-[100]"
            style={{ left: overlayOrigin.left, top: overlayOrigin.top, x, y }}
          >
            <div className="rounded-xl ring-2 ring-amber-300 ring-offset-2 ring-offset-transparent">
              <GameCardView card={card} size="lg" isClickable={false} />
            </div>
          </motion.div>,
          document.body,
        )}
    </>
  );
}

export function PlayerHand({
  hand,
  selectedCard,
  canPlay,
  isSubmitting = false,
  isCardPlayable,
  onSelectCard,
  dropZoneRef,
  onDropCard,
  onDragStateChange,
}: PlayerHandProps) {
  return (
    <UiCard className="bg-zinc-950/80 border-amber-500/40 backdrop-blur-md shadow-2xl">
      <CardHeader className="p-3 pb-2 flex flex-row items-center justify-between border-b border-zinc-800/60">
        <CardTitle className="text-xs font-black uppercase tracking-widest text-amber-400 flex items-center gap-2">
          <span>🃏</span> La Tua Mano ({hand.length} carte)
        </CardTitle>
        {canPlay && (
          <span className="text-xs text-amber-300 font-bold animate-pulse">
            Trascina una carta sul tavolo per giocarla
          </span>
        )}
      </CardHeader>
      <CardContent className="p-3">
        {hand.length > 0 ? (
          <div className="flex flex-wrap gap-2 sm:gap-3 items-center justify-center p-3 bg-zinc-900/60 rounded-xl border border-zinc-800/80 min-h-[140px]">
            {hand.map((card, index) => {
              const isSelected = cardEquals(card, selectedCard);
              const isLegal = isCardPlayable(card);
              const draggable = canPlay && isLegal && !isSubmitting;
              return (
                <DraggableHandCard
                  key={index}
                  card={card}
                  isSelected={isSelected}
                  isLegal={canPlay ? isLegal : true}
                  draggable={draggable}
                  dropZoneRef={dropZoneRef}
                  onToggleSelect={() => {
                    if (canPlay && isLegal) onSelectCard(isSelected ? null : card);
                  }}
                  onDropCard={onDropCard}
                  onDragStateChange={onDragStateChange}
                />
              );
            })}
          </div>
        ) : (
          <div className="text-center py-8 text-zinc-500 text-xs">
            Mano vuota (in attesa delle carte del prossimo round)
          </div>
        )}
      </CardContent>
    </UiCard>
  );
}
