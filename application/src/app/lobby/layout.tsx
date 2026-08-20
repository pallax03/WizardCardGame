import { ChatSheet } from "@/features/chat";
import { Suspense } from "react";
import { Skeleton } from "@/ui/components/skeleton";

export default function LobbyLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <div>
      <div>{children}</div>
      <Suspense fallback={<Skeleton className="fixed bottom-6 right-6 h-14 w-14 rounded-full shadow-lg bg-zinc-800 z-50" />}>
        <ChatSheet />
      </Suspense>
    </div>
  );
}
