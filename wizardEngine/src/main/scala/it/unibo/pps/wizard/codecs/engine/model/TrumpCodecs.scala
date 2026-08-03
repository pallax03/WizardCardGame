package it.unibo.pps.wizard.codecs.engine.model

import io.circe._, io.circe.syntax._
import it.unibo.pps.wizard.codecs.combinators.DiscriminatedCodecs._
import it.unibo.pps.wizard.engine.model.basic.gameplay.Trump
import it.unibo.pps.wizard.engine.model.basic.cards.Card

object TrumpCodecs:
  import CardCodecs.given
  
  given Encoder[Trump] = Encoder.instance: t =>
    val fields = List(
      t.card.map(c => "card" -> c.asJson),
      t.effectiveColor.map(col => "color" -> col.asJson)
    ).flatten
    Json.obj(fields*).withTag("type", t.productPrefix)

  given Decoder[Trump] = decodeByTag("type"):
    case "Absent" => Decoder.const(Trump.Absent)
    case "WizardResolved" => Decoder.instance: cursor =>
      for
        card  <- cursor.downField("card").as[Card]
        color <- cursor.downField("color").as[Card.Color]
        wiz   <- card match
          case w: Card.Wizard => Right(w)
          case _ => Left(DecodingFailure("Wizard expected for WizardResolved", cursor.history))
      yield Trump.WizardResolved(wiz, color)
    case "Jester" | "Standard" | "WizardUnresolved" => Decoder.forProduct1("card")(Trump.apply)