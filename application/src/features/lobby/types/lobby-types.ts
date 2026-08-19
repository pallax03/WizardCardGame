export interface Difficulty {
  level?: string;
}

export interface ApiPlayer {
  id: string;
  name: string;
  difficulty?: Difficulty | null;
}

export interface LobbyApiResponse {
  lobbyId: string;
  players: ApiPlayer[];
}