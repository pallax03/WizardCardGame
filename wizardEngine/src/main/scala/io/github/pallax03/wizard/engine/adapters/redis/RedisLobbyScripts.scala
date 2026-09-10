package io.github.pallax03.wizard.engine.adapters.redis

import io.github.pallax03.wizard.util.RedisUtil

private[redis] object RedisLobbyScripts:

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
