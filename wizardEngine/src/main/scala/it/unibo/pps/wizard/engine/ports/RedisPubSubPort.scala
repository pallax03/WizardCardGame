package it.unibo.pps.wizard.engine.ports

/**
 * Internal port for the Redis Pub/Sub.
 * Single point of Truth of the entire wizard game engine services.
 *
 * GameEngineOutboundPort: subscribe to this, take and publish GameStates.
 *  - every wizardGameEngine Adapter need to be sync with Redis
 * WebSocketsPort: subscribe clients, and serve synced GameStates
 */
trait RedisPubSubPort:
  ???