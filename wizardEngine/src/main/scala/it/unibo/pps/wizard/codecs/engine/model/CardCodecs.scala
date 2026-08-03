package it.unibo.pps.wizard.codecs.engine.model

import io.circe.*
import io.circe.syntax.*
import it.unibo.pps.wizard.codecs.combinators.DiscriminatedCodecs.*
import it.unibo.pps.wizard.engine.model.basic.cards.Card
import it.unibo.pps.wizard.engine.model.basic.cards.SpecialCard

object CardCodecs:
  given Encoder[Card.Color] = Encoder.encodeString.contramap(_.toString)
  given Decoder[Card.Color] = Decoder.decodeString.emapTry(s => scala.util.Try(Card.Color.valueOf(s)))

  given Encoder[Card.Rank] = Encoder.encodeInt.contramap(_.value)
  given Decoder[Card.Rank] = Decoder.decodeInt.emap: v =>
    Card.Rank.values.find(_.value == v).toRight(s"Invalid rank value: $v")
  
  given Encoder[Card] = Encoder.instance:
    case Card.Standard(c, r) => Json.obj("color" -> c.asJson, "rank" -> r.asJson).withTag("type", "Standard")
    case sc: (SpecialCard & Product) =>
      Json.obj("id" -> sc.id.asJson).withTag("type", sc.productPrefix)

  given Decoder[Card] = decodeByTag("type"):
    case "Standard" => Decoder.forProduct2("color", "rank")(Card.Standard.apply)
    case "Wizard"   => Decoder.forProduct1("id")(Card.Wizard.apply)
    case "Jester"   => Decoder.forProduct1("id")(Card.Jester.apply)