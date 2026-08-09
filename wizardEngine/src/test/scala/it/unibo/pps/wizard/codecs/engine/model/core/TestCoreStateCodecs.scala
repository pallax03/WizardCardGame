//package it.unibo.pps.wizard.codecs.engine.model.core
//
////import io.circe.parser._, io.circe.syntax._
//
////import it.unibo.pps.wizard.engine.model._
//
////import org.scalatest.EitherValues._
//import org.scalatest.matchers.should.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//
//class TestCoreStateCodecs extends AnyWordSpec with Matchers:
//
////  import core.CoreState
////  import basic.PlayerId
////  import basic.gameplay.Round
////  import CoreStateCodecs.given
//  "CoreStateCodecs" should:
////    val playersIds = List(PlayerId(1), PlayerId(2))
//    "encode and decode CoreState correctly" in:
////      val core: CoreState = CoreState.initialize(
////        playersIds,
////        Round.start
////      )
////      val jsonString = core.asJson.noSpaces
////      jsonString shouldBe """{"playersIds":[1,2],""}"""
////      decode[CoreState](jsonString).value shouldBe error
//      ???
