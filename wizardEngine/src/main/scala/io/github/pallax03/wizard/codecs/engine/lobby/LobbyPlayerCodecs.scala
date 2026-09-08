package io.github.pallax03.wizard.codecs.engine.lobby

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

import io.github.pallax03.wizard.codecs.engine.lobby.LobbyCodecs.given
import io.github.pallax03.wizard.codecs.engine.model.basic.PlayerIdCodecs.given
import io.github.pallax03.wizard.engine.lobby.LobbyPlayer

object LobbyPlayerCodecs:
  given Codec[LobbyPlayer] = deriveCodec[LobbyPlayer]
