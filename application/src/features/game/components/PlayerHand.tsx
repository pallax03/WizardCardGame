"use client";

import { useRef, useState, type RefObject } from "react";
import { createPortal } from "react-dom";
import { animate, motion, useMotionValue, type AnimationPlaybackControls } from "motion/react";
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
  hintedCard?: Card | null;
  dropZoneRef?: RefObject<HTMLDivElement | null>;
  onDropCard?: (card: Card) => void;
  onDragStateChange?: (dragging: boolean) => void;
}

type OverlayOrigin = { left: number; top: number };

interface DraggableHandCardProps {
  card: Card;
  isSelected: boolean;
  isHinted: boolean;
  isLegal: boolean;
  draggable: boolean;
  rotation: number;
  offsetY: number;
  size: "sm" | "md" | "lg";
  dropZoneRef?: RefObject<HTMLDivElement | null>;
  onToggleSelect: () => void;
  onDropCard?: (card: Card) => void;
  onDragStateChange?: (dragging: boolean) => void;
}

function DraggableHandCard({
  card,
  isSelected,
  isHinted,
  isLegal,
  draggable,
  rotation,
  offsetY,
  size,
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
        style={{
          rotate: isSelected ? 0 : rotation,
          y: isSelected ? -28 : offsetY,
          zIndex: isSelected ? 50 : 10,
        }}
        whileHover={draggable ? { y: -28, rotate: 0, zIndex: 40, scale: 1.08 } : undefined}
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
        aria-label={`${cardToString(card)}${isLegal ? "" : " (non giocabile)"}${isHinted ? " (suggerita dall'AI)" : ""}. Trascina sul tavolo per giocarla.`}
        className={`relative shrink-0 transition-all duration-200 origin-bottom ${
          draggable ? "cursor-grab touch-none active:cursor-grabbing" : ""
        } ${overlayOrigin ? "opacity-0" : ""}`}
      >
        <GameCardView
          card={card}
          size={size}
          isSelected={isSelected}
          isHinted={isHinted}
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
            <div className="rounded-xl ring-2 ring-amber-300 ring-offset-2 ring-offset-transparent shadow-2xl">
              <GameCardView card={card} size={size} isClickable={false} />
            </div>
          </motion.div>,
          document.body
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
  hintedCard = null,
  dropZoneRef,
  onDropCard,
  onDragStateChange,
}: PlayerHandProps) {
  const totalCards = hand.length;

  // Regola richiesta: < 5 -> "lg", 5..9 -> "md", >= 10 -> "sm"
  const cardSize: "sm" | "md" | "lg" =
    totalCards <= 4 ? "lg" : totalCards <= 7 ? "md" : "sm";

  const renderRow = (cardsRow: Card[], startIndex: number, size: "sm" | "md" | "lg") => {
    const rowTotal = cardsRow.length;

    // Spaziatura adattata alla dimensione della carta
    const spacingClass =
      size === "sm"
        ? rowTotal <= 5
          ? "-space-x-3 sm:-space-x-5"
          : "-space-x-5 sm:-space-x-7"
        : rowTotal <= 2
        ? "space-x-4 sm:space-x-8"
        : rowTotal <= 4
        ? "-space-x-4 sm:-space-x-6"
        : rowTotal <= 6
        ? "-space-x-6 sm:-space-x-8"
        : "-space-x-8 sm:-space-x-10";

    return (
      <div className={`flex items-end justify-center w-full max-w-full ${spacingClass} min-h-[120px] sm:min-h-[140px] py-2 px-0.5 overflow-visible`}>
        {cardsRow.map((card, i) => {
          const index = startIndex + i;
          const isSelected = cardEquals(card, selectedCard);
          const isHinted = cardEquals(card, hintedCard);
          const isLegal = isCardPlayable(card);
          const draggable = canPlay && isLegal && !isSubmitting;

          const midIndex = (rowTotal - 1) / 2;
          const offsetFromCenter = i - midIndex;
          const rotation = rowTotal > 1 ? offsetFromCenter * 2.2 : 0;
          const offsetY = rowTotal > 1 ? Math.abs(offsetFromCenter) * 1.2 : 0;

          return (
            <DraggableHandCard
              key={index}
              card={card}
              isSelected={isSelected}
              isHinted={isHinted}
              isLegal={canPlay ? isLegal : true}
              draggable={draggable}
              rotation={rotation}
              offsetY={offsetY}
              size={size}
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
    );
  };

  if (totalCards === 0) {
    return (
      <div className="relative w-full pt-12 pb-4 px-2 overflow-visible">
        <div className="text-center py-4 text-zinc-500 text-xs italic">
          Mano vuota
        </div>
      </div>
    );
  }

  /* Dai 10 elementi in poi la mano viene divisa a metà su 2 righe con dimensione "sm" */
  if (totalCards >= 10) {
    const halfIndex = Math.ceil(totalCards / 2);
    const topRow = hand.slice(0, halfIndex);
    const bottomRow = hand.slice(halfIndex);

    return (
      <div className="relative w-full pt-12 pb-4 px-0.5 overflow-visible flex flex-col items-center -space-y-10">
        <div className="z-10 relative w-full flex justify-center">{renderRow(topRow, 0, cardSize)}</div>
        <div className="z-20 relative w-full flex justify-center">{renderRow(bottomRow, halfIndex, cardSize)}</div>
      </div>
    );
  }

  return (
    <div className="relative w-full pt-16 pb-4 px-0.5 overflow-visible">
      {renderRow(hand, 0, cardSize)}
    </div>
  );
}