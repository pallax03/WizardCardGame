"use client";

import { useEffect, useState } from "react";
import { getPlayerGameSnapshot } from "@/features/game/api";
import { mapSnapshotToBoardState } from "@/features/game/state/snapshotMapper";
import type { GameBoardState } from "@/features/game/types";

export function useLobbyGameSnapshot(
  lobbyId: string | null,
  playerId: number | null,
  enabled: boolean,
  statusKey: string | undefined
) {
  const [board, setBoard] = useState<GameBoardState | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [loadFailed, setLoadFailed] = useState(false);

  useEffect(() => {
    if (!enabled || !lobbyId || playerId === null) {
      queueMicrotask(() => {
        setBoard(null);
        setIsLoading(false);
        setLoadFailed(false);
      });
      return;
    }
    let cancelled = false;
    queueMicrotask(() => {
      if (cancelled) return;
      setIsLoading(true);
      setLoadFailed(false);
    });
    getPlayerGameSnapshot(lobbyId)
      .then((snapshot) => {
        if (cancelled) return;
        if (!snapshot) {
          setBoard(null);
          return;
        }
        try {
          setBoard(mapSnapshotToBoardState(snapshot, playerId));
        } catch {
          setBoard(null);
          setLoadFailed(true);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setBoard(null);
          setLoadFailed(true);
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [enabled, lobbyId, playerId, statusKey]);

  return { board, isLoading, loadFailed };
}
