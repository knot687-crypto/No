--!nonstrict
--[[
	G E N E S I S  ·  Glass Interface

	A frosted-glass menu for Roblox experiences, layered over custom artwork.
	Every panel is real glass: it samples the artwork sitting behind it, frosts
	it, tints it, and catches light along its rim.

	Install
	  1  Upload assets/background.png and assets/background-frost.png as images
	     (Creator Dashboard → Development Items → Images, or Studio's Asset Manager).
	  2  Paste both ids into CONFIG below.
	  3  Put this LocalScript in StarterPlayer › StarterPlayerScripts.
	  4  Optional: put GenesisConfig.server.lua in ServerScriptService so every
	     setting follows the player between visits.
]]

local CONFIG = {
	Title = "Genesis",
	Tagline = "Glass Interface",

	-- Image ids for the artwork. Leave rbxassetid://0 to run without it.
	Artwork = "rbxassetid://0",
	ArtworkFrost = "rbxassetid://0",
	-- Pixel size of the uploaded artwork (both files share it).
	ArtworkSize = Vector2.new(1024, 558),
	-- Where the two fingertips meet, in artwork pixels. The living glow sits here.
	ArtworkFocus = Vector2.new(505, 276),

	OpenOnStart = true,
	DisplayOrder = 50,

	-- RemoteFunction created by GenesisConfig.server.lua.
	RemoteName = "GenesisConfig",
}

--------------------------------------------------------------------------------
-- Services
--------------------------------------------------------------------------------

local Players = game:GetService("Players")
local RunService = game:GetService("RunService")
local TweenService = game:GetService("TweenService")
local UserInputService = game:GetService("UserInputService")
local GuiService = game:GetService("GuiService")
local Lighting = game:GetService("Lighting")
local HttpService = game:GetService("HttpService")
local ReplicatedStorage = game:GetService("ReplicatedStorage")

local player = Players.LocalPlayer
local playerGui = player:WaitForChild("PlayerGui")

--------------------------------------------------------------------------------
-- Design tokens
--------------------------------------------------------------------------------

local WINDOW = Vector2.new(880, 480)
local INSET = 10
local BAR = 52
local CONTENT = Vector2.new(WINDOW.X - INSET * 2, WINDOW.Y - INSET * 3 - BAR)
local COLLAPSED = INSET * 2 + BAR
local TAB_WIDTH = 104
local RADIUS = { window = 22, panel = 16, card = 14, control = 10, small = 8 }

local WHITE = Color3.new(1, 1, 1)
local BLACK = Color3.new(0, 0, 0)
local INK = Color3.fromRGB(244, 245, 250)
local TINT = Color3.fromRGB(8, 8, 11)
local SURFACE = Color3.fromRGB(14, 14, 18)
local GREEN = Color3.fromRGB(88, 214, 141)
local AMBER = Color3.fromRGB(255, 190, 96)
local RED = Color3.fromRGB(255, 108, 112)

-- Supporting text is INK at these transparencies.
local TEXT = { secondary = 0.42, tertiary = 0.58 }

local ACCENTS = {
	{ name = "Platinum", color = Color3.fromRGB(236, 238, 245) },
	{ name = "Ice", color = Color3.fromRGB(128, 192, 255) },
	{ name = "Iris", color = Color3.fromRGB(156, 138, 255) },
	{ name = "Rose", color = Color3.fromRGB(255, 124, 158) },
	{ name = "Amber", color = Color3.fromRGB(255, 194, 104) },
	{ name = "Jade", color = Color3.fromRGB(96, 226, 174) },
}

local TYPEFACES = {
	{ name = "Builder Sans", family = "rbxasset://fonts/families/BuilderSans.json" },
	{ name = "Gotham", family = "rbxasset://fonts/families/GothamSSm.json" },
	{ name = "Montserrat", family = "rbxasset://fonts/families/Montserrat.json" },
}

local CORNER_SCALE = { Sharp = 0.4, Soft = 1, Round = 1.5 }
local MOTION_SCALE = { Off = 0, Smooth = 1, Fluid = 1.45 }

local TONES = {
	{ name = "Natural", saturation = 0, contrast = 0, brightness = 0, tint = WHITE },
	{ name = "Noir", saturation = -1, contrast = 0.2, brightness = -0.02, tint = WHITE },
	{ name = "Warm", saturation = 0.06, contrast = 0.05, brightness = 0.01, tint = Color3.fromRGB(255, 238, 216) },
	{ name = "Cool", saturation = 0, contrast = 0.06, brightness = 0, tint = Color3.fromRGB(216, 232, 255) },
	{ name = "Vivid", saturation = 0.35, contrast = 0.12, brightness = 0.02, tint = WHITE },
}

local function names(list)
	local result = {}
	for index, item in list do
		result[index] = item.name
	end
	return result
end

local function byName(list)
	local result = {}
	for _, item in list do
		result[item.name] = item
	end
	return result
end

local ACCENT = byName(ACCENTS)
local TYPEFACE = byName(TYPEFACES)
local TONE = byName(TONES)

--------------------------------------------------------------------------------
-- Settings
--------------------------------------------------------------------------------

local DEFAULTS = {
	accent = "Platinum",
	typeface = "Builder Sans",
	corners = "Soft",
	scale = 100,
	glass = 36,
	frost = 85,
	artwork = 100,
	edgeLight = true,

	menuKey = "RightShift",
	motion = "Smooth",
	focusBlur = true,
	blurSize = 12,
	ambient = true,
	notifications = true,
	dock = true,
	autosave = true,

	fov = math.floor(workspace.CurrentCamera.FieldOfView + 0.5),
	zoomKey = "Z",
	zoomFov = 30,
	crosshair = "None",
	perfOverlay = false,

	bloom = false,
	sunRays = false,
	depthOfField = false,
	tone = "Natural",
	saturation = 0,
	contrast = 0,
}

local RANGES = {
	scale = { 80, 125 },
	glass = { 0, 100 },
	frost = { 0, 100 },
	artwork = { 20, 100 },
	blurSize = { 2, 24 },
	fov = { 40, 120 },
	zoomFov = { 10, 50 },
	saturation = { -100, 100 },
	contrast = { -100, 100 },
}

local CHOICES = {
	accent = names(ACCENTS),
	typeface = names(TYPEFACES),
	corners = { "Sharp", "Soft", "Round" },
	motion = { "Off", "Smooth", "Fluid" },
	crosshair = { "None", "Dot", "Cross", "Circle" },
	tone = names(TONES),
}

local Settings = {}
do
	local values = table.clone(DEFAULTS)
	local watchers = {}
	local listeners = {}

	local function run(callback, ...)
		local ok, problem = pcall(callback, ...)
		if not ok then
			warn("[Genesis] " .. tostring(problem))
		end
	end

	function Settings.get(key)
		return values[key]
	end

	function Settings.set(key, value)
		if values[key] == value then
			return
		end
		values[key] = value
		for _, callback in watchers[key] or {} do
			run(callback, value)
		end
		for _, callback in listeners do
			run(callback, key, value)
		end
	end

	-- Calls back on every change.
	function Settings.listen(key, callback)
		watchers[key] = watchers[key] or {}
		table.insert(watchers[key], callback)
	end

	-- Calls back with the current value now, then on every change.
	function Settings.watch(key, callback)
		Settings.listen(key, callback)
		run(callback, values[key])
	end

	function Settings.changed(callback)
		table.insert(listeners, callback)
	end

	-- Validates a value that came back from storage.
	function Settings.sanitize(key, value)
		local default = DEFAULTS[key]
		if default == nil or typeof(value) ~= typeof(default) then
			return nil
		end
		if type(value) == "number" then
			if value ~= value then
				return nil
			end
			local range = RANGES[key]
			return if range then math.clamp(value, range[1], range[2]) else value
		end
		if CHOICES[key] and not table.find(CHOICES[key], value) then
			return nil
		end
		if key == "menuKey" or key == "zoomKey" then
			local ok = pcall(function()
				return Enum.KeyCode[value]
			end)
			return if ok then value else nil
		end
		return value
	end
end

--------------------------------------------------------------------------------
-- Theme and motion
--------------------------------------------------------------------------------

local function accent()
	local entry = ACCENT[Settings.get("accent")]
	return if entry then entry.color else ACCENTS[1].color
end

-- Ink that stays readable on top of a colour.
local function inkOn(color)
	local luminance = 0.2126 * color.R + 0.7152 * color.G + 0.0722 * color.B
	return if luminance > 0.62 then Color3.fromRGB(14, 14, 18) else WHITE
end

local function family()
	local entry = TYPEFACE[Settings.get("typeface")]
	return if entry then entry.family else TYPEFACES[1].family
end

local function duration(seconds)
	return seconds * (MOTION_SCALE[Settings.get("motion")] or 1)
end

local function tween(object, seconds, goal, style, direction)
	local time = duration(seconds)
	if time <= 0 then
		for property, value in goal do
			object[property] = value
		end
		return nil
	end
	local track = TweenService:Create(
		object,
		TweenInfo.new(time, style or Enum.EasingStyle.Quint, direction or Enum.EasingDirection.Out),
		goal
	)
	track:Play()
	return track
end

local themeHooks = {}

-- Runs now and again whenever the accent changes.
local function themed(callback)
	table.insert(themeHooks, callback)
	callback(false)
end

-- Live-themed objects. These are strong tables on purpose: an Instance's Lua
-- handle can be collected while the Instance itself is still on screen, which
-- would silently drop it from a weak table. Short-lived UI calls forget().
local fontRegistry = {}
local cornerRegistry = {}

--------------------------------------------------------------------------------
-- Building blocks
--------------------------------------------------------------------------------

local CLASS_DEFAULTS = {
	Frame = { BorderSizePixel = 0, BackgroundTransparency = 1 },
	ImageLabel = { BorderSizePixel = 0, BackgroundTransparency = 1 },
	ScrollingFrame = { BorderSizePixel = 0, BackgroundTransparency = 1 },
	CanvasGroup = { BorderSizePixel = 0, BackgroundTransparency = 1 },
	TextLabel = {
		BorderSizePixel = 0,
		BackgroundTransparency = 1,
		TextColor3 = INK,
		TextSize = 14,
		TextXAlignment = Enum.TextXAlignment.Left,
	},
	TextButton = {
		BorderSizePixel = 0,
		BackgroundTransparency = 1,
		TextColor3 = INK,
		TextSize = 14,
		AutoButtonColor = false,
		Text = "",
	},
	TextBox = {
		BorderSizePixel = 0,
		BackgroundTransparency = 1,
		TextColor3 = INK,
		TextSize = 14,
		ClearTextOnFocus = false,
		TextXAlignment = Enum.TextXAlignment.Left,
	},
}

local TEXT_CLASSES = { TextLabel = true, TextButton = true, TextBox = true }

local function setFont(object, weight)
	fontRegistry[object] = weight
	object.FontFace = Font.new(family(), weight)
end

-- Instance.new with defaults, properties and a font weight in one call.
local function new(className, properties)
	local object = Instance.new(className)
	for property, value in CLASS_DEFAULTS[className] or {} do
		object[property] = value
	end
	local parent, weight = nil, nil
	for property, value in properties do
		if property == "Parent" then
			parent = value
		elseif property == "weight" then
			weight = value
		else
			object[property] = value
		end
	end
	if TEXT_CLASSES[className] then
		setFont(object, weight or Enum.FontWeight.Regular)
	end
	if parent then
		object.Parent = parent
	end
	return object
end

-- Forgets a short-lived piece of UI (a toast, an open list) before it is destroyed.
local function forget(root)
	fontRegistry[root] = nil
	cornerRegistry[root] = nil
	for _, descendant in root:GetDescendants() do
		fontRegistry[descendant] = nil
		cornerRegistry[descendant] = nil
	end
end

local function radiusFor(base, extra)
	return math.floor(base * (CORNER_SCALE[Settings.get("corners")] or 1) + (extra or 0) + 0.5)
end

-- Rounded corners that follow the Corners setting. "full" makes a pill.
local function corner(parent, radius, extra)
	local object = Instance.new("UICorner")
	if radius == "full" then
		object.CornerRadius = UDim.new(1, 0)
	else
		cornerRegistry[object] = { radius, extra or 0 }
		object.CornerRadius = UDim.new(0, radiusFor(radius, extra))
	end
	object.Parent = parent
	return object
end

local function stroke(parent, transparency, color, thickness)
	return new("UIStroke", {
		ApplyStrokeMode = Enum.ApplyStrokeMode.Border,
		Color = color or WHITE,
		Transparency = transparency or 0.9,
		Thickness = thickness or 1,
		Parent = parent,
	})
end

local function numbers(points)
	local keypoints = table.create(#points)
	for index, point in points do
		keypoints[index] = NumberSequenceKeypoint.new(point[1], point[2])
	end
	return NumberSequence.new(keypoints)
end

local function colors(points)
	local keypoints = table.create(#points)
	for index, point in points do
		keypoints[index] = ColorSequenceKeypoint.new(point[1], point[2])
	end
	return ColorSequence.new(keypoints)
end

local function gradient(parent, properties)
	properties.Parent = parent
	return new("UIGradient", properties)
end

local function padding(parent, top, right, bottom, left)
	return new("UIPadding", {
		PaddingTop = UDim.new(0, top),
		PaddingRight = UDim.new(0, right or top),
		PaddingBottom = UDim.new(0, bottom or top),
		PaddingLeft = UDim.new(0, left or right or top),
		Parent = parent,
	})
end

local function list(parent, direction, gap, horizontal, vertical)
	return new("UIListLayout", {
		FillDirection = direction or Enum.FillDirection.Vertical,
		Padding = UDim.new(0, gap or 0),
		SortOrder = Enum.SortOrder.LayoutOrder,
		HorizontalAlignment = horizontal or Enum.HorizontalAlignment.Left,
		VerticalAlignment = vertical or Enum.VerticalAlignment.Top,
		Parent = parent,
	})
end

local orderCounter = 0
local function nextOrder()
	orderCounter += 1
	return orderCounter
end

-- Light that falls from the top edge of a stroke: bright rim, dim sides.
local function rimLight(strokeObject, strength)
	strength = strength or 1
	return gradient(strokeObject, {
		Rotation = 90,
		Transparency = numbers({
			{ 0, 0 },
			{ 0.22, 1 - 0.45 * strength },
			{ 0.7, 1 - 0.25 * strength },
			{ 1, 1 - 0.5 * strength },
		}),
	})
end

-- Many faint, growing layers read as one soft shadow, with no image asset.
-- Keep it to large surfaces: on small controls the steps become visible.
local function softShadow(parent, radius, spread, opacity, offsetY, layers)
	local holder = new("Frame", { Name = "Shadow", Size = UDim2.fromScale(1, 1), ZIndex = 0, Parent = parent })
	for index = 1, layers do
		local grow = spread * index / layers
		local layer = new("Frame", {
			AnchorPoint = Vector2.new(0.5, 0.5),
			Position = UDim2.new(0.5, 0, 0.5, offsetY),
			Size = UDim2.new(1, grow * 2, 1, grow * 2),
			BackgroundColor3 = BLACK,
			BackgroundTransparency = 1 - opacity / layers,
			Parent = holder,
		})
		corner(layer, radius, grow)
	end
	return holder
end

local function pressable(button)
	local scale = new("UIScale", { Parent = button })
	local function release()
		tween(scale, 0.35, { Scale = 1 }, Enum.EasingStyle.Back)
	end
	button.MouseButton1Down:Connect(function()
		tween(scale, 0.12, { Scale = 0.95 })
	end)
	button.MouseButton1Up:Connect(release)
	button.MouseLeave:Connect(release)
	return scale
end

local function prettyKey(name)
	local special = {
		LeftShift = "Left Shift",
		RightShift = "Right Shift",
		LeftControl = "Left Ctrl",
		RightControl = "Right Ctrl",
		LeftAlt = "Left Alt",
		RightAlt = "Right Alt",
		Return = "Enter",
		Backquote = "`",
	}
	return special[name] or (name:gsub("(%l)(%u)", "%1 %2"))
end

local function pointer(input)
	if input and input.UserInputType == Enum.UserInputType.Touch then
		local inset = GuiService:GetGuiInset()
		return Vector2.new(input.Position.X + inset.X, input.Position.Y + inset.Y)
	end
	return UserInputService:GetMouseLocation()
end

local function isPress(input)
	return input.UserInputType == Enum.UserInputType.MouseButton1 or input.UserInputType == Enum.UserInputType.Touch
end

local function isMove(input)
	return input.UserInputType == Enum.UserInputType.MouseMovement or input.UserInputType == Enum.UserInputType.Touch
end

local function inside(object, point)
	local position, size = object.AbsolutePosition, object.AbsoluteSize
	return point.X >= position.X
		and point.Y >= position.Y
		and point.X <= position.X + size.X
		and point.Y <= position.Y + size.Y
end

--------------------------------------------------------------------------------
-- Glyphs: crisp vector icons drawn from rounded frames
--------------------------------------------------------------------------------

local function glyph(parent, name, size, properties)
	local canvas = new("Frame", { Size = UDim2.fromOffset(size, size), Parent = parent })
	for property, value in properties or {} do
		canvas[property] = value
	end
	local fills, outlines = {}, {}
	local unit = 1 / 16

	local function solid(object)
		object.BackgroundColor3 = WHITE
		object.BackgroundTransparency = 0
		table.insert(fills, object)
		return object
	end

	local function dot(x, y, d)
		local object = solid(new("Frame", {
			AnchorPoint = Vector2.new(0.5, 0.5),
			Position = UDim2.fromScale(x * unit, y * unit),
			Size = UDim2.fromScale(d * unit, d * unit),
			Parent = canvas,
		}))
		corner(object, "full")
	end

	local function box(x, y, w, h, r)
		local object = solid(new("Frame", {
			Position = UDim2.fromScale(x * unit, y * unit),
			Size = UDim2.fromScale(w * unit, h * unit),
			Parent = canvas,
		}))
		new("UICorner", { CornerRadius = UDim.new(0, math.floor(r * size * unit + 0.5)), Parent = object })
	end

	local function line(x1, y1, x2, y2, w)
		w = w or 1.5
		local dx, dy = x2 - x1, y2 - y1
		local object = solid(new("Frame", {
			AnchorPoint = Vector2.new(0.5, 0.5),
			Position = UDim2.fromScale((x1 + x2) * 0.5 * unit, (y1 + y2) * 0.5 * unit),
			Size = UDim2.fromScale((math.sqrt(dx * dx + dy * dy) + w) * unit, w * unit),
			Rotation = math.deg(math.atan2(dy, dx)),
			Parent = canvas,
		}))
		corner(object, "full")
	end

	-- Outline described by its outer bounds; the stroke is drawn outside the frame.
	local function outline(x, y, w, h, r, thickness)
		thickness = thickness or 1.5
		local object = new("Frame", {
			Position = UDim2.fromScale((x + thickness) * unit, (y + thickness) * unit),
			Size = UDim2.fromScale((w - thickness * 2) * unit, (h - thickness * 2) * unit),
			Parent = canvas,
		})
		new("UICorner", {
			CornerRadius = if r == "full"
				then UDim.new(1, 0)
				else UDim.new(0, math.floor(math.max(r - thickness, 0) * size * unit + 0.5)),
			Parent = object,
		})
		table.insert(outlines, stroke(object, 0, WHITE, thickness * size * unit))
	end

	local function ring(x, y, d, thickness)
		outline(x - d / 2, y - d / 2, d, d, "full", thickness)
	end

	if name == "home" then
		box(1.6, 1.6, 5.6, 5.6, 1.8)
		box(8.8, 1.6, 5.6, 5.6, 1.8)
		box(1.6, 8.8, 5.6, 5.6, 1.8)
		box(8.8, 8.8, 5.6, 5.6, 1.8)
	elseif name == "gameplay" then
		outline(0.8, 3.6, 14.4, 8.8, 4.4, 1.5)
		line(4.1, 8, 7.1, 8, 1.4)
		line(5.6, 6.5, 5.6, 9.5, 1.4)
		dot(10.2, 7.1, 1.9)
		dot(11.9, 8.9, 1.9)
	elseif name == "visuals" then
		ring(8, 8, 13.6, 1.5)
		dot(8, 8, 8.6)
		local half = fills[#fills]
		gradient(half, { Transparency = numbers({ { 0, 0 }, { 0.5, 0 }, { 0.501, 1 }, { 1, 1 } }) })
	elseif name == "settings" then
		line(2, 4, 14, 4, 1.4)
		line(2, 8, 14, 8, 1.4)
		line(2, 12, 14, 12, 1.4)
		dot(10.5, 4, 4)
		dot(5.5, 8, 4)
		dot(9, 12, 4)
	elseif name == "close" then
		line(4.5, 4.5, 11.5, 11.5, 1.6)
		line(11.5, 4.5, 4.5, 11.5, 1.6)
	elseif name == "minimize" then
		line(4, 8, 12, 8, 1.6)
	elseif name == "restore" then
		outline(3.6, 3.6, 8.8, 8.8, 2.6, 1.5)
	elseif name == "chevron" then
		line(4.5, 6.5, 8, 10, 1.5)
		line(8, 10, 11.5, 6.5, 1.5)
	elseif name == "check" then
		line(3.5, 8.5, 6.5, 11.5, 1.7)
		line(6.5, 11.5, 12.5, 4.5, 1.7)
	elseif name == "info" then
		ring(8, 8, 14, 1.4)
		dot(8, 5, 1.9)
		line(8, 7.6, 8, 11.2, 1.6)
	elseif name == "pulse" then
		line(1, 8.5, 4.5, 8.5, 1.5)
		line(4.5, 8.5, 6.5, 3.5, 1.5)
		line(6.5, 3.5, 9.5, 12.5, 1.5)
		line(9.5, 12.5, 11.5, 8.5, 1.5)
		line(11.5, 8.5, 15, 8.5, 1.5)
	elseif name == "signal" then
		box(2, 10, 2.8, 4, 1.2)
		box(6.6, 6.5, 2.8, 7.5, 1.2)
		box(11.2, 2.5, 2.8, 11.5, 1.2)
	elseif name == "people" then
		dot(8, 5.1, 5.4)
		box(3.2, 9.6, 9.6, 5.4, 2.7)
	elseif name == "clock" then
		ring(8, 8, 14, 1.5)
		line(8, 8, 8, 4.4, 1.5)
		line(8, 8, 10.6, 9.6, 1.5)
	elseif name == "sparkle" then
		-- Stepped tapers read as a four-point star with concave sides.
		for _, arm in { { 6.6, 0.9 }, { 4.6, 1.7 }, { 3, 2.6 }, { 1.8, 3.6 } } do
			line(8, 8 - arm[1], 8, 8 + arm[1], arm[2])
			line(8 - arm[1], 8, 8 + arm[1], 8, arm[2])
		end
	end

	local api = { Frame = canvas }
	function api.paint(color, transparency, seconds)
		for _, object in fills do
			tween(object, seconds or 0, { BackgroundColor3 = color, BackgroundTransparency = transparency })
		end
		for _, object in outlines do
			tween(object, seconds or 0, { Color = color, Transparency = transparency })
		end
	end
	return api
end

--------------------------------------------------------------------------------
-- Artwork sampling: glass shows exactly the slice of art that sits behind it
--------------------------------------------------------------------------------

local function hasAsset(id)
	return type(id) == "string" and id ~= "" and id ~= "rbxassetid://0"
end

local HAS_ART = hasAsset(CONFIG.Artwork)
local HAS_FROST = hasAsset(CONFIG.ArtworkFrost)

local artRoot -- the window's full-size artwork, shown with ScaleType.Crop
local sampled = {}
local frostLayers = {}
local tintLayers = {}
local rimStrokes = {}
local rimLines = {}

local function artShade()
	local value = Settings.get("artwork") / 100
	return Color3.new(value, value, value)
end

-- Screen position of an artwork pixel inside the window.
local function artworkPoint(point)
	local position, size = artRoot.AbsolutePosition, artRoot.AbsoluteSize
	local source = CONFIG.ArtworkSize
	local scale = math.max(size.X / source.X, size.Y / source.Y)
	return Vector2.new(
		position.X + (size.X - source.X * scale) * 0.5 + point.X * scale,
		position.Y + (size.Y - source.Y * scale) * 0.5 + point.Y * scale
	)
end

local function sample(layer)
	if not artRoot then
		return
	end
	local rootSize, layerSize = artRoot.AbsoluteSize, layer.AbsoluteSize
	if rootSize.X < 1 or rootSize.Y < 1 or layerSize.X < 1 or layerSize.Y < 1 then
		return
	end
	-- The rect is never clamped to the image: clamping would stretch a pane that is
	-- partly scrolled past the window, while the overhang simply stays dark.
	local scale = math.max(rootSize.X / CONFIG.ArtworkSize.X, rootSize.Y / CONFIG.ArtworkSize.Y)
	local origin = artworkPoint(Vector2.zero)
	layer.ImageRectOffset = (layer.AbsolutePosition - origin) / scale
	layer.ImageRectSize = layerSize / scale
end

local function resampleAll()
	for layer in sampled do
		sample(layer)
	end
end

-- An ImageLabel that always shows the artwork directly behind it.
local function artLayer(parent, image, properties)
	local layer = new("ImageLabel", {
		Name = "Art",
		Size = UDim2.fromScale(1, 1),
		Image = image,
		ImageColor3 = artShade(),
		Parent = parent,
	})
	for property, value in properties or {} do
		layer[property] = value
	end
	sampled[layer] = true
	layer:GetPropertyChangedSignal("AbsolutePosition"):Connect(function()
		sample(layer)
	end)
	layer:GetPropertyChangedSignal("AbsoluteSize"):Connect(function()
		sample(layer)
	end)
	sample(layer)
	return layer
end

-- Keeps an anchored object over one artwork pixel, inside any container.
local function pinToArtwork(object, container, point)
	local function update()
		local size = container.AbsoluteSize
		if not artRoot or size.X < 1 or size.Y < 1 then
			return
		end
		local relative = (artworkPoint(point) - container.AbsolutePosition) / size
		object.Position = UDim2.fromScale(relative.X, relative.Y)
	end
	for _, source in { container, artRoot } do
		source:GetPropertyChangedSignal("AbsolutePosition"):Connect(update)
		source:GetPropertyChangedSignal("AbsoluteSize"):Connect(update)
	end
	update()
end

-- The glass recipe: frosted art, tint and a sheen, stacked from zIndex upward.
local function glassStack(parent, radius, zIndex)
	local frost = artLayer(parent, if HAS_FROST then CONFIG.ArtworkFrost else "", {
		Name = "Frost",
		ImageTransparency = 1 - Settings.get("frost") / 100,
		Visible = HAS_FROST,
		ZIndex = zIndex,
	})
	corner(frost, radius)
	frostLayers[frost] = true

	local tint = new("Frame", {
		Name = "Tint",
		Size = UDim2.fromScale(1, 1),
		BackgroundColor3 = TINT,
		BackgroundTransparency = 1 - Settings.get("glass") / 100,
		ZIndex = zIndex + 1,
		Parent = parent,
	})
	corner(tint, radius)
	tintLayers[tint] = true

	local sheen = new("Frame", {
		Name = "Sheen",
		Size = UDim2.fromScale(1, 1),
		BackgroundColor3 = WHITE,
		BackgroundTransparency = 0,
		ZIndex = zIndex + 2,
		Parent = parent,
	})
	corner(sheen, radius)
	gradient(sheen, {
		Rotation = 58,
		Transparency = numbers({ { 0, 0.93 }, { 0.3, 0.976 }, { 0.58, 1 }, { 1, 1 } }),
	})
end

-- A pane of glass with a rim light and a specular line along its top edge.
local function glassPanel(parent, name, position, size, radius, zIndex)
	local panel = new("Frame", { Name = name, Position = position, Size = size, ZIndex = zIndex, Parent = parent })
	glassStack(panel, radius, 1)

	local content = new("Frame", { Name = "Content", Size = UDim2.fromScale(1, 1), ZIndex = 4, Parent = panel })

	local rim = new("Frame", { Name = "Rim", Size = UDim2.fromScale(1, 1), ZIndex = 5, Parent = panel })
	corner(rim, radius)
	local rimStroke = stroke(rim, 0.8)
	rimLight(rimStroke)
	rimStrokes[rimStroke] = true

	local highlight = new("Frame", {
		Name = "Specular",
		Position = UDim2.fromOffset(18, 1),
		Size = UDim2.new(1, -36, 0, 1),
		BackgroundColor3 = WHITE,
		BackgroundTransparency = 0,
		ZIndex = 5,
		Parent = panel,
	})
	gradient(highlight, { Transparency = numbers({ { 0, 1 }, { 0.3, 0.8 }, { 0.7, 0.8 }, { 1, 1 } }) })
	rimLines[highlight] = true

	return { Frame = panel, Content = content, radius = radius }
end

-- A strip of the same glass laid over scrolling content so it dissolves at
-- the edge instead of being cut.
local function glassFade(panel, panelSize, edge, height)
	local top = edge == "top"
	local group = new("CanvasGroup", {
		Name = if top then "FadeTop" else "FadeBottom",
		AnchorPoint = Vector2.new(0, if top then 0 else 1),
		Position = UDim2.fromScale(0, if top then 0 else 1),
		Size = UDim2.new(1, 0, 0, height),
		ZIndex = 20,
		Parent = panel.Content,
	})
	corner(group, panel.radius)
	gradient(group, {
		Rotation = 90,
		Transparency = if top then numbers({ { 0, 0 }, { 1, 1 } }) else numbers({ { 0, 1 }, { 1, 0 } }),
	})
	local replica = new("Frame", {
		Position = UDim2.fromOffset(0, if top then 0 else height - panelSize.Y),
		Size = UDim2.fromOffset(panelSize.X, panelSize.Y),
		BackgroundColor3 = BLACK,
		BackgroundTransparency = 0,
		Parent = group,
	})
	if HAS_ART then
		artLayer(replica, CONFIG.Artwork, { ZIndex = 1 })
	end
	glassStack(replica, panel.radius, 2)
	return group
end

--------------------------------------------------------------------------------
-- Screen layers
--------------------------------------------------------------------------------

local gui = new("ScreenGui", {
	Name = "Genesis",
	ResetOnSpawn = false,
	IgnoreGuiInset = true,
	ZIndexBehavior = Enum.ZIndexBehavior.Sibling,
	DisplayOrder = CONFIG.DisplayOrder,
})

local hud = new("Frame", { Name = "Hud", Size = UDim2.fromScale(1, 1), ZIndex = 1, Parent = gui })
local backdrop = new("Frame", {
	Name = "Backdrop",
	Size = UDim2.fromScale(1, 1),
	BackgroundColor3 = BLACK,
	BackgroundTransparency = 1,
	ZIndex = 2,
	Parent = gui,
})
local stage = new("Frame", { Name = "Stage", Size = UDim2.fromScale(1, 1), ZIndex = 3, Parent = gui })
-- The window passes through this group only while it fades, so it stays razor sharp at rest.
local fader =
	new("CanvasGroup", { Name = "Fader", Size = UDim2.fromScale(1, 1), ZIndex = 3, Visible = false, Parent = gui })
local toastLayer = new("Frame", {
	Name = "Toasts",
	AnchorPoint = Vector2.new(1, 1),
	Position = UDim2.new(1, -18, 1, -18),
	Size = UDim2.fromOffset(332, 420),
	ZIndex = 6,
	Parent = gui,
})
list(toastLayer, Enum.FillDirection.Vertical, 0, Enum.HorizontalAlignment.Right, Enum.VerticalAlignment.Bottom)
-- Toasts and the dock shrink with the window on small screens.
local compactScales = { new("UIScale", { Parent = toastLayer }) }

--------------------------------------------------------------------------------
-- Window
--------------------------------------------------------------------------------

local Menu = { open = false, collapsed = false, page = nil, fadeToken = 0, pageToken = 0, collapseToken = 0 }
local Overlay = {}
local notify -- defined with the toasts

local host = new("Frame", {
	Name = "Window",
	AnchorPoint = Vector2.new(0.5, 0.5),
	Position = UDim2.fromScale(0.5, 0.5),
	Size = UDim2.fromOffset(WINDOW.X, WINDOW.Y),
	Visible = false,
	Parent = stage,
})
local hostScale = new("UIScale", { Parent = host })
-- A modal button frees the mouse in first person while the menu is up.
new("TextButton", { Name = "Modal", Size = UDim2.fromOffset(0, 0), Modal = true, Parent = host })

local body = new("Frame", {
	Name = "Body",
	AnchorPoint = Vector2.new(0.5, 0.5),
	Position = UDim2.fromScale(0.5, 0.5),
	Size = UDim2.fromScale(1, 1),
	Parent = host,
})
local bodyScale = new("UIScale", { Parent = body })

local shell = new("Frame", { Name = "Shell", Size = UDim2.fromScale(1, 1), Parent = body })
softShadow(shell, RADIUS.window, 44, 0.6, 18, 12)

local window = new("Frame", {
	Name = "Glass",
	Size = UDim2.fromScale(1, 1),
	BackgroundColor3 = BLACK,
	BackgroundTransparency = 0,
	ClipsDescendants = true,
	ZIndex = 1,
	Parent = shell,
})
corner(window, RADIUS.window)

artRoot = new("ImageLabel", {
	Name = "Artwork",
	Size = UDim2.fromScale(1, 1),
	Image = if HAS_ART then CONFIG.Artwork else "",
	ImageColor3 = artShade(),
	ScaleType = Enum.ScaleType.Crop,
	ZIndex = 1,
	Parent = window,
})
corner(artRoot, RADIUS.window)
artRoot:GetPropertyChangedSignal("AbsolutePosition"):Connect(resampleAll)
artRoot:GetPropertyChangedSignal("AbsoluteSize"):Connect(resampleAll)

local edge = new("Frame", { Name = "Edge", Size = UDim2.fromScale(1, 1), ZIndex = 2, Parent = shell })
corner(edge, RADIUS.window)
local edgeStroke = stroke(edge, 0.74)
rimLight(edgeStroke, 1)
rimStrokes[edgeStroke] = true

local overlay = new("Frame", { Name = "Overlay", Size = UDim2.fromScale(1, 1), ZIndex = 10, Parent = body })

--------------------------------------------------------------------------------
-- Living artwork: a breathing glow where the fingers meet, and drifting stars
--------------------------------------------------------------------------------

local Ambient = { particles = {}, pulses = {} }
do
	local layer = new("Frame", { Name = "Ambient", Size = UDim2.fromScale(1, 1), ZIndex = 2, Parent = window })
	Ambient.Frame = layer

	local random = Random.new(1997)
	for index = 1, 26 do
		local size = if random:NextNumber() < 0.3 then 3 else 2
		local particle = new("Frame", {
			AnchorPoint = Vector2.new(0.5, 0.5),
			Size = UDim2.fromOffset(size, size),
			BackgroundColor3 = WHITE,
			BackgroundTransparency = 1,
			Parent = layer,
		})
		corner(particle, "full")
		Ambient.particles[index] = {
			frame = particle,
			x = random:NextNumber(0.18, 0.82),
			y = random:NextNumber(0, 1),
			speed = random:NextNumber(0.006, 0.018),
			sway = random:NextNumber(0.3, 0.9),
			twinkle = random:NextNumber(0.8, 2.4),
			phase = random:NextNumber(0, math.pi * 2),
			alpha = random:NextNumber(0.25, 0.7),
		}
	end

	-- A pulse is a few concentric discs; together they read as one soft light.
	-- Discs are sized in scale so the breathing is sub-pixel smooth.
	function Ambient.pulse(container, point, scale)
		local extent = math.floor(150 * scale)
		local holder = new("Frame", {
			Name = "Pulse",
			AnchorPoint = Vector2.new(0.5, 0.5),
			Size = UDim2.fromOffset(extent, extent),
			ZIndex = 2,
			Parent = container,
		})
		local discs = {}
		for index, spec in { { 22, 0.88 }, { 46, 0.93 }, { 84, 0.962 }, { 140, 0.982 } } do
			local size = spec[1] * scale / extent
			local disc = new("Frame", {
				AnchorPoint = Vector2.new(0.5, 0.5),
				Position = UDim2.fromScale(0.5, 0.5),
				Size = UDim2.fromScale(size, size),
				BackgroundColor3 = accent(),
				BackgroundTransparency = spec[2],
				ZIndex = 2,
				Parent = holder,
			})
			corner(disc, "full")
			discs[index] = { frame = disc, size = size, base = spec[2] }
		end
		pinToArtwork(holder, container, point)
		table.insert(Ambient.pulses, discs)
		themed(function(animated)
			for _, disc in discs do
				tween(disc.frame, if animated then 0.4 else 0, { BackgroundColor3 = accent() })
			end
		end)
		return holder
	end

	Ambient.pulse(layer, CONFIG.ArtworkFocus, 1)

	function Ambient.step(now)
		local enabled = Settings.get("ambient")
		for _, particle in Ambient.particles do
			if enabled then
				local y = (particle.y - now * particle.speed) % 1
				local x = particle.x + math.sin(now * particle.sway + particle.phase) * 0.006
				local edgeFade = math.clamp(math.min(y, 1 - y) * 6, 0, 1)
				local light = 0.55 + 0.45 * math.sin(now * particle.twinkle + particle.phase)
				particle.frame.Position = UDim2.fromScale(x, y)
				particle.frame.BackgroundTransparency = 1 - particle.alpha * light * edgeFade
			else
				particle.frame.BackgroundTransparency = 1
			end
		end
		local breath = 0.5 + 0.5 * math.sin(now * 1.25)
		for _, discs in Ambient.pulses do
			for _, disc in discs do
				local size = disc.size * (0.92 + 0.14 * breath)
				disc.frame.Size = UDim2.fromScale(size, size)
				disc.frame.BackgroundTransparency = if enabled
					then 1 - (1 - disc.base) * (0.55 + 0.45 * breath)
					else 1 - (1 - disc.base) * 0.6
			end
		end
	end
end

--------------------------------------------------------------------------------
-- Top bar
--------------------------------------------------------------------------------

local topBar =
	glassPanel(window, "TopBar", UDim2.fromOffset(INSET, INSET), UDim2.new(1, -INSET * 2, 0, BAR), RADIUS.panel, 3)
local dragHandle =
	new("TextButton", { Name = "Drag", Size = UDim2.fromScale(1, 1), ZIndex = 1, Parent = topBar.Content })

-- Brand: a sparkle in a dark tile, like the star where the hands meet.
local function brandMark(parent, size)
	local tile = new("Frame", {
		Size = UDim2.fromOffset(size, size),
		BackgroundColor3 = Color3.fromRGB(34, 34, 41),
		BackgroundTransparency = 0,
		Parent = parent,
	})
	corner(tile, math.floor(size * 0.3))
	gradient(tile, { Rotation = 120, Color = colors({ { 0, WHITE }, { 1, Color3.fromRGB(70, 70, 82) } }) })
	rimLight(stroke(tile, 0.72), 1)
	local glow = new("Frame", {
		AnchorPoint = Vector2.new(0.5, 0.5),
		Position = UDim2.fromScale(0.5, 0.5),
		Size = UDim2.fromScale(0.62, 0.62),
		BackgroundColor3 = accent(),
		BackgroundTransparency = 0.88,
		Parent = tile,
	})
	corner(glow, "full")
	local inner = new("Frame", {
		AnchorPoint = Vector2.new(0.5, 0.5),
		Position = UDim2.fromScale(0.5, 0.5),
		Size = UDim2.fromScale(0.34, 0.34),
		BackgroundColor3 = accent(),
		BackgroundTransparency = 0.78,
		Parent = tile,
	})
	corner(inner, "full")
	local star = glyph(tile, "sparkle", math.floor(size * 0.66), {
		AnchorPoint = Vector2.new(0.5, 0.5),
		Position = UDim2.fromScale(0.5, 0.5),
	})
	star.paint(WHITE, 0)
	themed(function(animated)
		local seconds = if animated then 0.4 else 0
		tween(glow, seconds, { BackgroundColor3 = accent() })
		tween(inner, seconds, { BackgroundColor3 = accent() })
	end)
	return tile
end

do
	local brand = new("Frame", {
		Name = "Brand",
		Position = UDim2.fromOffset(10, 0),
		Size = UDim2.new(0, 200, 1, 0),
		ZIndex = 2,
		Parent = topBar.Content,
	})
	local mark = brandMark(brand, 32)
	mark.AnchorPoint = Vector2.new(0, 0.5)
	mark.Position = UDim2.fromScale(0, 0.5)
	new("TextLabel", {
		Position = UDim2.fromOffset(44, 8),
		Size = UDim2.new(1, -44, 0, 20),
		Text = CONFIG.Title,
		TextSize = 16,
		weight = Enum.FontWeight.Bold,
		Parent = brand,
	})
	new("TextLabel", {
		Position = UDim2.fromOffset(44, 27),
		Size = UDim2.new(1, -44, 0, 14),
		Text = CONFIG.Tagline,
		TextSize = 11,
		TextTransparency = TEXT.tertiary,
		weight = Enum.FontWeight.Medium,
		Parent = brand,
	})
end

local NAV = {
	{ id = "home", label = "Home", icon = "home" },
	{ id = "gameplay", label = "Gameplay", icon = "gameplay" },
	{ id = "visuals", label = "Visuals", icon = "visuals" },
	{ id = "settings", label = "Settings", icon = "settings" },
}
local tabs = {}

local nav = new("Frame", {
	Name = "Navigation",
	AnchorPoint = Vector2.new(0.5, 0.5),
	Position = UDim2.fromScale(0.5, 0.5),
	Size = UDim2.fromOffset(#NAV * TAB_WIDTH + 8, 38),
	BackgroundColor3 = WHITE,
	BackgroundTransparency = 0.955,
	ZIndex = 2,
	Parent = topBar.Content,
})
corner(nav, RADIUS.control + 2)
stroke(nav, 0.91)

local navThumb = new("Frame", {
	Name = "Thumb",
	Position = UDim2.fromOffset(4, 4),
	Size = UDim2.fromOffset(TAB_WIDTH, 30),
	BackgroundColor3 = WHITE,
	BackgroundTransparency = 0.885,
	ZIndex = 2,
	Parent = nav,
})
corner(navThumb, RADIUS.control - 1)
rimLight(stroke(navThumb, 0.76), 1)

-- A thread of accent light along the lower edge of the active tab, tapering
-- out at both ends like light caught in the glass.
local navBeam = new("Frame", {
	Name = "Beam",
	AnchorPoint = Vector2.new(0.5, 1),
	Position = UDim2.new(0.5, 0, 1, 0),
	Size = UDim2.new(1, -28, 0, 1),
	BackgroundColor3 = accent(),
	BackgroundTransparency = 0,
	ZIndex = 3,
	Parent = navThumb,
})
gradient(navBeam, { Transparency = numbers({ { 0, 1 }, { 0.5, 0.05 }, { 1, 1 } }) })
themed(function(animated)
	tween(navBeam, if animated then 0.4 else 0, { BackgroundColor3 = accent() })
end)

local controls = new("Frame", {
	Name = "Controls",
	AnchorPoint = Vector2.new(1, 0.5),
	Position = UDim2.new(1, -10, 0.5, 0),
	Size = UDim2.fromOffset(160, 34),
	ZIndex = 2,
	Parent = topBar.Content,
})
list(controls, Enum.FillDirection.Horizontal, 8, Enum.HorizontalAlignment.Right, Enum.VerticalAlignment.Center)

do
	local avatar =
		new("Frame", { Name = "Avatar", Size = UDim2.fromOffset(30, 30), LayoutOrder = 1, Parent = controls })
	local image = new("ImageLabel", {
		Size = UDim2.fromScale(1, 1),
		BackgroundColor3 = Color3.fromRGB(40, 40, 48),
		BackgroundTransparency = 0,
		Image = "rbxthumb://type=AvatarHeadShot&id=" .. player.UserId .. "&w=150&h=150",
		Parent = avatar,
	})
	corner(image, "full")
	local ring = stroke(image, 0.35, accent(), 1.5)
	themed(function(animated)
		tween(ring, if animated then 0.4 else 0, { Color = accent() })
	end)
	local status = new("Frame", {
		AnchorPoint = Vector2.new(1, 1),
		Position = UDim2.new(1, 1, 1, 1),
		Size = UDim2.fromOffset(9, 9),
		BackgroundColor3 = GREEN,
		BackgroundTransparency = 0,
		ZIndex = 2,
		Parent = avatar,
	})
	corner(status, "full")
	stroke(status, 0, Color3.fromRGB(12, 12, 16), 2)

	new("Frame", {
		Name = "Divider",
		Size = UDim2.fromOffset(1, 20),
		BackgroundColor3 = WHITE,
		BackgroundTransparency = 0.88,
		LayoutOrder = 2,
		Parent = controls,
	})
end

local function chromeButton(iconName, order, hoverColor)
	local button = new("TextButton", {
		Size = UDim2.fromOffset(32, 32),
		BackgroundColor3 = WHITE,
		BackgroundTransparency = 0.955,
		LayoutOrder = order,
		Parent = controls,
	})
	corner(button, RADIUS.control)
	local edgeLine = stroke(button, 0.92)
	local icon =
		glyph(button, iconName, 14, { AnchorPoint = Vector2.new(0.5, 0.5), Position = UDim2.fromScale(0.5, 0.5) })
	icon.paint(INK, 0.32)
	pressable(button)
	button.MouseEnter:Connect(function()
		tween(button, 0.2, { BackgroundTransparency = 0.9 })
		tween(edgeLine, 0.2, { Transparency = 0.84 })
		icon.paint(hoverColor or INK, 0, 0.2)
	end)
	button.MouseLeave:Connect(function()
		tween(button, 0.25, { BackgroundTransparency = 0.955 })
		tween(edgeLine, 0.25, { Transparency = 0.92 })
		icon.paint(INK, 0.32, 0.25)
	end)
	return button, icon
end

local minimizeButton, minimizeIcon = chromeButton("minimize", 3)
local restoreIcon = glyph(minimizeButton, "restore", 14, {
	AnchorPoint = Vector2.new(0.5, 0.5),
	Position = UDim2.fromScale(0.5, 0.5),
	Visible = false,
})
restoreIcon.paint(INK, 0.32)
minimizeButton.MouseEnter:Connect(function()
	restoreIcon.paint(INK, 0, 0.2)
end)
minimizeButton.MouseLeave:Connect(function()
	restoreIcon.paint(INK, 0.32, 0.25)
end)
local closeButton = chromeButton("close", 4, RED)

--------------------------------------------------------------------------------
-- Content panel and pages
--------------------------------------------------------------------------------

local contentPanel = glassPanel(
	window,
	"ContentPanel",
	UDim2.fromOffset(INSET, INSET * 2 + BAR),
	UDim2.fromOffset(CONTENT.X, CONTENT.Y),
	RADIUS.panel,
	3
)
-- The content pane rides through this group only while the window collapses.
local collapseFader = new(
	"CanvasGroup",
	{ Name = "CollapseFader", Size = UDim2.fromScale(1, 1), ZIndex = 3, Visible = false, Parent = window }
)
local pageHost = new(
	"Frame",
	{ Name = "Pages", Size = UDim2.fromScale(1, 1), ClipsDescendants = true, Parent = contentPanel.Content }
)
local pageFader =
	new("CanvasGroup", { Name = "PageFader", Size = UDim2.fromScale(1, 1), Visible = false, Parent = pageHost })
glassFade(contentPanel, CONTENT, "top", 22)
glassFade(contentPanel, CONTENT, "bottom", 26)

local pages = {}

local function newPage(id, title, subtitle)
	local page = new("ScrollingFrame", {
		Name = id,
		Size = UDim2.fromScale(1, 1),
		CanvasSize = UDim2.new(),
		AutomaticCanvasSize = Enum.AutomaticSize.Y,
		ScrollingDirection = Enum.ScrollingDirection.Y,
		ScrollBarThickness = 3,
		ScrollBarImageColor3 = WHITE,
		ScrollBarImageTransparency = 0.78,
		VerticalScrollBarInset = Enum.ScrollBarInset.None,
		Visible = false,
		Parent = pageHost,
	})
	padding(page, 22, 22, 26, 22)
	list(page, Enum.FillDirection.Vertical, 18)
	page:GetPropertyChangedSignal("CanvasPosition"):Connect(function()
		Overlay.close()
	end)
	pages[id] = page

	if title then
		local header = new("Frame", { Name = "Header", Size = UDim2.new(1, 0, 0, 50), LayoutOrder = 0, Parent = page })
		new("TextLabel", {
			Size = UDim2.new(1, 0, 0, 28),
			Text = title,
			TextSize = 24,
			weight = Enum.FontWeight.Bold,
			Parent = header,
		})
		new("TextLabel", {
			Position = UDim2.fromOffset(0, 32),
			Size = UDim2.new(1, 0, 0, 18),
			Text = subtitle,
			TextSize = 13,
			TextTransparency = TEXT.secondary,
			Parent = header,
		})
	end
	return page
end

local function columns(page)
	local row = new("Frame", {
		Name = "Columns",
		Size = UDim2.fromScale(1, 0),
		AutomaticSize = Enum.AutomaticSize.Y,
		LayoutOrder = nextOrder(),
		Parent = page,
	})
	list(row, Enum.FillDirection.Horizontal, 16)
	local result = {}
	for index = 1, 2 do
		local column = new("Frame", {
			Name = "Column" .. index,
			Size = UDim2.new(0.5, -8, 0, 0),
			AutomaticSize = Enum.AutomaticSize.Y,
			LayoutOrder = index,
			Parent = row,
		})
		list(column, Enum.FillDirection.Vertical, 18)
		result[index] = column
	end
	return result[1], result[2]
end

--------------------------------------------------------------------------------
-- Cards and rows
--------------------------------------------------------------------------------

-- Cards are a second, darker pane of glass: the art still glows through,
-- but text always sits on a calm surface.
local function cardFrame(parent, order)
	local card = new("Frame", {
		Name = "Card",
		Size = UDim2.fromScale(1, 0),
		AutomaticSize = Enum.AutomaticSize.Y,
		BackgroundColor3 = Color3.fromRGB(20, 20, 26),
		BackgroundTransparency = 0.66,
		LayoutOrder = order,
		Parent = parent,
	})
	corner(card, RADIUS.card)
	gradient(card, { Rotation = 90, Color = colors({ { 0, WHITE }, { 1, Color3.fromRGB(120, 120, 128) } }) })
	rimLight(stroke(card, 0.86), 0.9)
	return card
end

local function section(column, title)
	local holder = new("Frame", {
		Name = title,
		Size = UDim2.fromScale(1, 0),
		AutomaticSize = Enum.AutomaticSize.Y,
		LayoutOrder = nextOrder(),
		Parent = column,
	})
	list(holder, Enum.FillDirection.Vertical, 10)
	new("TextLabel", {
		Name = "Title",
		Size = UDim2.new(1, 0, 0, 14),
		Text = string.upper(title),
		TextSize = 11,
		TextTransparency = 0.5,
		weight = Enum.FontWeight.Bold,
		LayoutOrder = 1,
		Parent = holder,
	})
	local card = cardFrame(holder, 2)
	padding(card, 4, 0, 4, 0)
	list(card, Enum.FillDirection.Vertical, 0)
	return { Frame = card, rows = 0 }
end

local function row(card, title, description, controlWidth, height)
	height = height or (if description then 62 else 50)
	local frame = new("Frame", {
		Name = title,
		Size = UDim2.new(1, 0, 0, height),
		LayoutOrder = nextOrder(),
		Parent = card.Frame,
	})
	if card.rows > 0 then
		new("Frame", {
			Name = "Divider",
			Position = UDim2.fromOffset(16, 0),
			Size = UDim2.new(1, -32, 0, 1),
			BackgroundColor3 = WHITE,
			BackgroundTransparency = 0.94,
			Parent = frame,
		})
	end
	card.rows += 1

	local hover = new("Frame", {
		Name = "Hover",
		Position = UDim2.fromOffset(5, 3),
		Size = UDim2.new(1, -10, 1, -6),
		BackgroundColor3 = WHITE,
		BackgroundTransparency = 1,
		Parent = frame,
	})
	corner(hover, RADIUS.control)
	frame.MouseEnter:Connect(function()
		tween(hover, 0.25, { BackgroundTransparency = 0.975 })
	end)
	frame.MouseLeave:Connect(function()
		tween(hover, 0.35, { BackgroundTransparency = 1 })
	end)

	local textWidth = -(32 + (controlWidth or 0) + 14)
	local titleLabel = new("TextLabel", {
		Name = "Title",
		Position = UDim2.fromOffset(16, if description then 13 else 0),
		Size = UDim2.new(1, textWidth, 0, if description then 18 else height),
		Text = title,
		TextSize = 14,
		TextTruncate = Enum.TextTruncate.AtEnd,
		weight = Enum.FontWeight.Medium,
		ZIndex = 2,
		Parent = frame,
	})
	local descriptionLabel
	if description then
		descriptionLabel = new("TextLabel", {
			Name = "Description",
			Position = UDim2.fromOffset(16, 33),
			Size = UDim2.new(1, textWidth, 0, 16),
			Text = description,
			TextSize = 12,
			TextTransparency = TEXT.secondary,
			TextTruncate = Enum.TextTruncate.AtEnd,
			ZIndex = 2,
			Parent = frame,
		})
	end
	local slot = new("Frame", {
		Name = "Control",
		AnchorPoint = Vector2.new(1, 0.5),
		Position = UDim2.new(1, -16, 0.5, 0),
		Size = UDim2.fromOffset(controlWidth or 0, 34),
		ZIndex = 3,
		Parent = frame,
	})
	return slot, frame, titleLabel, descriptionLabel
end

--------------------------------------------------------------------------------
-- Pointer drags (sliders and the window share one tracker)
--------------------------------------------------------------------------------

local Drag = {}

function Drag.begin(onMove, onEnd)
	Drag.active = { move = onMove, finish = onEnd }
end

UserInputService.InputChanged:Connect(function(input)
	local active = Drag.active
	if active and isMove(input) then
		active.move(pointer(input))
	end
end)

UserInputService.InputEnded:Connect(function(input)
	local active = Drag.active
	if active and isPress(input) then
		Drag.active = nil
		if active.finish then
			active.finish()
		end
	end
end)

--------------------------------------------------------------------------------
-- Controls
--------------------------------------------------------------------------------

local function toggle(card, title, description, key)
	local slot = row(card, title, description, 44)
	local value = Settings.get(key)

	local track = new("TextButton", {
		Name = "Switch",
		AnchorPoint = Vector2.new(1, 0.5),
		Position = UDim2.new(1, 0, 0.5, 0),
		Size = UDim2.fromOffset(44, 24),
		BackgroundColor3 = WHITE,
		BackgroundTransparency = 0.88,
		ZIndex = 2,
		Parent = slot,
	})
	corner(track, "full")
	local edgeLine = stroke(track, 0.86)
	local knob = new("Frame", {
		Name = "Knob",
		AnchorPoint = Vector2.new(0, 0.5),
		Position = UDim2.new(0, 3, 0.5, 0),
		Size = UDim2.fromOffset(18, 18),
		BackgroundColor3 = WHITE,
		BackgroundTransparency = 0,
		ZIndex = 3,
		Parent = track,
	})
	corner(knob, "full")
	gradient(knob, { Rotation = 90, Color = colors({ { 0, WHITE }, { 1, Color3.fromRGB(214, 216, 224) } }) })

	local function render(animated)
		local seconds = if animated then 0.32 else 0
		local color = accent()
		if value then
			tween(track, seconds, { BackgroundColor3 = color, BackgroundTransparency = 0 })
			tween(edgeLine, seconds, { Color = color, Transparency = 0.5 })
			tween(knob, seconds, {
				AnchorPoint = Vector2.new(1, 0.5),
				Position = UDim2.new(1, -3, 0.5, 0),
				BackgroundColor3 = inkOn(color),
			})
		else
			tween(track, seconds, { BackgroundColor3 = WHITE, BackgroundTransparency = 0.88 })
			tween(edgeLine, seconds, { Color = WHITE, Transparency = 0.86 })
			tween(knob, seconds, {
				AnchorPoint = Vector2.new(0, 0.5),
				Position = UDim2.new(0, 3, 0.5, 0),
				BackgroundColor3 = Color3.fromRGB(232, 234, 240),
			})
		end
	end

	track.MouseButton1Down:Connect(function()
		tween(knob, 0.18, { Size = UDim2.fromOffset(23, 18) })
	end)
	local function release()
		tween(knob, 0.3, { Size = UDim2.fromOffset(18, 18) })
	end
	track.MouseButton1Up:Connect(release)
	track.MouseLeave:Connect(release)
	track.Activated:Connect(function()
		Settings.set(key, not value)
	end)

	Settings.listen(key, function(newValue)
		value = newValue
		render(true)
	end)
	themed(render)
end

-- A deferred slider previews while dragging and commits on release, for
-- settings (like interface scale) that would move the slider under the pointer.
local function slider(card, title, key, minimum, maximum, step, format, deferred)
	local slot, frame, titleLabel = row(card, title, nil, 0, 66)
	slot.Visible = false
	local value = Settings.get(key)

	local readout = new("TextLabel", {
		Name = "Value",
		AnchorPoint = Vector2.new(1, 0),
		Position = UDim2.new(1, -16, 0, 14),
		Size = UDim2.fromOffset(80, 18),
		Text = format(value),
		TextSize = 13,
		TextTransparency = TEXT.secondary,
		TextXAlignment = Enum.TextXAlignment.Right,
		weight = Enum.FontWeight.Medium,
		ZIndex = 2,
		Parent = frame,
	})
	titleLabel.Size = UDim2.new(1, -120, 0, 18)
	titleLabel.Position = UDim2.fromOffset(16, 14)

	local rail = new("Frame", {
		Name = "Rail",
		Position = UDim2.new(0, 16, 0, 46),
		Size = UDim2.new(1, -32, 0, 4),
		BackgroundColor3 = WHITE,
		BackgroundTransparency = 0.88,
		ZIndex = 2,
		Parent = frame,
	})
	corner(rail, "full")
	local fill = new("Frame", {
		Name = "Fill",
		Size = UDim2.fromScale(0, 1),
		BackgroundColor3 = accent(),
		BackgroundTransparency = 0,
		ZIndex = 2,
		Parent = rail,
	})
	corner(fill, "full")
	gradient(fill, { Transparency = numbers({ { 0, 0.55 }, { 1, 0 } }) })

	-- Children always draw over their parent, so the knob is a transparent
	-- holder with its shadow beneath the face.
	local knob = new("Frame", {
		Name = "Knob",
		AnchorPoint = Vector2.new(0.5, 0.5),
		Position = UDim2.fromScale(0, 0.5),
		Size = UDim2.fromOffset(16, 16),
		ZIndex = 4,
		Parent = rail,
	})
	local knobScale = new("UIScale", { Parent = knob })
	local knobShadow = new("Frame", {
		AnchorPoint = Vector2.new(0.5, 0.5),
		Position = UDim2.new(0.5, 0, 0.5, 1),
		Size = UDim2.new(1, 4, 1, 4),
		BackgroundColor3 = BLACK,
		BackgroundTransparency = 0.62,
		ZIndex = 1,
		Parent = knob,
	})
	corner(knobShadow, "full")
	local face = new("Frame", {
		Name = "Face",
		Size = UDim2.fromScale(1, 1),
		BackgroundColor3 = WHITE,
		BackgroundTransparency = 0,
		ZIndex = 2,
		Parent = knob,
	})
	corner(face, "full")
	gradient(face, { Rotation = 90, Color = colors({ { 0, WHITE }, { 1, Color3.fromRGB(218, 220, 228) } }) })

	local hit = new("TextButton", {
		Name = "Hit",
		Position = UDim2.new(0, 8, 0, 32),
		Size = UDim2.new(1, -16, 0, 30),
		ZIndex = 5,
		Parent = frame,
	})

	local dragging = false
	local function alphaOf(number)
		return math.clamp((number - minimum) / (maximum - minimum), 0, 1)
	end

	-- Ranges that straddle zero fill outward from a centre mark.
	local origin = if minimum < 0 and maximum > 0 then alphaOf(0) else 0
	if origin > 0 then
		new("Frame", {
			Name = "Zero",
			AnchorPoint = Vector2.new(0.5, 0.5),
			Position = UDim2.fromScale(origin, 0.5),
			Size = UDim2.fromOffset(2, 10),
			BackgroundColor3 = WHITE,
			BackgroundTransparency = 0.7,
			ZIndex = 3,
			Parent = rail,
		})
	end

	local function render(animated)
		local alpha = alphaOf(value)
		local seconds = if animated then 0.16 else 0
		tween(fill, seconds, {
			Position = UDim2.fromScale(math.min(origin, alpha), 0),
			Size = UDim2.fromScale(math.abs(alpha - origin), 1),
		})
		tween(knob, seconds, { Position = UDim2.fromScale(alpha, 0.5) })
		readout.Text = format(value)
	end

	local function setFromPointer(point)
		local position, size = rail.AbsolutePosition.X, rail.AbsoluteSize.X
		if size <= 0 then
			return
		end
		local alpha = math.clamp((point.X - position) / size, 0, 1)
		local raw = minimum + alpha * (maximum - minimum)
		local snapped = math.clamp(minimum + math.floor((raw - minimum) / step + 0.5) * step, minimum, maximum)
		if deferred then
			value = snapped
			render(false)
		else
			Settings.set(key, snapped)
		end
	end

	local function emphasize(active)
		tween(
			knobScale,
			if active then 0.2 else 0.35,
			{ Scale = if active then 1.18 else 1 },
			if active then Enum.EasingStyle.Quint else Enum.EasingStyle.Back
		)
		tween(readout, 0.2, { TextTransparency = if active then 0 else TEXT.secondary })
	end

	hit.InputBegan:Connect(function(input)
		if not isPress(input) then
			return
		end
		dragging = true
		emphasize(true)
		setFromPointer(pointer(input))
		Drag.begin(setFromPointer, function()
			dragging = false
			emphasize(false)
			if deferred then
				Settings.set(key, value)
			end
		end)
	end)
	hit.MouseEnter:Connect(function()
		if not dragging then
			tween(knobScale, 0.2, { Scale = 1.08 })
		end
	end)
	hit.MouseLeave:Connect(function()
		if not dragging then
			tween(knobScale, 0.3, { Scale = 1 })
		end
	end)

	Settings.listen(key, function(newValue)
		value = newValue
		render(not dragging)
	end)
	themed(function(animated)
		tween(fill, if animated then 0.4 else 0, { BackgroundColor3 = accent() })
	end)
	render(false)
end

local function dropdown(card, title, description, key, options, width)
	width = width or 156
	local slot = row(card, title, description, width)
	local button = new("TextButton", {
		Name = "Select",
		AnchorPoint = Vector2.new(1, 0.5),
		Position = UDim2.new(1, 0, 0.5, 0),
		Size = UDim2.fromOffset(width, 32),
		BackgroundColor3 = WHITE,
		BackgroundTransparency = 0.94,
		Parent = slot,
	})
	corner(button, RADIUS.control)
	local edgeLine = stroke(button, 0.9)
	local current = new("TextLabel", {
		Position = UDim2.fromOffset(12, 0),
		Size = UDim2.new(1, -40, 1, 0),
		Text = Settings.get(key),
		TextSize = 13,
		TextTruncate = Enum.TextTruncate.AtEnd,
		weight = Enum.FontWeight.Medium,
		Parent = button,
	})
	local chevron = glyph(button, "chevron", 12, {
		AnchorPoint = Vector2.new(1, 0.5),
		Position = UDim2.new(1, -11, 0.5, 0),
	})
	chevron.paint(INK, 0.4)
	pressable(button)

	button.MouseEnter:Connect(function()
		tween(button, 0.2, { BackgroundTransparency = 0.9 })
		tween(edgeLine, 0.2, { Transparency = 0.82 })
	end)
	button.MouseLeave:Connect(function()
		tween(button, 0.25, { BackgroundTransparency = 0.94 })
		tween(edgeLine, 0.25, { Transparency = 0.9 })
	end)
	button.Activated:Connect(function()
		if Overlay.owner == button then
			Overlay.close()
			return
		end
		Overlay.openList(button, options, Settings.get(key), function(choice)
			Settings.set(key, choice)
		end)
		tween(chevron.Frame, 0.3, { Rotation = 180 })
		Overlay.onClose = function()
			tween(chevron.Frame, 0.3, { Rotation = 0 })
		end
	end)
	Settings.listen(key, function(newValue)
		current.Text = newValue
	end)
end

local function segmented(card, title, description, key, options, segmentWidth)
	segmentWidth = segmentWidth or 64
	local width = #options * segmentWidth + 6
	local slot = row(card, title, description, width)
	local track = new("Frame", {
		Name = "Segments",
		AnchorPoint = Vector2.new(1, 0.5),
		Position = UDim2.new(1, 0, 0.5, 0),
		Size = UDim2.fromOffset(width, 32),
		BackgroundColor3 = WHITE,
		BackgroundTransparency = 0.955,
		Parent = slot,
	})
	corner(track, RADIUS.control)
	stroke(track, 0.91)
	local thumb = new("Frame", {
		Name = "Thumb",
		Position = UDim2.fromOffset(3, 3),
		Size = UDim2.fromOffset(segmentWidth, 26),
		BackgroundColor3 = WHITE,
		BackgroundTransparency = 0.86,
		ZIndex = 2,
		Parent = track,
	})
	corner(thumb, RADIUS.small)
	rimLight(stroke(thumb, 0.74), 1)

	local labels = {}
	for index, option in options do
		local button = new("TextButton", {
			Name = option,
			Position = UDim2.fromOffset(3 + (index - 1) * segmentWidth, 3),
			Size = UDim2.fromOffset(segmentWidth, 26),
			Text = option,
			TextSize = 12,
			TextTransparency = TEXT.secondary,
			weight = Enum.FontWeight.Medium,
			ZIndex = 3,
			Parent = track,
		})
		labels[option] = button
		button.Activated:Connect(function()
			Settings.set(key, option)
		end)
	end

	local function render(animated)
		local selected = Settings.get(key)
		local index = table.find(options, selected) or 1
		tween(thumb, if animated then 0.38 else 0, { Position = UDim2.fromOffset(3 + (index - 1) * segmentWidth, 3) })
		for option, label in labels do
			tween(label, if animated then 0.25 else 0, {
				TextTransparency = if option == selected then 0 else TEXT.secondary,
			})
		end
	end
	Settings.listen(key, function()
		render(true)
	end)
	render(false)
end

local function swatches(card, title, key)
	local width = #ACCENTS * 30 - 6
	local slot = row(card, title, nil, width)
	local holder = new("Frame", {
		AnchorPoint = Vector2.new(1, 0.5),
		Position = UDim2.new(1, 0, 0.5, 0),
		Size = UDim2.fromOffset(width, 24),
		Parent = slot,
	})
	list(holder, Enum.FillDirection.Horizontal, 6, Enum.HorizontalAlignment.Right, Enum.VerticalAlignment.Center)

	local items = {}
	for index, entry in ACCENTS do
		local button = new(
			"TextButton",
			{ Name = entry.name, Size = UDim2.fromOffset(24, 24), LayoutOrder = index, Parent = holder }
		)
		local ring = new("Frame", {
			AnchorPoint = Vector2.new(0.5, 0.5),
			Position = UDim2.fromScale(0.5, 0.5),
			Size = UDim2.fromOffset(22, 22),
			Parent = button,
		})
		corner(ring, "full")
		local ringStroke = stroke(ring, 1, WHITE, 1.5)
		local dot = new("Frame", {
			AnchorPoint = Vector2.new(0.5, 0.5),
			Position = UDim2.fromScale(0.5, 0.5),
			Size = UDim2.fromOffset(16, 16),
			BackgroundColor3 = entry.color,
			BackgroundTransparency = 0,
			ZIndex = 2,
			Parent = button,
		})
		corner(dot, "full")
		gradient(dot, { Rotation = 90, Color = colors({ { 0, WHITE }, { 1, Color3.fromRGB(196, 196, 204) } }) })
		stroke(dot, 0.82, WHITE, 1)
		items[entry.name] = { ring = ringStroke, dot = dot }
		button.Activated:Connect(function()
			Settings.set(key, entry.name)
		end)
		button.MouseEnter:Connect(function()
			if Settings.get(key) ~= entry.name then
				tween(dot, 0.2, { Size = UDim2.fromOffset(18, 18) })
			end
		end)
		button.MouseLeave:Connect(function()
			if Settings.get(key) ~= entry.name then
				tween(dot, 0.25, { Size = UDim2.fromOffset(16, 16) })
			end
		end)
	end

	local function render(animated)
		local selected = Settings.get(key)
		local seconds = if animated then 0.3 else 0
		for name, item in items do
			local active = name == selected
			tween(item.ring, seconds, { Transparency = if active then 0.08 else 1 })
			tween(item.dot, seconds, { Size = UDim2.fromOffset(if active then 13 else 16, if active then 13 else 16) })
		end
	end
	Settings.listen(key, function()
		render(true)
	end)
	render(false)
end

local KeyCapture = {}

local function keycap(parent, text, properties)
	local cap = new("TextLabel", {
		Size = UDim2.fromOffset(0, 26),
		AutomaticSize = Enum.AutomaticSize.X,
		BackgroundColor3 = WHITE,
		BackgroundTransparency = 0.93,
		Text = text,
		TextSize = 12,
		TextXAlignment = Enum.TextXAlignment.Center,
		weight = Enum.FontWeight.SemiBold,
		Parent = parent,
	})
	for property, value in properties or {} do
		cap[property] = value
	end
	padding(cap, 0, 10, 0, 10)
	corner(cap, RADIUS.small - 1)
	rimLight(stroke(cap, 0.84), 1)
	new("UISizeConstraint", { MinSize = Vector2.new(30, 0), Parent = cap })
	return cap
end

local function keybind(card, title, description, key)
	local slot = row(card, title, description, 118)
	local button = new("TextButton", {
		Name = "Bind",
		AnchorPoint = Vector2.new(1, 0.5),
		Position = UDim2.new(1, 0, 0.5, 0),
		Size = UDim2.fromOffset(0, 30),
		AutomaticSize = Enum.AutomaticSize.X,
		BackgroundColor3 = WHITE,
		BackgroundTransparency = 0.93,
		Text = prettyKey(Settings.get(key)),
		TextSize = 12,
		weight = Enum.FontWeight.SemiBold,
		Parent = slot,
	})
	padding(button, 0, 12, 0, 12)
	corner(button, RADIUS.small)
	local edgeLine = stroke(button, 0.84)
	new("UISizeConstraint", { MinSize = Vector2.new(64, 30), Parent = button })
	pressable(button)

	local listening = false
	local function finish(newKey)
		listening = false
		KeyCapture.active = nil
		if newKey then
			Settings.set(key, newKey)
		end
		button.Text = prettyKey(Settings.get(key))
		tween(edgeLine, 0.25, { Color = WHITE, Transparency = 0.84 })
		tween(button, 0.25, { TextTransparency = 0 })
	end

	button.Activated:Connect(function()
		if listening then
			finish(nil)
			return
		end
		if KeyCapture.active then
			KeyCapture.active(nil)
		end
		listening = true
		button.Text = "Press a key…"
		tween(edgeLine, 0.2, { Color = accent(), Transparency = 0.1 })
		tween(button, 0.2, { TextTransparency = 0.3 })
		KeyCapture.active = finish
	end)
	Settings.listen(key, function(newValue)
		if not listening then
			button.Text = prettyKey(newValue)
		end
	end)
end

local function actionButton(parent, text, style, onClick)
	local primary = style == "primary"
	local button = new("TextButton", {
		Size = UDim2.fromOffset(0, 32),
		AutomaticSize = Enum.AutomaticSize.X,
		BackgroundColor3 = if primary then accent() else WHITE,
		BackgroundTransparency = if primary then 0 else 0.93,
		Text = text,
		TextSize = 13,
		TextColor3 = if style == "danger" then RED else INK,
		weight = Enum.FontWeight.SemiBold,
		Parent = parent,
	})
	padding(button, 0, 16, 0, 16)
	corner(button, RADIUS.control)
	local edgeLine = stroke(button, if primary then 0.6 else 0.86)
	if primary then
		gradient(button, { Rotation = 90, Color = colors({ { 0, WHITE }, { 1, Color3.fromRGB(205, 205, 212) } }) })
		themed(function(animated)
			tween(button, if animated then 0.4 else 0, { BackgroundColor3 = accent(), TextColor3 = inkOn(accent()) })
		end)
	end
	pressable(button)
	button.MouseEnter:Connect(function()
		tween(button, 0.2, { BackgroundTransparency = if primary then 0.08 else 0.89 })
		tween(edgeLine, 0.2, { Transparency = if primary then 0.45 else 0.78 })
	end)
	button.MouseLeave:Connect(function()
		tween(button, 0.25, { BackgroundTransparency = if primary then 0 else 0.93 })
		tween(edgeLine, 0.25, { Transparency = if primary then 0.6 else 0.86 })
	end)
	button.Activated:Connect(onClick)
	return button
end

--------------------------------------------------------------------------------
-- Overlay: floating option lists above the window
--------------------------------------------------------------------------------

function Overlay.close()
	local current = Overlay.current
	if not current then
		return
	end
	Overlay.current = nil
	Overlay.owner = nil
	local onClose = Overlay.onClose
	Overlay.onClose = nil
	if onClose then
		onClose()
	end
	tween(current.scale, 0.16, { Scale = 0.96 }, Enum.EasingStyle.Quint, Enum.EasingDirection.In)
	for _, fade in current.fades do
		tween(fade[1], 0.14, { [fade[2]] = 1 })
	end
	task.delay(duration(0.18), function()
		forget(current.frame)
		current.frame:Destroy()
	end)
end

function Overlay.openList(anchor, options, selected, onPick)
	Overlay.close()
	local scale = overlay.AbsoluteSize.X / WINDOW.X
	if scale <= 0 then
		return
	end
	local relative = (anchor.AbsolutePosition - overlay.AbsolutePosition) / scale
	local anchorSize = anchor.AbsoluteSize / scale
	local itemHeight = 32
	local height = #options * itemHeight + 8
	local width = math.max(anchorSize.X, 150)
	local y = relative.Y + anchorSize.Y + 6
	local below = y + height <= WINDOW.Y - 6
	if not below then
		y = relative.Y - height - 6
	end

	local frame = new("Frame", {
		Name = "List",
		AnchorPoint = Vector2.new(0, if below then 0 else 1),
		Position = UDim2.fromOffset(relative.X + anchorSize.X - width, if below then y else y + height),
		Size = UDim2.fromOffset(width, height),
		ZIndex = 50,
		Parent = overlay,
	})
	local fades = {}
	local listScale = new("UIScale", { Scale = 0.96, Parent = frame })

	local shadow = softShadow(frame, RADIUS.card, 18, 0.55, 8, 8)
	for _, layer in shadow:GetChildren() do
		local target = layer.BackgroundTransparency
		layer.BackgroundTransparency = 1
		table.insert(fades, { layer, "BackgroundTransparency", target })
	end

	local surface = new("Frame", {
		Size = UDim2.fromScale(1, 1),
		BackgroundColor3 = SURFACE,
		BackgroundTransparency = 1,
		ZIndex = 2,
		Parent = frame,
	})
	corner(surface, RADIUS.card - 2)
	gradient(surface, { Rotation = 90, Color = colors({ { 0, WHITE }, { 1, Color3.fromRGB(150, 150, 160) } }) })
	table.insert(fades, { surface, "BackgroundTransparency", 0 })
	local surfaceStroke = stroke(surface, 1)
	rimLight(surfaceStroke, 1)
	table.insert(fades, { surfaceStroke, "Transparency", 0.8 })
	padding(surface, 4)
	list(surface, Enum.FillDirection.Vertical, 0)

	for index, option in options do
		local active = option == selected
		local item = new("TextButton", {
			Name = option,
			Size = UDim2.new(1, 0, 0, itemHeight),
			BackgroundColor3 = WHITE,
			BackgroundTransparency = 1,
			LayoutOrder = index,
			ZIndex = 3,
			Parent = surface,
		})
		corner(item, RADIUS.small)
		local text = new("TextLabel", {
			Position = UDim2.fromOffset(12, 0),
			Size = UDim2.new(1, -40, 1, 0),
			Text = option,
			TextSize = 13,
			TextTransparency = 1,
			weight = if active then Enum.FontWeight.SemiBold else Enum.FontWeight.Regular,
			ZIndex = 4,
			Parent = item,
		})
		table.insert(fades, { text, "TextTransparency", if active then 0 else 0.3 })
		if active then
			local check = glyph(item, "check", 12, {
				AnchorPoint = Vector2.new(1, 0.5),
				Position = UDim2.new(1, -12, 0.5, 0),
				ZIndex = 4,
			})
			check.paint(accent(), 1)
			check.paint(accent(), 0, 0.2)
		end
		item.MouseEnter:Connect(function()
			tween(item, 0.15, { BackgroundTransparency = 0.93 })
		end)
		item.MouseLeave:Connect(function()
			tween(item, 0.2, { BackgroundTransparency = 1 })
		end)
		item.Activated:Connect(function()
			onPick(option)
			Overlay.close()
		end)
	end

	for _, fade in fades do
		tween(fade[1], 0.22, { [fade[2]] = fade[3] })
	end
	tween(listScale, 0.32, { Scale = 1 })
	Overlay.current = { frame = frame, scale = listScale, fades = fades }
	Overlay.owner = anchor
end

UserInputService.InputBegan:Connect(function(input)
	local current = Overlay.current
	if current and isPress(input) then
		local point = pointer(input)
		if not inside(current.frame, point) and not (Overlay.owner and inside(Overlay.owner, point)) then
			Overlay.close()
		end
	end
end)

--------------------------------------------------------------------------------
-- Live session stats
--------------------------------------------------------------------------------

local Stats = { fps = 60, ping = 0, frames = 0, elapsed = 0, fpsHistory = {}, pingHistory = {}, listeners = {} }
local SESSION_START = os.clock()
local HISTORY = 26

for index = 1, HISTORY do
	Stats.fpsHistory[index] = 60
	Stats.pingHistory[index] = 0
end

function Stats.step(dt)
	Stats.frames += 1
	Stats.elapsed += dt
	if Stats.elapsed < 0.5 then
		return
	end
	Stats.fps = Stats.frames / Stats.elapsed
	Stats.frames, Stats.elapsed = 0, 0
	local ok, ping = pcall(function()
		return player:GetNetworkPing()
	end)
	Stats.ping = if ok and ping then ping * 1000 else 0
	-- The first reading seeds the whole history so the sparklines start level.
	if not Stats.seeded then
		Stats.seeded = true
		for index = 1, HISTORY do
			Stats.fpsHistory[index] = Stats.fps
			Stats.pingHistory[index] = Stats.ping
		end
	end
	table.remove(Stats.fpsHistory, 1)
	table.insert(Stats.fpsHistory, Stats.fps)
	table.remove(Stats.pingHistory, 1)
	table.insert(Stats.pingHistory, Stats.ping)
	for _, listener in Stats.listeners do
		listener()
	end
end

local function clockText(seconds)
	seconds = math.floor(seconds)
	local hours = seconds // 3600
	local minutes = (seconds % 3600) // 60
	if hours > 0 then
		return string.format("%d:%02d:%02d", hours, minutes, seconds % 60)
	end
	return string.format("%02d:%02d", minutes, seconds % 60)
end

--------------------------------------------------------------------------------
-- Home
--------------------------------------------------------------------------------

do
	local page = newPage("home")

	-- Hero: a pane of clear glass in the frosted panel, looking straight onto
	-- the artwork where the two hands meet.
	local hero = new("Frame", {
		Name = "Hero",
		Size = UDim2.new(1, 0, 0, 214),
		BackgroundColor3 = BLACK,
		BackgroundTransparency = 0,
		ClipsDescendants = true,
		LayoutOrder = nextOrder(),
		Parent = page,
	})
	corner(hero, RADIUS.card + 2)
	if HAS_ART then
		local lens = artLayer(hero, CONFIG.Artwork, { Name = "Lens", ZIndex = 1 })
		corner(lens, RADIUS.card + 2)
		Ambient.pulse(hero, CONFIG.ArtworkFocus, 0.9)
	else
		-- Without the artwork, a soft band of light stands in for the meeting hands:
		-- a horizontal fade inside a group that fades vertically.
		local aurora = new("CanvasGroup", {
			Name = "Aurora",
			AnchorPoint = Vector2.new(0.5, 0.5),
			Position = UDim2.fromScale(0.5, 0.68),
			Size = UDim2.new(1, 0, 0, 170),
			ZIndex = 2,
			Parent = hero,
		})
		gradient(aurora, { Rotation = 90, Transparency = numbers({ { 0, 1 }, { 0.5, 0 }, { 1, 1 } }) })
		local band = new("Frame", {
			Size = UDim2.fromScale(1, 1),
			BackgroundColor3 = accent(),
			BackgroundTransparency = 0.6,
			Parent = aurora,
		})
		gradient(band, { Transparency = numbers({ { 0, 1 }, { 0.5, 0 }, { 1, 1 } }) })
		themed(function(animated)
			tween(band, if animated then 0.4 else 0, { BackgroundColor3 = accent() })
		end)
	end
	-- Darken the sky for the title and let the sides fall away into the glass.
	local scrim = new("Frame", {
		Name = "Scrim",
		Size = UDim2.fromScale(1, 1),
		BackgroundColor3 = BLACK,
		BackgroundTransparency = 0,
		ZIndex = 3,
		Parent = hero,
	})
	corner(scrim, RADIUS.card + 2)
	gradient(scrim, {
		Rotation = 90,
		Transparency = numbers({ { 0, 0.08 }, { 0.36, 0.34 }, { 0.6, 1 }, { 0.84, 1 }, { 1, 0.55 } }),
	})
	local vignette = new("Frame", {
		Name = "Vignette",
		Size = UDim2.fromScale(1, 1),
		BackgroundColor3 = BLACK,
		BackgroundTransparency = 0,
		ZIndex = 3,
		Parent = hero,
	})
	corner(vignette, RADIUS.card + 2)
	gradient(vignette, {
		Transparency = numbers({ { 0, 0.3 }, { 0.22, 0.85 }, { 0.5, 1 }, { 0.78, 0.85 }, { 1, 0.3 } }),
	})
	local heroRim = stroke(hero, 0.8)
	rimLight(heroRim, 1)
	local heroSpecular = new("Frame", {
		Position = UDim2.fromOffset(24, 1),
		Size = UDim2.new(1, -48, 0, 1),
		BackgroundColor3 = WHITE,
		BackgroundTransparency = 0,
		ZIndex = 5,
		Parent = hero,
	})
	gradient(heroSpecular, { Transparency = numbers({ { 0, 1 }, { 0.3, 0.75 }, { 0.7, 0.75 }, { 1, 1 } }) })

	local chip = new("Frame", {
		Name = "Chip",
		AnchorPoint = Vector2.new(0.5, 0),
		Position = UDim2.new(0.5, 0, 0, 22),
		Size = UDim2.fromOffset(0, 24),
		AutomaticSize = Enum.AutomaticSize.X,
		BackgroundColor3 = WHITE,
		BackgroundTransparency = 0.9,
		ZIndex = 6,
		Parent = hero,
	})
	corner(chip, "full")
	rimLight(stroke(chip, 0.8), 1)
	padding(chip, 0, 12, 0, 10)
	list(chip, Enum.FillDirection.Horizontal, 7, Enum.HorizontalAlignment.Center, Enum.VerticalAlignment.Center)
	local liveDot = new("Frame", {
		Size = UDim2.fromOffset(6, 6),
		BackgroundColor3 = accent(),
		BackgroundTransparency = 0,
		LayoutOrder = 1,
		ZIndex = 7,
		Parent = chip,
	})
	corner(liveDot, "full")
	themed(function(animated)
		tween(liveDot, if animated then 0.4 else 0, { BackgroundColor3 = accent() })
	end)
	new("TextLabel", {
		Size = UDim2.fromOffset(0, 24),
		AutomaticSize = Enum.AutomaticSize.X,
		Text = "SESSION ACTIVE",
		TextSize = 10,
		TextTransparency = 0.12,
		weight = Enum.FontWeight.Bold,
		LayoutOrder = 2,
		ZIndex = 7,
		Parent = chip,
	})
	task.spawn(function()
		while chip.Parent do
			tween(liveDot, 0.9, { BackgroundTransparency = 0.65 }, Enum.EasingStyle.Sine, Enum.EasingDirection.InOut)
			task.wait(1)
			tween(liveDot, 0.9, { BackgroundTransparency = 0 }, Enum.EasingStyle.Sine, Enum.EasingDirection.InOut)
			task.wait(1)
		end
	end)

	local hour = tonumber(os.date("%H")) or 12
	local greeting = if hour < 5
		then "Good night"
		elseif hour < 12 then "Good morning"
		elseif hour < 18 then "Good afternoon"
		else "Good evening"
	-- The title sits in the dark sky above the fingertips and nothing crosses them.
	new("TextLabel", {
		Name = "Greeting",
		Position = UDim2.fromOffset(0, 56),
		Size = UDim2.new(1, 0, 0, 36),
		Text = greeting .. ", " .. player.DisplayName,
		TextSize = 30,
		TextXAlignment = Enum.TextXAlignment.Center,
		weight = Enum.FontWeight.Bold,
		ZIndex = 6,
		Parent = hero,
	})

	-- Stat tiles
	local tiles =
		new("Frame", { Name = "Stats", Size = UDim2.new(1, 0, 0, 108), LayoutOrder = nextOrder(), Parent = page })
	list(tiles, Enum.FillDirection.Horizontal, 12)

	local function tile(index, iconName, caption, unit)
		local frame = cardFrame(tiles, index)
		frame.AutomaticSize = Enum.AutomaticSize.None
		frame.Size = UDim2.new(0.25, -9, 1, 0)
		local icon = glyph(frame, iconName, 14, { Position = UDim2.fromOffset(16, 16) })
		icon.paint(INK, TEXT.tertiary)
		new("TextLabel", {
			Position = UDim2.fromOffset(38, 15),
			Size = UDim2.new(1, -54, 0, 16),
			Text = caption,
			TextSize = 11,
			TextTransparency = TEXT.tertiary,
			weight = Enum.FontWeight.Bold,
			Parent = frame,
		})
		local valueRow =
			new("Frame", { Position = UDim2.fromOffset(16, 38), Size = UDim2.new(1, -32, 0, 30), Parent = frame })
		list(valueRow, Enum.FillDirection.Horizontal, 5, Enum.HorizontalAlignment.Left, Enum.VerticalAlignment.Bottom)
		local value = new("TextLabel", {
			Size = UDim2.fromOffset(0, 30),
			AutomaticSize = Enum.AutomaticSize.X,
			Text = "—",
			TextSize = 26,
			TextYAlignment = Enum.TextYAlignment.Bottom,
			weight = Enum.FontWeight.Bold,
			LayoutOrder = 1,
			Parent = valueRow,
		})
		local unitLabel = new("TextLabel", {
			Size = UDim2.fromOffset(0, 24),
			AutomaticSize = Enum.AutomaticSize.X,
			Text = unit,
			TextSize = 12,
			TextTransparency = TEXT.secondary,
			TextYAlignment = Enum.TextYAlignment.Bottom,
			weight = Enum.FontWeight.Medium,
			LayoutOrder = 2,
			Parent = valueRow,
		})
		local foot =
			new("Frame", { Position = UDim2.new(0, 16, 1, -30), Size = UDim2.new(1, -32, 0, 16), Parent = frame })
		return value, foot, unitLabel
	end

	local function sparkline(foot, history, ceiling)
		local bars = {}
		local count = #history
		for index = 1, count do
			local bar = new("Frame", {
				AnchorPoint = Vector2.new(0, 1),
				Position = UDim2.new((index - 1) / count, 0, 1, 0),
				Size = UDim2.new(1 / count, -3, 0.2, 0),
				BackgroundColor3 = accent(),
				BackgroundTransparency = 0.15 + 0.7 * (1 - index / count),
				Parent = foot,
			})
			corner(bar, "full")
			bars[index] = bar
		end
		themed(function(animated)
			for _, bar in bars do
				tween(bar, if animated then 0.4 else 0, { BackgroundColor3 = accent() })
			end
		end)
		return function()
			local peak = ceiling()
			for index, bar in bars do
				local height = math.clamp(history[index] / peak, 0.12, 1)
				tween(bar, 0.4, { Size = UDim2.new(1 / count, -3, height, 0) })
			end
		end
	end

	local fpsValue, fpsFoot = tile(1, "pulse", "FRAMERATE", "fps")
	local pingValue, pingFoot = tile(2, "signal", "LATENCY", "ms")
	local playersValue, playersFoot, playersUnit = tile(3, "people", "PLAYERS", "online")
	local sessionValue, sessionFoot = tile(4, "clock", "SESSION", "")

	local fpsBars = sparkline(fpsFoot, Stats.fpsHistory, function()
		local peak = 30
		for _, reading in Stats.fpsHistory do
			peak = math.max(peak, reading)
		end
		return peak
	end)
	local pingBars = sparkline(pingFoot, Stats.pingHistory, function()
		local peak = 40
		for _, reading in Stats.pingHistory do
			peak = math.max(peak, reading)
		end
		return peak
	end)

	local capacity = new("Frame", {
		AnchorPoint = Vector2.new(0, 1),
		Position = UDim2.fromScale(0, 1),
		Size = UDim2.new(1, 0, 0, 4),
		BackgroundColor3 = WHITE,
		BackgroundTransparency = 0.9,
		Parent = playersFoot,
	})
	corner(capacity, "full")
	local capacityFill = new("Frame", {
		Size = UDim2.fromScale(0, 1),
		BackgroundColor3 = accent(),
		BackgroundTransparency = 0,
		Parent = capacity,
	})
	corner(capacityFill, "full")
	gradient(capacityFill, { Transparency = numbers({ { 0, 0.5 }, { 1, 0 } }) })
	themed(function(animated)
		tween(capacityFill, if animated then 0.4 else 0, { BackgroundColor3 = accent() })
	end)

	new("TextLabel", {
		Size = UDim2.fromScale(1, 1),
		Text = "Joined at " .. os.date("%H:%M"),
		TextSize = 11,
		TextTransparency = TEXT.tertiary,
		TextYAlignment = Enum.TextYAlignment.Bottom,
		weight = Enum.FontWeight.Medium,
		Parent = sessionFoot,
	})

	local function refresh()
		fpsValue.Text = tostring(math.floor(Stats.fps + 0.5))
		pingValue.Text = tostring(math.floor(Stats.ping + 0.5))
		local count, maximum = #Players:GetPlayers(), math.max(Players.MaxPlayers, 1)
		playersValue.Text = tostring(count)
		playersUnit.Text = "of " .. maximum
		tween(capacityFill, 0.5, { Size = UDim2.fromScale(math.clamp(count / maximum, 0.02, 1), 1) })
		sessionValue.Text = clockText(os.clock() - SESSION_START)
		fpsBars()
		pingBars()
	end
	table.insert(Stats.listeners, refresh)
	refresh()

	-- Quick settings and shortcuts
	local left, right = columns(page)
	local quick = section(left, "Quick settings")
	toggle(quick, "Ambient motion", "Drifting stars and a living glow", "ambient")
	toggle(quick, "Focus blur", "Soften the world while the menu is open", "focusBlur")
	toggle(quick, "Notifications", "Toasts for saves and changes", "notifications")

	local keys = section(right, "Shortcuts")
	local function shortcut(title, description, key, fixed)
		local slot = row(keys, title, description, 140)
		local cap = keycap(slot, if fixed then fixed else prettyKey(Settings.get(key)), {
			AnchorPoint = Vector2.new(1, 0.5),
			Position = UDim2.new(1, 0, 0.5, 0),
		})
		if key then
			Settings.listen(key, function(name)
				cap.Text = prettyKey(name)
			end)
		end
	end
	shortcut("Show or hide", "Works from anywhere in the game", "menuKey")
	shortcut("Zoom camera", "Hold to look closer", "zoomKey")
	shortcut("Move the window", "Drag the top bar", nil, "Drag")
end

--------------------------------------------------------------------------------
-- Gameplay
--------------------------------------------------------------------------------

do
	local page = newPage("gameplay", "Gameplay", "Camera feel and on-screen aids, applied only to you.")
	local left, right = columns(page)

	local cameraCard = section(left, "Camera")
	slider(cameraCard, "Field of view", "fov", 40, 120, 1, function(value)
		return value .. "°"
	end)
	keybind(cameraCard, "Zoom key", "Hold to look closer", "zoomKey")
	slider(cameraCard, "Zoom field of view", "zoomFov", 10, 50, 1, function(value)
		return value .. "°"
	end)

	local overlayCard = section(right, "Overlay")
	dropdown(overlayCard, "Crosshair", "Centre-screen reticle", "crosshair", CHOICES.crosshair, 132)
	toggle(overlayCard, "Performance chip", "Frame rate and latency in a corner", "perfOverlay")
end

--------------------------------------------------------------------------------
-- Visuals
--------------------------------------------------------------------------------

do
	local page = newPage("visuals", "Visuals", "Real-time post-processing, rendered only on your screen.")
	local left, right = columns(page)

	local post = section(left, "Post-processing")
	toggle(post, "Bloom", "Soft glow on bright surfaces", "bloom")
	toggle(post, "Sun rays", "Light shafts around the sun", "sunRays")
	toggle(post, "Depth of field", "Cinematic distance blur", "depthOfField")

	-- Time of day starts from the server's sky and is never saved, so it is
	-- seeded here instead of living in DEFAULTS.
	local atmosphere = section(left, "Atmosphere")
	Settings.set("clockTime", math.floor(Lighting.ClockTime * 4 + 0.5) / 4)
	slider(atmosphere, "Time of day", "clockTime", 0, 24, 0.25, function(value)
		local hours = math.floor(value) % 24
		local minutes = math.floor((value - math.floor(value)) * 60 + 0.5)
		return string.format("%02d:%02d", hours, minutes)
	end)

	local color = section(right, "Colour")
	dropdown(color, "Tone", "Colour grade preset", "tone", CHOICES.tone, 132)
	slider(color, "Saturation", "saturation", -100, 100, 1, function(value)
		return (if value > 0 then "+" else "") .. value
	end)
	slider(color, "Contrast", "contrast", -100, 100, 1, function(value)
		return (if value > 0 then "+" else "") .. value
	end)
end

--------------------------------------------------------------------------------
-- Settings
--------------------------------------------------------------------------------

local Persistence = { status = "connecting", listeners = {} }

do
	local page = newPage("settings", "Settings", "Make Genesis yours — every change applies instantly.")
	local left, right = columns(page)

	local appearance = section(left, "Appearance")
	swatches(appearance, "Accent", "accent")
	dropdown(appearance, "Typeface", nil, "typeface", CHOICES.typeface, 148)
	segmented(appearance, "Corners", nil, "corners", CHOICES.corners, 62)
	slider(appearance, "Interface scale", "scale", 80, 125, 5, function(value)
		return value .. "%"
	end, true)

	local glass = section(left, "Glass")
	slider(glass, "Glass opacity", "glass", 0, 100, 1, function(value)
		return value .. "%"
	end)
	slider(glass, "Frost", "frost", 0, 100, 1, function(value)
		return value .. "%"
	end)
	slider(glass, "Artwork brightness", "artwork", 20, 100, 1, function(value)
		return value .. "%"
	end)
	toggle(glass, "Edge light", "Specular rim on every pane", "edgeLight")

	local behaviour = section(right, "Behaviour")
	keybind(behaviour, "Menu key", "Show or hide Genesis", "menuKey")
	segmented(behaviour, "Motion", nil, "motion", CHOICES.motion, 62)
	toggle(behaviour, "Focus blur", "Blur and dim the world behind", "focusBlur")
	slider(behaviour, "Blur strength", "blurSize", 2, 24, 1, function(value)
		return tostring(value)
	end)
	toggle(behaviour, "Ambient motion", "Drifting stars and a living glow", "ambient")
	toggle(behaviour, "Notifications", "Toasts for saves and changes", "notifications")
	toggle(behaviour, "Dock", "Floating pill while the menu is hidden", "dock")

	local configuration = section(right, "Configuration")
	toggle(configuration, "Auto-save", "Save changes as you make them", "autosave")
	local slot, _, _, statusLabel = row(configuration, "Cloud", "Connecting…", 150)
	local actions = new("Frame", {
		AnchorPoint = Vector2.new(1, 0.5),
		Position = UDim2.new(1, 0, 0.5, 0),
		Size = UDim2.fromOffset(150, 32),
		Parent = slot,
	})
	list(actions, Enum.FillDirection.Horizontal, 8, Enum.HorizontalAlignment.Right, Enum.VerticalAlignment.Center)
	local saveButton = actionButton(actions, "Save", "primary", function()
		Persistence.save(true)
	end)
	saveButton.LayoutOrder = 1
	local resetButton
	local armed = false
	resetButton = actionButton(actions, "Reset", "danger", function()
		if not armed then
			armed = true
			resetButton.Text = "Confirm"
			task.delay(3, function()
				if armed then
					armed = false
					resetButton.Text = "Reset"
				end
			end)
			return
		end
		armed = false
		resetButton.Text = "Reset"
		Persistence.reset()
	end)
	resetButton.LayoutOrder = 2

	table.insert(Persistence.listeners, function(text)
		statusLabel.Text = text
	end)
end

--------------------------------------------------------------------------------
-- Navigation
--------------------------------------------------------------------------------

for index, item in NAV do
	local button = new("TextButton", {
		Name = item.label,
		Position = UDim2.fromOffset(4 + (index - 1) * TAB_WIDTH, 4),
		Size = UDim2.fromOffset(TAB_WIDTH, 30),
		ZIndex = 4,
		Parent = nav,
	})
	local holder = new("Frame", { Size = UDim2.fromScale(1, 1), ZIndex = 4, Parent = button })
	list(holder, Enum.FillDirection.Horizontal, 7, Enum.HorizontalAlignment.Center, Enum.VerticalAlignment.Center)
	local icon = glyph(holder, item.icon, 15, { LayoutOrder = 1, ZIndex = 4 })
	local text = new("TextLabel", {
		Size = UDim2.fromOffset(0, 30),
		AutomaticSize = Enum.AutomaticSize.X,
		Text = item.label,
		TextSize = 13,
		weight = Enum.FontWeight.Medium,
		LayoutOrder = 2,
		ZIndex = 4,
		Parent = holder,
	})
	local tab = { index = index, hovered = false }
	function tab.paint(seconds)
		local active = Menu.page == item.id
		local transparency = if active then 0 elseif tab.hovered then 0.22 else 0.48
		icon.paint(INK, transparency, seconds)
		tween(text, seconds, { TextTransparency = transparency })
	end
	tabs[item.id] = tab
	button.MouseEnter:Connect(function()
		tab.hovered = true
		tab.paint(0.2)
	end)
	button.MouseLeave:Connect(function()
		tab.hovered = false
		tab.paint(0.25)
	end)
	button.Activated:Connect(function()
		if Menu.collapsed then
			Menu.setCollapsed(false)
		end
		Menu.select(item.id)
	end)
	tab.paint(0)
end

function Menu.select(id, instant)
	if Menu.page == id then
		return
	end
	Overlay.close()
	local previous = pages[Menu.page]
	Menu.page = id
	for _, tab in tabs do
		tab.paint(if instant then 0 else 0.25)
	end
	tween(navThumb, if instant then 0 else 0.45, {
		Position = UDim2.fromOffset(4 + (tabs[id].index - 1) * TAB_WIDTH, 4),
	})

	Menu.pageToken += 1
	local token = Menu.pageToken
	for _, page in pages do
		page.Parent = pageHost
		page.Position = UDim2.new()
	end
	pageFader.Visible = false
	if previous then
		previous.Visible = false
	end
	local page = pages[id]
	page.Visible = true
	if instant or duration(1) <= 0 then
		return
	end
	-- The incoming page rises into place through a fading group, then lands back sharp.
	pageFader.Visible = true
	pageFader.GroupTransparency = 1
	page.Parent = pageFader
	page.Position = UDim2.fromOffset(0, 12)
	tween(pageFader, 0.3, { GroupTransparency = 0 })
	tween(page, 0.5, { Position = UDim2.new() })
	task.delay(duration(0.5), function()
		if Menu.pageToken == token then
			page.Parent = pageHost
			pageFader.Visible = false
		end
	end)
end

--------------------------------------------------------------------------------
-- Toasts
--------------------------------------------------------------------------------

local toastCount = 0

function notify(title, message, iconName)
	if not Settings.get("notifications") then
		return
	end
	toastCount += 1
	-- The slot reserves layout space; the group around it leaves room for the shadow.
	local slot = new("Frame", {
		Name = "Toast",
		Size = UDim2.fromOffset(332, 0),
		LayoutOrder = toastCount,
		Parent = toastLayer,
	})
	local group = new("CanvasGroup", {
		Position = UDim2.fromOffset(40, -12),
		Size = UDim2.fromOffset(332, 106),
		GroupTransparency = 1,
		Parent = slot,
	})
	local cardHolder =
		new("Frame", { Position = UDim2.fromOffset(16, 12), Size = UDim2.fromOffset(300, 72), Parent = group })
	softShadow(cardHolder, RADIUS.card, 12, 0.5, 5, 8)
	local surface = new("Frame", {
		Size = UDim2.fromScale(1, 1),
		BackgroundColor3 = SURFACE,
		BackgroundTransparency = 0.06,
		ZIndex = 2,
		Parent = cardHolder,
	})
	corner(surface, RADIUS.card)
	gradient(surface, { Rotation = 90, Color = colors({ { 0, WHITE }, { 1, Color3.fromRGB(140, 140, 150) } }) })
	rimLight(stroke(surface, 0.78), 1)

	local color = accent()
	local badge = new("Frame", {
		Position = UDim2.fromOffset(14, 17),
		Size = UDim2.fromOffset(36, 36),
		BackgroundColor3 = color,
		BackgroundTransparency = 0.86,
		ZIndex = 3,
		Parent = surface,
	})
	corner(badge, "full")
	stroke(badge, 0.6, color, 1)
	local icon = glyph(badge, iconName or "check", 16, {
		AnchorPoint = Vector2.new(0.5, 0.5),
		Position = UDim2.fromScale(0.5, 0.5),
		ZIndex = 4,
	})
	icon.paint(color, 0)

	new("TextLabel", {
		Position = UDim2.fromOffset(62, 16),
		Size = UDim2.new(1, -76, 0, 18),
		Text = title,
		TextSize = 14,
		TextTruncate = Enum.TextTruncate.AtEnd,
		weight = Enum.FontWeight.SemiBold,
		ZIndex = 3,
		Parent = surface,
	})
	new("TextLabel", {
		Position = UDim2.fromOffset(62, 36),
		Size = UDim2.new(1, -76, 0, 16),
		Text = message,
		TextSize = 12,
		TextTransparency = TEXT.secondary,
		TextTruncate = Enum.TextTruncate.AtEnd,
		ZIndex = 3,
		Parent = surface,
	})
	local progress = new("Frame", {
		AnchorPoint = Vector2.new(0, 1),
		Position = UDim2.new(0, 16, 1, -1),
		Size = UDim2.new(1, -32, 0, 2),
		BackgroundColor3 = color,
		BackgroundTransparency = 0.25,
		ZIndex = 4,
		Parent = surface,
	})
	corner(progress, "full")

	local lifetime = 4
	tween(slot, 0.4, { Size = UDim2.fromOffset(332, 82) })
	tween(group, 0.5, { Position = UDim2.fromOffset(0, -12), GroupTransparency = 0 })
	TweenService:Create(progress, TweenInfo.new(lifetime, Enum.EasingStyle.Linear), {
		Size = UDim2.new(0, 0, 0, 2),
	}):Play()
	task.delay(lifetime, function()
		tween(
			group,
			0.32,
			{ Position = UDim2.fromOffset(40, -12), GroupTransparency = 1 },
			Enum.EasingStyle.Quint,
			Enum.EasingDirection.In
		)
		task.wait(duration(0.25))
		tween(slot, 0.35, { Size = UDim2.fromOffset(332, 0) })
		task.wait(duration(0.4))
		forget(slot)
		slot:Destroy()
	end)
end

--------------------------------------------------------------------------------
-- Dock: the pill that brings the menu back
--------------------------------------------------------------------------------

local Dock = {}
do
	local group = new("CanvasGroup", {
		Name = "Dock",
		AnchorPoint = Vector2.new(0.5, 0),
		Position = UDim2.new(0.5, 0, 0, 2),
		Size = UDim2.fromOffset(0, 46),
		AutomaticSize = Enum.AutomaticSize.X,
		GroupTransparency = 1,
		Visible = false,
		ZIndex = 5,
		Parent = gui,
	})
	padding(group, 2)
	table.insert(compactScales, new("UIScale", { Parent = group }))
	local pill = new("TextButton", {
		Size = UDim2.new(0, 0, 1, 0),
		AutomaticSize = Enum.AutomaticSize.X,
		BackgroundColor3 = SURFACE,
		BackgroundTransparency = 0.12,
		Parent = group,
	})
	corner(pill, "full")
	gradient(pill, { Rotation = 90, Color = colors({ { 0, WHITE }, { 1, Color3.fromRGB(150, 150, 160) } }) })
	rimLight(stroke(pill, 0.74), 1)
	padding(pill, 0, 8, 0, 7)
	list(pill, Enum.FillDirection.Horizontal, 10, Enum.HorizontalAlignment.Left, Enum.VerticalAlignment.Center)
	local mark = brandMark(pill, 28)
	mark.LayoutOrder = 1
	new("TextLabel", {
		Size = UDim2.fromOffset(0, 20),
		AutomaticSize = Enum.AutomaticSize.X,
		Text = CONFIG.Title,
		TextSize = 14,
		weight = Enum.FontWeight.Bold,
		LayoutOrder = 2,
		Parent = pill,
	})
	local cap = keycap(pill, prettyKey(Settings.get("menuKey")), { LayoutOrder = 3 })
	Settings.listen("menuKey", function(name)
		cap.Text = prettyKey(name)
	end)
	local scale = pressable(pill)
	pill.MouseEnter:Connect(function()
		tween(scale, 0.25, { Scale = 1.04 })
	end)
	pill.Activated:Connect(function()
		Menu.setOpen(true)
	end)

	function Dock.set(visible)
		if visible then
			group.Visible = true
		end
		tween(group, if visible then 0.45 else 0.2, {
			GroupTransparency = if visible then 0 else 1,
			Position = UDim2.new(0.5, 0, 0, if visible then 14 else 2),
		})
		if not visible then
			task.delay(duration(0.2), function()
				if not Menu.open and Settings.get("dock") then
					return
				end
				group.Visible = false
			end)
		end
	end
end

--------------------------------------------------------------------------------
-- HUD: crosshair and performance chip
--------------------------------------------------------------------------------

do
	local crosshair = new("Frame", {
		Name = "Crosshair",
		AnchorPoint = Vector2.new(0.5, 0.5),
		Position = UDim2.fromScale(0.5, 0.5),
		Size = UDim2.fromOffset(40, 40),
		Parent = hud,
	})

	local function part(position, size)
		local piece = new("Frame", {
			AnchorPoint = Vector2.new(0.5, 0.5),
			Position = position,
			Size = size,
			BackgroundColor3 = accent(),
			BackgroundTransparency = 0,
			Parent = crosshair,
		})
		corner(piece, "full")
		stroke(piece, 0.55, BLACK, 1)
		return piece
	end

	local function build(style)
		crosshair:ClearAllChildren()
		local center = UDim2.fromScale(0.5, 0.5)
		if style == "Dot" then
			part(center, UDim2.fromOffset(5, 5))
		elseif style == "Cross" then
			part(UDim2.new(0.5, 0, 0.5, -8), UDim2.fromOffset(2, 8))
			part(UDim2.new(0.5, 0, 0.5, 8), UDim2.fromOffset(2, 8))
			part(UDim2.new(0.5, -8, 0.5, 0), UDim2.fromOffset(8, 2))
			part(UDim2.new(0.5, 8, 0.5, 0), UDim2.fromOffset(8, 2))
		elseif style == "Circle" then
			local ring = new("Frame", {
				AnchorPoint = Vector2.new(0.5, 0.5),
				Position = center,
				Size = UDim2.fromOffset(16, 16),
				Parent = crosshair,
			})
			corner(ring, "full")
			stroke(ring, 0.1, accent(), 1.5)
			part(center, UDim2.fromOffset(3, 3))
		end
	end
	Settings.watch("crosshair", build)
	Settings.listen("accent", function()
		build(Settings.get("crosshair"))
	end)

	local chip = new("Frame", {
		Name = "Performance",
		AnchorPoint = Vector2.new(0, 1),
		Position = UDim2.new(0, 16, 1, -16),
		Size = UDim2.fromOffset(0, 30),
		AutomaticSize = Enum.AutomaticSize.X,
		BackgroundColor3 = SURFACE,
		BackgroundTransparency = 0.18,
		Visible = false,
		Parent = hud,
	})
	corner(chip, "full")
	rimLight(stroke(chip, 0.78), 1)
	padding(chip, 0, 12, 0, 10)
	list(chip, Enum.FillDirection.Horizontal, 8, Enum.HorizontalAlignment.Left, Enum.VerticalAlignment.Center)
	local health = new("Frame", {
		Size = UDim2.fromOffset(7, 7),
		BackgroundColor3 = GREEN,
		BackgroundTransparency = 0,
		LayoutOrder = 1,
		Parent = chip,
	})
	corner(health, "full")
	local readout = new("TextLabel", {
		Size = UDim2.fromOffset(0, 30),
		AutomaticSize = Enum.AutomaticSize.X,
		Text = "",
		TextSize = 12,
		weight = Enum.FontWeight.SemiBold,
		LayoutOrder = 2,
		Parent = chip,
	})
	table.insert(Stats.listeners, function()
		local fps = math.floor(Stats.fps + 0.5)
		readout.Text = fps .. " FPS  ·  " .. math.floor(Stats.ping + 0.5) .. " ms"
		health.BackgroundColor3 = if fps >= 50 then GREEN elseif fps >= 30 then AMBER else RED
	end)
	Settings.watch("perfOverlay", function(enabled)
		chip.Visible = enabled
	end)
end

--------------------------------------------------------------------------------
-- World effects
--------------------------------------------------------------------------------

local Effects = {}

-- Finds or creates a client-side post effect in Lighting.
function Effects.get(className, name, create)
	local effect = Lighting:FindFirstChild(name)
	if not effect and create then
		effect = Instance.new(className)
		effect.Name = name
		effect.Parent = Lighting
	end
	return effect
end

function Effects.updateBlur()
	local blur = Effects.get("BlurEffect", "GenesisFocusBlur", true)
	local active = Menu.open and Settings.get("focusBlur")
	tween(blur, 0.45, { Size = if active then Settings.get("blurSize") else 0 })
	tween(backdrop, 0.45, { BackgroundTransparency = if active then 0.74 else 1 })
end
Settings.listen("focusBlur", Effects.updateBlur)
Settings.listen("blurSize", Effects.updateBlur)

Settings.watch("bloom", function(enabled)
	local effect = Effects.get("BloomEffect", "GenesisBloom", enabled)
	if effect then
		effect.Enabled = enabled
		effect.Intensity = 0.7
		effect.Size = 30
		effect.Threshold = 1.6
	end
end)

Settings.watch("sunRays", function(enabled)
	local effect = Effects.get("SunRaysEffect", "GenesisSunRays", enabled)
	if effect then
		effect.Enabled = enabled
		effect.Intensity = 0.08
		effect.Spread = 0.7
	end
end)

Settings.watch("depthOfField", function(enabled)
	local effect = Effects.get("DepthOfFieldEffect", "GenesisDepthOfField", enabled)
	if effect then
		effect.Enabled = enabled
		effect.FarIntensity = 0.32
		effect.FocusDistance = 36
		effect.InFocusRadius = 32
		effect.NearIntensity = 0
	end
end)

local function updateGrade()
	local tone = TONE[Settings.get("tone")] or TONES[1]
	local saturation = tone.saturation + Settings.get("saturation") / 100
	local contrast = tone.contrast + Settings.get("contrast") / 250
	local neutral = tone.name == "Natural" and saturation == 0 and contrast == 0
	local effect = Effects.get("ColorCorrectionEffect", "GenesisGrade", not neutral)
	if effect then
		effect.Enabled = not neutral
		tween(effect, 0.4, {
			Saturation = saturation,
			Contrast = contrast,
			Brightness = tone.brightness,
			TintColor = tone.tint,
		})
	end
end
Settings.watch("tone", updateGrade)
Settings.listen("saturation", updateGrade)
Settings.listen("contrast", updateGrade)

Settings.listen("clockTime", function(value)
	Lighting.ClockTime = value
end)

-- Field of view, with a hold-to-zoom key.
local Zoom = { held = false }
local function applyFieldOfView(seconds)
	local target = if Zoom.held then Settings.get("zoomFov") else Settings.get("fov")
	local currentCamera = workspace.CurrentCamera
	if currentCamera then
		tween(currentCamera, seconds, { FieldOfView = target }, Enum.EasingStyle.Quint)
	end
end
Settings.listen("fov", function()
	applyFieldOfView(0)
end)
Settings.listen("zoomFov", function()
	if Zoom.held then
		applyFieldOfView(0)
	end
end)

--------------------------------------------------------------------------------
-- Live appearance settings
--------------------------------------------------------------------------------

Settings.listen("accent", function()
	for _, hook in themeHooks do
		hook(true)
	end
end)

Settings.listen("typeface", function()
	local current = family()
	for object, weight in fontRegistry do
		object.FontFace = Font.new(current, weight)
	end
end)

Settings.listen("corners", function()
	for object, spec in cornerRegistry do
		object.CornerRadius = UDim.new(0, radiusFor(spec[1], spec[2]))
	end
end)

Settings.listen("glass", function(value)
	for layer in tintLayers do
		layer.BackgroundTransparency = 1 - value / 100
	end
end)

Settings.listen("frost", function(value)
	for layer in frostLayers do
		layer.ImageTransparency = 1 - value / 100
	end
end)

Settings.listen("artwork", function()
	local shade = artShade()
	artRoot.ImageColor3 = shade
	for layer in sampled do
		layer.ImageColor3 = shade
	end
end)

Settings.watch("edgeLight", function(enabled)
	for object in rimStrokes do
		object.Enabled = enabled
	end
	for object in rimLines do
		object.Visible = enabled
	end
end)

-- The window keeps the chosen scale but always fits below Roblox's top bar.
local function fitScale()
	local viewport = workspace.CurrentCamera.ViewportSize
	local inset = GuiService:GetGuiInset()
	local fit = math.min((viewport.X - 24) / WINDOW.X, (viewport.Y - inset.Y - 24) / WINDOW.Y)
	hostScale.Scale = math.max(math.min(Settings.get("scale") / 100, fit), 0.35)
	for _, scale in compactScales do
		scale.Scale = math.clamp(fit, 0.6, 1)
	end
end
Settings.watch("scale", fitScale)

--------------------------------------------------------------------------------
-- Window behaviour
--------------------------------------------------------------------------------

local Window = { offset = Vector2.zero, target = Vector2.zero }

local function clampOffset(offset)
	local viewport = workspace.CurrentCamera.ViewportSize
	local half = WINDOW * hostScale.Scale * 0.5
	local limitX = math.max(viewport.X * 0.5 + half.X - 120, 0)
	local limitY = math.max(viewport.Y * 0.5 + half.Y - 60, 0)
	return Vector2.new(math.clamp(offset.X, -limitX, limitX), math.clamp(offset.Y, -limitY, limitY))
end

dragHandle.InputBegan:Connect(function(input)
	if not isPress(input) then
		return
	end
	Overlay.close()
	local start = pointer(input)
	local origin = Window.target
	Drag.begin(function(point)
		Window.target = clampOffset(origin + (point - start))
	end)
end)

-- Folds the window up to its top bar (and back), fading the content pane as one piece.
function Menu.setCollapsed(collapsed)
	if Menu.collapsed == collapsed then
		return
	end
	Menu.collapsed = collapsed
	Menu.collapseToken += 1
	local token = Menu.collapseToken
	Overlay.close()
	minimizeIcon.Frame.Visible = not collapsed
	restoreIcon.Frame.Visible = collapsed

	local panel = contentPanel.Frame
	local function settle()
		if Menu.collapseToken ~= token then
			return
		end
		panel.Parent = window
		panel.Visible = not collapsed
		collapseFader.Visible = false
	end
	collapseFader.Visible = true
	panel.Parent = collapseFader
	tween(artRoot, 0.5, { ImageTransparency = if collapsed then 0.6 else 0 })
	if collapsed then
		collapseFader.GroupTransparency = 0
		tween(collapseFader, 0.2, { GroupTransparency = 1 })
		tween(shell, 0.5, { Size = UDim2.new(1, 0, 0, COLLAPSED) })
		task.delay(duration(0.5), settle)
	else
		panel.Visible = true
		collapseFader.GroupTransparency = 1
		tween(shell, 0.5, { Size = UDim2.fromScale(1, 1) })
		task.delay(duration(0.16), function()
			if Menu.collapseToken == token then
				tween(collapseFader, 0.36, { GroupTransparency = 0 })
			end
		end)
		task.delay(duration(0.56), settle)
	end
end

function Menu.setOpen(open)
	if Menu.open == open then
		return
	end
	Menu.open = open
	Menu.fadeToken += 1
	local token = Menu.fadeToken
	Overlay.close()
	if KeyCapture.active then
		KeyCapture.active(nil)
	end

	fader.Visible = true
	host.Parent = fader
	host.Visible = true
	if open then
		fader.GroupTransparency = 1
		bodyScale.Scale = 0.94
		tween(fader, 0.35, { GroupTransparency = 0 })
		tween(bodyScale, 0.6, { Scale = 1 }, Enum.EasingStyle.Quint)
	else
		tween(fader, 0.24, { GroupTransparency = 1 }, Enum.EasingStyle.Quint, Enum.EasingDirection.In)
		tween(bodyScale, 0.24, { Scale = 0.96 }, Enum.EasingStyle.Quint, Enum.EasingDirection.In)
	end
	task.delay(duration(if open then 0.6 else 0.26), function()
		if Menu.fadeToken ~= token then
			return
		end
		host.Parent = stage
		host.Visible = open
		fader.Visible = false
	end)

	Effects.updateBlur()
	Dock.set(not open and Settings.get("dock"))
end

Settings.listen("dock", function(enabled)
	if not Menu.open then
		Dock.set(enabled)
	end
end)

minimizeButton.Activated:Connect(function()
	Menu.setCollapsed(not Menu.collapsed)
end)
closeButton.Activated:Connect(function()
	Menu.setOpen(false)
end)

--------------------------------------------------------------------------------
-- Input
--------------------------------------------------------------------------------

UserInputService.InputBegan:Connect(function(input, processed)
	if input.UserInputType ~= Enum.UserInputType.Keyboard then
		return
	end
	local capture = KeyCapture.active
	if capture then
		if input.KeyCode == Enum.KeyCode.Escape then
			capture(nil)
		else
			capture(input.KeyCode.Name)
			notify("Shortcut updated", "Bound to " .. prettyKey(input.KeyCode.Name) .. ".", "check")
		end
		return
	end
	if processed then
		return
	end
	if input.KeyCode.Name == Settings.get("menuKey") then
		Menu.setOpen(not Menu.open)
	elseif input.KeyCode.Name == Settings.get("zoomKey") then
		Zoom.held = true
		applyFieldOfView(0.35)
	end
end)

UserInputService.InputEnded:Connect(function(input)
	if
		input.UserInputType == Enum.UserInputType.Keyboard
		and input.KeyCode.Name == Settings.get("zoomKey")
		and Zoom.held
	then
		Zoom.held = false
		applyFieldOfView(0.45)
	end
end)

--------------------------------------------------------------------------------
-- Persistence
--------------------------------------------------------------------------------

do
	local STATUS_TEXT = {
		connecting = "Connecting…",
		local_only = "This session only",
		saving = "Saving…",
		failed = "Server unreachable",
	}

	local function setStatus(status, detail)
		Persistence.status = status
		local text = detail or STATUS_TEXT[status] or status
		for _, listener in Persistence.listeners do
			listener(text)
		end
	end

	local function snapshot()
		local data = {}
		for key in DEFAULTS do
			data[key] = Settings.get(key)
		end
		return data
	end

	function Persistence.save(manual)
		local remote = Persistence.remote
		if not remote then
			setStatus("local_only")
			if manual then
				notify("Saved for this session", "Install GenesisConfig to sync.", "info")
			end
			return
		end
		setStatus("saving")
		local encoded = HttpService:JSONEncode(snapshot())
		local ok, accepted = pcall(function()
			return remote:InvokeServer("save", encoded)
		end)
		if ok and accepted then
			setStatus("synced", "Saved · " .. os.date("%H:%M"))
			if manual then
				notify("Configuration saved", "Synced to your account.", "check")
			end
		else
			setStatus("failed")
		end
	end

	function Persistence.reset()
		Persistence.applying = true
		for key, value in DEFAULTS do
			Settings.set(key, value)
		end
		Persistence.applying = false
		Persistence.schedule()
		notify("Defaults restored", "Every setting is back to factory.", "info")
	end

	function Persistence.schedule()
		Persistence.due = os.clock() + 1.5
		if Persistence.waiting then
			return
		end
		Persistence.waiting = true
		task.spawn(function()
			while os.clock() < Persistence.due do
				task.wait(0.25)
			end
			Persistence.waiting = false
			Persistence.save(false)
		end)
	end

	Settings.changed(function(key)
		if Persistence.applying or DEFAULTS[key] == nil then
			return
		end
		if Settings.get("autosave") or key == "autosave" then
			Persistence.schedule()
		end
	end)

	task.spawn(function()
		local remote = ReplicatedStorage:WaitForChild(CONFIG.RemoteName, 6)
		if not (remote and remote:IsA("RemoteFunction")) then
			setStatus("local_only")
			return
		end
		Persistence.remote = remote
		local ok, payload = pcall(function()
			return remote:InvokeServer("load")
		end)
		if ok and type(payload) == "string" and payload ~= "" then
			local decoded, data = pcall(function()
				return HttpService:JSONDecode(payload)
			end)
			if decoded and type(data) == "table" then
				Persistence.applying = true
				for key, value in data do
					local clean = Settings.sanitize(key, value)
					if clean ~= nil then
						Settings.set(key, clean)
					end
				end
				Persistence.applying = false
			end
		end
		if ok then
			setStatus("synced", "Synced with your account")
		else
			setStatus("failed")
		end
	end)
end

--------------------------------------------------------------------------------
-- Frame loop
--------------------------------------------------------------------------------

RunService.RenderStepped:Connect(function(dt)
	Stats.step(dt)
	if host.Visible then
		Ambient.step(os.clock())
		local blend = 1 - math.exp(-dt * 20)
		Window.offset = Window.offset:Lerp(Window.target, blend)
		host.Position = UDim2.new(0.5, math.floor(Window.offset.X + 0.5), 0.5, math.floor(Window.offset.Y + 0.5))
	end
end)

local function watchViewport()
	local currentCamera = workspace.CurrentCamera
	if currentCamera then
		currentCamera:GetPropertyChangedSignal("ViewportSize"):Connect(function()
			fitScale()
			Window.target = clampOffset(Window.target)
		end)
		fitScale()
	end
end
workspace:GetPropertyChangedSignal("CurrentCamera"):Connect(watchViewport)
watchViewport()

--------------------------------------------------------------------------------
-- Start
--------------------------------------------------------------------------------

Menu.select("home", true)
gui.Parent = playerGui
task.defer(resampleAll)

if CONFIG.OpenOnStart then
	task.delay(0.35, function()
		Menu.setOpen(true)
		task.wait(0.9)
		notify(
			"Welcome back, " .. player.DisplayName,
			"Toggle the menu with " .. prettyKey(Settings.get("menuKey")) .. ".",
			"sparkle"
		)
	end)
else
	Dock.set(Settings.get("dock"))
end
