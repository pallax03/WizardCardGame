package io.github.pallax03.wizard.codecs.engine.model.core

import io.circe.*
import io.circe.syntax.*

import io.github.pallax03.wizard.codecs.combinators.DiscriminatedCodecs.*
import io.github.pallax03.wizard.codecs.engine.model.*
import io.github.pallax03.wizard.engine.model.basic.*
import io.github.pallax03.wizard.engine.model.core.{CardNotAllowedReasons, GameActionError}
import io.github.pallax03.wizard.engine.model.events.{InvitationEvent, WizardEvent}

import sttp.tapir.Schema

object GameActionErrorCodecs:
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

  given Encoder[GameActionError] = Encoder.instance:
    case GameActionError.NotYourTurn(turnOf) =>
      Json.obj("turnOf" -> turnOf.asJson).withTag("error", "NotYourTurn")
    case GameActionError.InvalidBid(round, bid) =>
      Json.obj("round" -> round.asJson, "bid" -> bid.asJson).withTag("error", "InvalidBid")
    case GameActionError.InvalidAction(invitationEvent) =>
      Json
        .obj("invitationEvent" -> invitationEvent.map(_.asInstanceOf[WizardEvent]).asJson)
        .withTag("error", "InvalidAction")
    case GameActionError.CardNotAllowed(reason) =>
      Json.obj("reason" -> reason.asJson).withTag("error", "CardNotAllowed")

  given Decoder[GameActionError] = decodeByTag("error"):
    case "NotYourTurn" => Decoder.forProduct1("turnOf")(GameActionError.NotYourTurn.apply)
    case "InvalidBid"  => Decoder.forProduct2("round", "bid")(GameActionError.InvalidBid.apply)
    case "InvalidAction" =>
      Decoder.forProduct1[GameActionError, Option[WizardEvent]]("invitationEvent") { ev =>
        GameActionError.InvalidAction(ev.map(_.asInstanceOf[InvitationEvent]))
      }
    case "CardNotAllowed" => Decoder.forProduct1("reason")(GameActionError.CardNotAllowed.apply)

  given Schema[CardNotAllowedReasons] = Schema.string
  given Schema[GameActionError] = Schema.string
