"use client";

import { useState } from "react";
import { Card as UiCard, CardContent, CardHeader, CardTitle } from "@/ui/components/card";
import type { EventMessage } from "../types";

interface GameEventLogProps {
  gameEvents: EventMessage[];
}

export function GameEventLog({ gameEvents }: GameEventLogProps) {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <UiCard className="bg-zinc-900/60 border-zinc-800">
      <CardHeader
        className="pb-2 cursor-pointer flex flex-row items-center justify-between"
        onClick={() => setIsOpen((prev) => !prev)}
      >
        <div className="flex items-center gap-2">
          <CardTitle className="text-sm font-semibold uppercase tracking-wider text-zinc-400">
            Received Event Messages ({gameEvents.length}) {isOpen ? "▲" : "▼"}
          </CardTitle>
          <span className="text-[10px] bg-zinc-800 text-zinc-400 px-1.5 py-0.5 rounded">
            WebSocket stream
          </span>
        </div>
        <span className="text-xs text-indigo-400">
          {isOpen ? "Hide Log" : "Inspect Events"}
        </span>
      </CardHeader>
      {isOpen && (
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
  );
}
