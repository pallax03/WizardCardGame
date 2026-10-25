package io.github.pallax03.wizard.engine.lobby

/** Defines the AI reasoning strategy assigned to automated bot players. */
enum BotsDifficulty:
  /** Fast baseline strategy executing heuristic fallback moves via [[io.github.pallax03.wizard.engine.model.rules.FallbackStrategy]]. */
  case Dumb

  /** Advanced strategy utilizing rule-based logic and game inference via [[io.github.pallax03.wizard.engine.adapters.prolog.WizardPrologEngine]]. */
  case Prolog
