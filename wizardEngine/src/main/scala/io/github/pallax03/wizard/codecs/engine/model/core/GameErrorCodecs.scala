package io.github.pallax03.wizard.codecs.engine.model.core

import io.circe.*
import io.circe.syntax.*

import sttp.tapir.Schema

import io.github.pallax03.wizard.codecs.combinators.DiscriminatedCodecs.*
import io.github.pallax03.wizard.codecs.engine.model._
import io.github.pallax03.wizard.engine.model.basic._
import io.github.pallax03.wizard.engine.model.core.{CardNotAllowedReasons, GameError}
import io.github.pallax03.wizard.engine.model.events.{InvitationEvent, WizardEvent}

object GameErrorCodecs:
  import basic.CardCodecs.given
  import basic.PlayerIdCodecs.given
  import WizardEventsCodecs.given
  import gameplay.Round
  import bidding.Bid
  
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

  given Encoder[GameError] = Encoder.instance:
    case GameError.NotYourTurn(turnOf) =>
      Json.obj("turnOf" -> turnOf.asJson).withTag("error", "NotYourTurn")
    case GameError.InvalidBid(round, bid) =>
      Json.obj("round" -> round.asJson, "bid" -> bid.asJson).withTag("error", "InvalidBid")
    case GameError.InvalidAction(invitationEvent) =>
      Json
        .obj("invitationEvent" -> invitationEvent.map(_.asInstanceOf[WizardEvent]).asJson)
        .withTag("error", "InvalidAction")
    case GameError.CardNotAllowed(reason) =>
      Json.obj("reason" -> reason.asJson).withTag("error", "CardNotAllowed")

  given Decoder[GameError] = decodeByTag("error"):
    case "NotYourTurn" => Decoder.forProduct1("turnOf")(GameError.NotYourTurn.apply)
    case "InvalidBid"  => Decoder.forProduct2("round", "bid")(GameError.InvalidBid.apply)
    case "InvalidAction" =>
      Decoder.forProduct1[GameError, Option[WizardEvent]]("invitationEvent") { ev =>
        GameError.InvalidAction(ev.map(_.asInstanceOf[InvitationEvent]))
      }
    case "CardNotAllowed" => Decoder.forProduct1("reason")(GameError.CardNotAllowed.apply)
  
  given Schema[CardNotAllowedReasons] = Schema.string
  given Schema[GameError] = Schema.string
