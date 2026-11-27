import { defineConfig } from 'astro/config';
import mermaid from 'astro-mermaid';
import starlight from '@astrojs/starlight';
import starlightThemeRapide from 'starlight-theme-rapide';
import remarkMath from 'remark-math';
import rehypeKatex from 'rehype-katex';

export default defineConfig({
  site: 'https://pallax03.github.io',
  base: '/WizardCardGame',
  outDir: './dist',
  markdown: {
    remarkPlugins: [remarkMath],
    rehypePlugins: [rehypeKatex],
  },
  integrations: [
    mermaid({
      autoTheme: true,
    }),
    starlight({
      title: 'WizardCardGame',
      description: 'Software Process Engineering (SPE) Technical Report',
      plugins: [
        starlightThemeRapide(),
        {
          name: 'custom-css-after-rapide',
          hooks: {
            'config:setup'({ config, updateConfig }) {
              updateConfig({
                customCss: [...(config.customCss ?? []), './src/styles/custom.css'],
              });
            },
          },
        },
      ],
      customCss: [
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
            { label: '2. Requirements', slug: '02-requirements' },
            { label: '4. Development Process & DVCS', slug: '04-process-dvcs' },
          ],
        },
        {
          label: 'Play Wizard Online',
          link: 'https://wizardcardgame.vercel.app/',
          attrs: { target: '_blank', rel: 'noopener noreferrer' },
        },
      ],
    }),
  ],
});
