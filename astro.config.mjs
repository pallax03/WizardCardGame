import { defineConfig } from 'astro/config';
import mermaid from 'astro-mermaid';
import starlight from '@astrojs/starlight';
import starlightThemeTerminal from 'starlight-theme-terminal';
import remarkMath from 'remark-math';
import rehypeKatex from 'rehype-katex';

export default defineConfig({
  site: 'https://pallax03.github.io',
  base: '/WizardCardGame/report',
  outDir: './dist/report',
  markdown: {
    remarkPlugins: [remarkMath],
    rehypePlugins: [rehypeKatex],
  },
  integrations: [
    mermaid({
      autoTheme: true,
    }),
    starlight({
      title: 'WizardCardGame - SPE Report',
      description: 'Software Process Engineering (SPE) Technical Report',
      plugins: [
        starlightThemeTerminal(),
      ],
      customCss: [
        './src/styles/terminal-custom.css',
        'katex/dist/katex.min.css',
      ],
      head: [
        {
          tag: 'link',
          attrs: {
            rel: 'icon',
            type: 'image/svg+xml',
            href: 'https://raw.githubusercontent.com/pallax03/WizardCardGame/master/application/src/app/icon.svg',
          },
        },
      ],
      social: {
        github: 'https://github.com/pallax03/WizardCardGame',
      },
      sidebar: [
        {
          label: 'Project Report',
          items: [
            { label: 'Overview', slug: 'index' },
            { label: '1. Introduction & Evolution', slug: '01-introduction' },
            { label: '4. Development Process & DVCS', slug: '04-process-dvcs' },
          ],
        },
      ],
    }),
  ],
});
