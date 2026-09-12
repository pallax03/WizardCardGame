package io.github.pallax03.wizard.engine.model.core

import io.github.pallax03.wizard.engine.model.basic.PlayerId
import io.github.pallax03.wizard.engine.model.basic.bidding.Bid
import io.github.pallax03.wizard.engine.model.basic.cards.Card
import io.github.pallax03.wizard.engine.model.basic.gameplay.Round
import io.github.pallax03.wizard.engine.model.events.InvitationEvent

enum CardNotAllowedReasons(val legitCards: List[Card]):
  case CardNotInHand(cards: List[Card]) extends CardNotAllowedReasons(cards)
  case MustFollowColor(requiredColor: Card.Color, cards: List[Card])
      extends CardNotAllowedReasons(cards)

enum GameActionError:
  case NotYourTurn(turnOf: PlayerId)
  case InvalidBid(round: Round, invalidBid: Bid)
  case InvalidAction(invitationEvent: Option[InvitationEvent])
  case CardNotAllowed(reason: CardNotAllowedReasons)
