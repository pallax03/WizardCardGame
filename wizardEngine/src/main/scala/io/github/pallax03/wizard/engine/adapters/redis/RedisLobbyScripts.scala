package io.github.pallax03.wizard.engine.adapters.redis

private[redis] object RedisLobbyScripts:

  val addPlayerScript: String =
    """
      |local lobbyStr = redis.call('GET', KEYS[1])
      |local lobby
      |if not lobbyStr then
      |  lobby = { lobbyId = ARGV[3], players = {}, status = "WAITING" }
      |else
      |  lobby = cjson.decode(lobbyStr)
      |end
      |
      |local inputName = ARGV[1]
      |local isBot = ARGV[2] ~= ''
      |
      |if not isBot then
      |  for i, p in ipairs(lobby.players) do
      |    if p.name == inputName and (p.difficulty == nil or p.difficulty == cjson.null) then
      |      if not p.isOnline then
      |        return cjson.encode(p)
      |      else
      |        return nil
      |      end
      |    end
      |  end
      |end
      |
      |if #lobby.players >= 6 then return nil end
      |
      |local counterKey = KEYS[1] .. ':nextId'
      |local newId = 0
      |if redis.call('EXISTS', counterKey) == 0 then
      |  local maxId = -1
      |  for i, p in ipairs(lobby.players) do
      |    if p.id > maxId then maxId = p.id end
      |  end
      |  newId = maxId + 1
      |  redis.call('SET', counterKey, newId + 1, 'EX', 86400)
      |else
      |  newId = tonumber(redis.call('INCR', counterKey)) - 1
      |  redis.call('EXPIRE', counterKey, 86400)
      |end
      |
      |local newPlayer = { id = newId, name = inputName }
      |if not isBot then
      |  newPlayer.difficulty = cjson.null
      |  newPlayer.isOnline = false
      |else
      |  newPlayer.difficulty = ARGV[2]
      |  newPlayer.name = 'Bot-' .. (newId+1)
      |  newPlayer.isOnline = true
      |end
      |
      |table.insert(lobby.players, newPlayer)
      |redis.call('SET', KEYS[1], cjson.encode(lobby), 'EX', 86400)
      |
      |return cjson.encode(newPlayer)
      |""".stripMargin

  val setPlayerOnlineScript: String =
    """
      |local lobbyStr = redis.call('GET', KEYS[1])
      |if not lobbyStr then return 0 end
      |local lobby = cjson.decode(lobbyStr)
      |local targetPlayerId = tonumber(ARGV[1])
      |local isOnline = ARGV[2] == 'true'
      |local found = false
      |for i, player in ipairs(lobby.players) do
      |  if player.id == targetPlayerId then
      |    player.isOnline = isOnline
      |    found = true
      |    break
      |  end
      |end
      |if found then
      |  redis.call('SET', KEYS[1], cjson.encode(lobby), 'EX', 86400)
      |  return 1
      |else
      |  return 0
      |end
      |""".stripMargin
