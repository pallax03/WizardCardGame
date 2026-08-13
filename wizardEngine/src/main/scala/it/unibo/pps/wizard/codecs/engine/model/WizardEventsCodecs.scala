package it.unibo.pps.wizard.codecs.engine.model

import io.circe.*
import io.circe.syntax.*
import it.unibo.pps.wizard.engine.model.events.*

object WizardEventsCodecs:
  import basic.PlayerIdCodecs.given
  import basic.HandsCodecs.given
  import basic.TrumpCodecs.given
  import basic.CardCodecs.given
  import basic.ScoreboardCodecs.given
  import core.GameErrorCodecs.given

  given Encoder[WizardEvent] = Encoder.instance: e =>
    val encodedEvent = e match
      case ev: LifecycleEvent => Encoder.AsObject.derived[LifecycleEvent].encodeObject(ev)
      case ev: ProgressEvent => Encoder.AsObject.derived[ProgressEvent].encodeObject(ev)
      case ev: ActionEvent => Encoder.AsObject.derived[ActionEvent].encodeObject(ev)
      case ev: InvitationEvent => Encoder.AsObject.derived[InvitationEvent].encodeObject(ev)
      case ev: FailureEvent => Encoder.AsObject.derived[FailureEvent].encodeObject(ev)

    val eventType = e.getClass.getInterfaces
      .map(_.getSimpleName)
      .find(_.endsWith("Event"))
      .getOrElse("WizardEvent")
    val eventAction  = encodedEvent.keys.head
    val fields = encodedEvent(eventAction).get
    val scopedFields = e match
      case p: PlayerScoped => List("playerId" -> p.playerId.asJson)
      case d: DestinationScoped => List("destinationId" -> d.destinationId.asJson)
      case _ => Nil

    val eventFields = List(
      "type" -> Json.fromString(eventType),
      "action" -> Json.fromString(eventAction)
    ) ::: scopedFields ::: List(
      "fields" -> fields
    )

    Json.obj("event" -> Json.obj(eventFields*))
