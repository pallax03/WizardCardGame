"use client";

import { useState } from "react";
import { useParams, useSearchParams } from "next/navigation";
import { Button } from "@/ui/components/button";
import { chooseTrumpColor, playCard, placeBid } from "@/features/game/api";

export default function GamePage() {
  const params = useParams<{ id: string }>();
  const searchParams = useSearchParams();
  const lobbyId = params.id;
  const playerId = Number(searchParams.get("playerId") || "1");

  const [status, setStatus] = useState<string | null>(null);

  const handlePlayCard = async () => {
    try {
      setStatus("Playing card...");
      await playCard(lobbyId, playerId);
      setStatus("Card played successfully");
      console.log("Card played successfully");
    } catch (error) {
      setStatus(`Error playing card: ${error instanceof Error ? error.message : String(error)}`);
      console.error("Error playing card:", error);
    }
  };

  const handlePlaceBid = async () => {
    try {
      setStatus("Placing bid...");
      await placeBid(lobbyId, playerId, 1);
      setStatus("Bid placed successfully");
      console.log("Bid placed successfully");
    } catch (error) {
      setStatus(`Error placing bid: ${error instanceof Error ? error.message : String(error)}`);
      console.error("Error placing bid:", error);
    }
  };

  const handleChooseTrumpColor = async () => {
    try {
      setStatus("Choosing trump color...");
      await chooseTrumpColor(lobbyId, playerId, "Red");
      setStatus("Trump color chosen successfully");
      console.log("Trump color chosen successfully");
    } catch (error) {
      setStatus(`Error choosing trump color: ${error instanceof Error ? error.message : String(error)}`);
      console.error("Error choosing trump color:", error);
    }
  };

  return (
    <main className="app-page p-6 flex items-center justify-center min-h-screen">
      <div className="flex flex-col items-center justify-center gap-4">
        <h1 className="text-4xl font-bold mb-4">Game Started</h1>
        <p className="text-muted-foreground mb-2">
          Lobby: <span className="font-semibold">{lobbyId}</span> | Player:{" "}
          <span className="font-semibold">{playerId}</span>
        </p>
        <div className="flex flex-col gap-3 w-64">
          <Button variant="primary" size="lg" onClick={handlePlayCard}>
            Play Card
          </Button>
          <Button variant="primary" size="lg" onClick={handlePlaceBid}>
            Place Bid
          </Button>
          <Button variant="primary" size="lg" onClick={handleChooseTrumpColor}>
            Choose Trump Color
          </Button>
        </div>
        {status && (
          <p className="text-sm mt-4 text-center px-4 py-2 rounded bg-muted/50">
            {status}
          </p>
        )}
      </div>
    </main>
  );
}