import { ChatSheet } from "@/features/chat/components/ChatSheet";
import { LobbySessionProvider } from "@/features/lobby-session";
import { Suspense } from "react";
import { Skeleton } from "@/ui/components/skeleton";

export default function LobbyLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <LobbySessionProvider>
      <div>
        <div>{children}</div>
        <Suspense fallback={<Skeleton className="fixed bottom-6 right-6 h-14 w-14 rounded-full shadow-lg bg-zinc-800 z-50" />}>
          <ChatSheet />
        </Suspense>
      </div>
    </LobbySessionProvider>
  );
}
