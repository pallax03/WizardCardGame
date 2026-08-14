package it.unibo.pps.wizard.codecs.engine.model.core

import io.circe._
import io.circe.syntax._
import it.unibo.pps.wizard.codecs.combinators.DiscriminatedCodecs._
import it.unibo.pps.wizard.codecs.engine.model.basic.CardCodecs.given
import it.unibo.pps.wizard.codecs.engine.model.basic.PlayerIdCodecs.given
import it.unibo.pps.wizard.engine.model.basic.cards.Card
import it.unibo.pps.wizard.engine.model.core.CardNotAllowedReasons
import it.unibo.pps.wizard.engine.model.core.GameError
import it.unibo.pps.wizard.engine.model.core.InconsistentStateReasons

object GameErrorCodecs:
  given Encoder[CardNotAllowedReasons] = Encoder.instance:
    case CardNotAllowedReasons.CardNotInHand(cards) =>
      Json.obj("legalCards" -> cards.asJson).withTag("type", "CardNotInHand")
    case CardNotAllowedReasons.MustFollowColor(requiredColor, cards) =>
      Json
        .obj("requiredColor" -> requiredColor.asJson, "legalCards" -> cards.asJson)
        .withTag("type", "MustFollowColor")

  given Decoder[CardNotAllowedReasons] = decodeByTag("type"):
    case "CardNotInHand" =>
      Decoder.forProduct1("legalCards")(CardNotAllowedReasons.CardNotInHand.apply)
    case "MustFollowColor" =>
      Decoder.forProduct2("requiredColor", "legalCards")(
        CardNotAllowedReasons.MustFollowColor.apply
      )

  given Encoder[InconsistentStateReasons] = Encoder.instance:
    case InconsistentStateReasons.TableNoWinner =>
      Json.obj().withTag("type", "TableNoWinner")
    case InconsistentStateReasons.HandNotFoundFor(playerId) =>
      Json.obj("playerId" -> playerId.asJson).withTag("type", "HandNotFoundFor")
    case InconsistentStateReasons.CorruptedState(message) =>
      Json.obj("message" -> message.asJson).withTag("type", "CorruptedState")

  given Decoder[InconsistentStateReasons] = decodeByTag("type"):
    case "TableNoWinner" => Decoder.const(InconsistentStateReasons.TableNoWinner)
    case "HandNotFoundFor" =>
      Decoder.forProduct1("playerId")(InconsistentStateReasons.HandNotFoundFor.apply)
    case "CorruptedState" =>
      Decoder.forProduct1("message")(InconsistentStateReasons.CorruptedState.apply)

  given Encoder[GameError] = Encoder.instance: t =>
    val reason = t match
      case GameError.CardNotAllowed(reason)    => Some(reason.asJson)
      case GameError.InconsistentState(reason) => Some(reason.asJson)
      case _                                   => None
    reason.fold(Json.obj())(r => Json.obj("reason" -> r)).withTag("error", t.productPrefix)

  given Decoder[GameError] = decodeByTag("error"):
    case "NotYourTurn"       => Decoder.const(GameError.NotYourTurn)
    case "InvalidBid"        => Decoder.const(GameError.InvalidBid)
    case "InvalidAction"     => Decoder.const(GameError.InvalidAction)
    case "CardNotAllowed"    => Decoder.forProduct1("reason")(GameError.CardNotAllowed.apply)
    case "InconsistentState" => Decoder.forProduct1("reason")(GameError.InconsistentState.apply)
