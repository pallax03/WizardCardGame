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
      |if lobby.status ~= "WAITING" then return "ERR_IN_PROGRESS" end
      |
      |local inputName = ARGV[1]
      |local isBot = ARGV[2] ~= ''
      |local secret = ARGV[4]
      |
      |if not isBot then
      |  if secret ~= '' then
      |    for i, p in ipairs(lobby.players) do
      |      if p.secret == secret then
      |        return cjson.encode(p)
      |      end
      |    end
      |  end
      |end
      |
      |if #lobby.players >= 6 then return "ERR_FULL" end
      |
      |local maxId = -1
      |for i, p in ipairs(lobby.players) do
      |  if p.id > maxId then maxId = p.id end
      |end
      |local newId = maxId + 1
      |
      |local newPlayer = { id = newId, name = inputName }
      |if not isBot then
      |  newPlayer.difficulty = cjson.null
      |  newPlayer.isOnline = false
      |  newPlayer.secret = secret ~= '' and secret or cjson.null
      |else
      |  newPlayer.difficulty = ARGV[2]
      |  newPlayer.name = 'Bot-' .. (newId+1)
      |  newPlayer.isOnline = true
      |  newPlayer.secret = cjson.null
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

  val removePlayerScript: String =
    """
      |local lobbyStr = redis.call('GET', KEYS[1])
      |if not lobbyStr then return 0 end
      |local lobby = cjson.decode(lobbyStr)
      |local targetPlayerId = tonumber(ARGV[1])
      |
      |local newPlayers = {}
      |local found = false
      |for i, p in ipairs(lobby.players) do
      |  if p.id ~= targetPlayerId then
      |    table.insert(newPlayers, p)
      |  else
      |    found = true
      |  end
      |end
      |
      |if not found then return 0 end
      |
      |if #newPlayers == 0 then
      |  redis.call('DEL', KEYS[1])
      |else
      |  lobby.players = newPlayers
      |  redis.call('SET', KEYS[1], cjson.encode(lobby), 'EX', 86400)
      |end
      |return 1
      |""".stripMargin
