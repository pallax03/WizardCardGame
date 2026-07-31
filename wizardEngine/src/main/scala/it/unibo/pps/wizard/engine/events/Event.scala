package it.unibo.pps.wizard.engine.events

import scala.reflect.ClassTag
import scala.reflect.classTag

/** Represents an event in the game engine. */
trait Event

object Event:

  /**
   * Returns the address (simple name) of the event type T.
   *
   * @tparam T The type of the event, which must be a subtype of Event.
   * @return The simple name of the event type T.
   */
  def addressOf[T <: Event: ClassTag]: String = classTag[T].runtimeClass.getSimpleName

export Event.*
