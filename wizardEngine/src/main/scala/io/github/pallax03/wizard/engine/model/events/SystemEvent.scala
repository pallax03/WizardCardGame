package io.github.pallax03.wizard.engine.model.events

import io.github.pallax03.wizard.engine.model.basic.PlayerId

case class SystemEvent(playerId: PlayerId, action: String) extends PlayerScoped

object SystemEvent:
  def online(playerId: PlayerId): SystemEvent = SystemEvent(playerId, "online")
  def offline(playerId: PlayerId): SystemEvent = SystemEvent(playerId, "offline")
  def joined(playerId: PlayerId): SystemEvent = SystemEvent(playerId, "joined")
  def left(playerId: PlayerId): SystemEvent = SystemEvent(playerId, "left")
  def configUpdated(playerId: PlayerId): SystemEvent =
    SystemEvent(playerId, "config_updated")
  def paused(playerId: PlayerId): SystemEvent = SystemEvent(playerId, "paused")
  def resumed(playerId: PlayerId): SystemEvent = SystemEvent(playerId, "resumed")
  def afkReplaced(playerId: PlayerId): SystemEvent = SystemEvent(playerId, "afk_replaced")
  def strikesUpdated(playerId: PlayerId, strikes: Int): SystemEvent =
    SystemEvent(playerId, s"strikes_$strikes")