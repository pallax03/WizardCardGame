import path from "node:path";

export default {
  "application/**/*.{ts,tsx,js,jsx,mjs,cjs,json,css}": (files) => {
    const rel = files.map((f) => path.relative("application", f));
    const eslintFiles = rel.filter((f) =>
      /\.(ts|tsx|js|jsx|mjs|cjs)$/.test(f)
    );
    const cmds = [
      `prettier --write ${files.map((f) => JSON.stringify(f)).join(" ")}`,
    ];
    if (eslintFiles.length > 0) {
      cmds.push(
        `npm -w application exec -- eslint --fix ${eslintFiles.map((f) => JSON.stringify(f)).join(" ")}`
      );
    }
    return cmds;
  },
  "packages/**/*.{ts,tsx,js,jsx,mjs,cjs,json}": (files) => [
    `prettier --write ${files.map((f) => JSON.stringify(f)).join(" ")}`,
  ],
  "wizardEngine/**/*.{scala,sbt}": () =>
    "cd wizardEngine && sbt --batch scalafmtAll scalafmtSbt",
};
