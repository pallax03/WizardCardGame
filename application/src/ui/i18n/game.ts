const it = {
  locale: "it",
  metadata: {
    title: "Wizard Card Game | Gioca online",
    lobbyTitle: "Wizard Card Game | Stanza",
    description: "Gioca a Wizard online con i tuoi amici",
  },
};

const en = {
  locale: "en",
  metadata: {
    title: "Wizard Card Game | Play online",
    lobbyTitle: "Wizard Card Game | Lobby",
    description: "Play Wizard online with your friends",
  },
};

export const getGameI18n = (lang: string) => lang === "en" ? en : it;

// Fallback for any client imports if they exist
export const gameI18n = it;

