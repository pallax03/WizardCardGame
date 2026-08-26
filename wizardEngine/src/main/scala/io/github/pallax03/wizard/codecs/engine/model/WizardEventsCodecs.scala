package io.github.pallax03.wizard.codecs.engine.model

import io.circe._
import io.circe.syntax._
import io.github.pallax03.wizard.engine.model.basic._
import io.github.pallax03.wizard.engine.model.core.GameError
import io.github.pallax03.wizard.engine.model.events._

object WizardEventsCodecs:

  import gameplay.Round
  import cards.Card
  import basic.PlayerIdCodecs.given
  import basic.HandsCodecs.given
  import basic.TrumpCodecs.given
  import basic.CardCodecs.given
  import basic.ScoreboardCodecs.given
  import core.GameErrorCodecs.given

  given Encoder[WizardEvent] = Encoder.instance: e =>
    val encodedEvent = e match
      case ev: LifecycleEvent  => Encoder.AsObject.derived[LifecycleEvent].encodeObject(ev)
      case ev: ProgressEvent   => Encoder.AsObject.derived[ProgressEvent].encodeObject(ev)
      case ev: ActionEvent     => Encoder.AsObject.derived[ActionEvent].encodeObject(ev)
      case ev: InvitationEvent => Encoder.AsObject.derived[InvitationEvent].encodeObject(ev)
      case ev: FailureEvent    => Encoder.AsObject.derived[FailureEvent].encodeObject(ev)

    val eventType = e.getClass.getInterfaces
      .map(_.getSimpleName)
      .find(_.endsWith("Event"))
      .getOrElse("WizardEvent")
    val eventAction = encodedEvent.keys.head
    val fields = encodedEvent(eventAction).get
    val scopedFields = e match
      case p: PlayerScoped      => List("playerId" -> p.playerId.asJson)
      case d: DestinationScoped => List("destinationId" -> d.destinationId.asJson)
      case _                    => Nil

    val eventFields = List(
      "type" -> Json.fromString(eventType),
      "action" -> Json.fromString(eventAction)
    ) ::: scopedFields ::: List(
      "fields" -> fields
    )

    Json.obj("event" -> Json.obj(eventFields*))

  given Decoder[WizardEvent] = Decoder.instance { c =>
    val ev = c.downField("event")
    val fields = ev.downField("fields")

    ev.downField("action").as[String].flatMap {
      case "GameStarted" =>
        fields.get[List[PlayerId]]("playersIds").map(LifecycleEvent.GameStarted.apply)
      case "WaitingForTrump" =>
        ev.get[PlayerId]("playerId").map(InvitationEvent.WaitingForTrump.apply)
      case "WaitingForBid" =>
        for {
          p <- ev.get[PlayerId]("playerId")
          r <- fields.get[Round]("round")
        } yield InvitationEvent.WaitingForBid(p, r)
      case "WaitingForCard" =>
        for {
          p <- ev.get[PlayerId]("playerId")
          cards <- fields.get[List[Card]]("legalCards")
        } yield InvitationEvent.WaitingForCard(p, cards)
      case "ActionFailed" =>
        for {
          p <- fields.get[PlayerId]("playerId")
          err <- fields.get[GameError]("reason")
        } yield FailureEvent.ActionFailed(p, err)
      case other =>
        Left(DecodingFailure(s"No decoding for $other.", c.history))
    }
  }
