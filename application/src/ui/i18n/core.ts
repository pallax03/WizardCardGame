import { it, en } from "./dictionaries";

export function getLang() {
  if (typeof window === "undefined") return "it";
  return document.cookie.includes("wizard_lang=en") ? "en" : "it";
}

export function t<K extends keyof typeof it>(section: K): typeof it[K] {
  return new Proxy({} as typeof it[K], {
    get: (_, prop: string) => {
      const lang = getLang();
      const dict = lang === "en" ? en : it;
      return (dict[section] as Record<string, unknown>)[prop];
    }
  });
}
