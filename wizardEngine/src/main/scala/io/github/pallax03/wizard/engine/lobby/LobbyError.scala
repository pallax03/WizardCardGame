package io.github.pallax03.wizard.engine.lobby

enum LobbyError(val message: String, val code: String):
  case LobbyFull extends LobbyError("Lobby is full", "LOBBY_FULL")
  case GameInProgress extends LobbyError("Game already started or finished", "GAME_ALREADY_STARTED")
