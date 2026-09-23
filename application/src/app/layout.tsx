import { Geist, Geist_Mono, Inter } from "next/font/google";
import "./globals.css";
import { cn } from "@/lib/utils";

const inter = Inter({subsets:['latin'],variable:'--font-sans'});

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

import { cookies } from "next/headers";
import { getGameI18n } from "@/i18n/game";

export async function generateMetadata() {
  const cookieStore = await cookies();
  const lang = cookieStore.get("wizard_lang")?.value === "en" ? "en" : "it";
  const i18n = getGameI18n(lang);
  return {
    title: i18n.metadata.title,
    description: i18n.metadata.description,
    openGraph: {
      images: ['/app.png']
    }
  };
}

export default async function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const cookieStore = await cookies();
  const lang = cookieStore.get("wizard_lang")?.value === "en" ? "en" : "it";
  
  return (
    <html
      lang={lang}
      className={cn("dark h-full", "antialiased", geistSans.variable, geistMono.variable, "font-sans", inter.variable)}
    >
      <body className="min-h-full flex flex-col">
        {children}
      </body>
    </html>
  );
}
