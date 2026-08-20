"use server";

export async function getLobbyState(lobbyId: string) {
    const res = await fetch(`${process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:5001'}/api/lobby/${lobbyId}`, {
        cache: 'no-store'
    });
    if (!res.ok) {
        throw new Error("Failed to fetch lobby state");
    }
    return res.json();
}
