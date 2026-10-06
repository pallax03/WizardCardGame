package io.github.pallax03.wizard.engine.adapters.redis

import io.github.pallax03.wizard.util.RedisUtil

/** Redis Lua scripts for atomic distributed coordination. */
private[redis] object RedisLobbyScripts:

  /**
   * Compare-And-Swap (CAS) Lua script for optimistic concurrency control on lobby state.
   *
   * Keys:
   *  - `KEYS[1]`: Redis key for the target lobby (`lobby:<uuid>`).
   *
   * Arguments:
   *  - `ARGV[1]`: Expected revision version (`0` for initial creation).
   *  - `ARGV[2]`: Serialized JSON payload of the updated [[io.github.pallax03.wizard.engine.lobby.Lobby]].
   *
   * Return:
   *  - `1`: Version matched; new state written with default TTL.
   *  - `0`: Version mismatch or state conflict; write rejected.
   */
  val casLobbyScript: String =
    s"""
      |local currentStr = redis.call('GET', KEYS[1])
      |if not currentStr then
      |  if tonumber(ARGV[1]) ~= 0 then return 0 end
      |else
      |  local currentObj = cjson.decode(currentStr)
      |  if currentObj.version ~= tonumber(ARGV[1]) then return 0 end
      |end
      |redis.call('SET', KEYS[1], ARGV[2], 'EX', ${RedisUtil.DEFAULT_TTL})
      |return 1
      |""".stripMargin
