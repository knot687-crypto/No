--!nonstrict
--[[
	G E N E S I S  ·  Config

	Keeps every player's Genesis settings between visits. Put this Script in
	ServerScriptService; the menu finds it on its own.

	To test saving in Studio, turn on Game Settings → Security →
	Enable Studio Access to API Services.
]]

local DataStoreService = game:GetService("DataStoreService")
local HttpService = game:GetService("HttpService")
local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")

local REMOTE_NAME = "GenesisConfig" -- must match CONFIG.RemoteName in the LocalScript
local STORE_NAME = "GenesisInterface"
local MAX_PAYLOAD = 4000 -- characters of JSON; a full config is well under 1000
local WRITE_GAP = 6 -- seconds between DataStore writes for one player

local store = DataStoreService:GetDataStore(STORE_NAME)

local remote = ReplicatedStorage:FindFirstChild(REMOTE_NAME)
if not remote then
	remote = Instance.new("RemoteFunction")
	remote.Name = REMOTE_NAME
	remote.Parent = ReplicatedStorage
end

local loaded = {} -- [Player] = JSON string, "" when nothing is stored yet
local loading = {} -- [Player] = true while the first read is in flight
local pending = {} -- [Player] = newest JSON waiting for its write slot
local lastWrite = {} -- [Player] = os.clock() of the last write
local warned = false

local function keyFor(player)
	return "settings_" .. player.UserId
end

local function attempt(tries, callback)
	for index = 1, tries do
		local ok, result = pcall(callback)
		if ok then
			return true, result
		end
		if not warned then
			warned = true
			warn("[GenesisConfig] DataStore unavailable, settings will not persist: " .. tostring(result))
		end
		if index < tries then
			task.wait(index * 2)
		end
	end
	return false, nil
end

local function read(player)
	if loaded[player] ~= nil then
		return loaded[player]
	end
	if loading[player] then
		while loading[player] do
			task.wait(0.1)
		end
		return loaded[player] or ""
	end
	loading[player] = true
	local ok, value = attempt(2, function()
		return store:GetAsync(keyFor(player))
	end)
	loading[player] = nil
	if not player.Parent then
		return ""
	end
	loaded[player] = if ok and type(value) == "string" then value else ""
	return loaded[player]
end

local function write(player, payload)
	lastWrite[player] = os.clock()
	local ok = attempt(3, function()
		store:SetAsync(keyFor(player), payload, { player.UserId })
	end)
	return ok
end

-- Writes now if the player's write slot is free; otherwise keeps only the
-- newest payload and writes it once the gap has passed.
local function queue(player, payload)
	loaded[player] = payload
	local wait = WRITE_GAP - (os.clock() - (lastWrite[player] or -math.huge))
	if wait <= 0 then
		pending[player] = nil
		return write(player, payload)
	end
	local scheduled = pending[player] ~= nil
	pending[player] = payload
	if not scheduled then
		task.delay(wait, function()
			local latest = pending[player]
			if latest and player.Parent then
				pending[player] = nil
				write(player, latest)
			end
		end)
	end
	return true
end

local function valid(payload)
	if type(payload) ~= "string" or #payload == 0 or #payload > MAX_PAYLOAD then
		return false
	end
	local ok, decoded = pcall(function()
		return HttpService:JSONDecode(payload)
	end)
	return ok and type(decoded) == "table"
end

remote.OnServerInvoke = function(player, action, payload)
	if action == "load" then
		return read(player)
	elseif action == "save" and valid(payload) then
		return queue(player, payload)
	end
	return false
end

Players.PlayerRemoving:Connect(function(player)
	local latest = pending[player]
	pending[player] = nil
	if latest then
		write(player, latest)
	end
	loaded[player], loading[player], lastWrite[player] = nil, nil, nil
end)

game:BindToClose(function()
	local writing = 0
	for player, payload in pending do
		writing += 1
		task.spawn(function()
			write(player, payload)
			writing -= 1
		end)
	end
	table.clear(pending)
	local deadline = os.clock() + 25
	while writing > 0 and os.clock() < deadline do
		task.wait(0.1)
	end
end)
