package dev.xeys.visuals.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.xeys.visuals.Xeys;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.module.ModuleManager;
import dev.xeys.visuals.core.setting.Setting;
import dev.xeys.visuals.core.setting.SettingChanges;
import dev.xeys.visuals.hud.HudModule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Persists modules, settings and HUD layout as named JSON profiles in {@code config/xeys/profiles}.
 * Changes autosave after a short quiet period; writes are atomic so a crash never corrupts a profile.
 */
public final class ConfigManager {
	public static final int FORMAT = 1;
	public static final String DEFAULT_PROFILE = "Default";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final long AUTOSAVE_DELAY_MS = 1200L;

	private static Path root;
	private static Path profiles;
	private static Path exports;
	private static String active = DEFAULT_PROFILE;
	private static long savedVersion;
	private static long lastSeenVersion;
	private static long lastChangeAt;
	private static boolean loading;

	private ConfigManager() {
	}

	public static void init() {
		root = FabricLoader.getInstance().getConfigDir().resolve("xeys");
		profiles = root.resolve("profiles");
		exports = root.resolve("exports");
		try {
			Files.createDirectories(profiles);
		} catch (IOException e) {
			Xeys.LOG.error("Could not create config directory {}", profiles, e);
		}
		JsonObject state = readJson(root.resolve("xeys.json"));
		if (state != null && state.has("activeProfile")) {
			active = sanitizeName(state.get("activeProfile").getAsString());
		}
		load(active);
	}

	public static String activeProfile() {
		return active;
	}

	// ------------------------------------------------------------------ autosave

	/** Call every client tick. */
	public static void tick() {
		long version = SettingChanges.version();
		if (version != lastSeenVersion) {
			lastSeenVersion = version;
			lastChangeAt = System.currentTimeMillis();
		}
		if (version != savedVersion && System.currentTimeMillis() - lastChangeAt >= AUTOSAVE_DELAY_MS) {
			save();
		}
	}

	public static void saveIfDirty() {
		if (SettingChanges.version() != savedVersion) {
			save();
		}
	}

	// ------------------------------------------------------------------ profiles

	public static List<String> listProfiles() {
		List<String> names = new ArrayList<>();
		try (Stream<Path> files = Files.list(profiles)) {
			files.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
				String file = p.getFileName().toString();
				names.add(file.substring(0, file.length() - 5));
			});
		} catch (IOException e) {
			Xeys.LOG.warn("Could not list profiles", e);
		}
		if (!names.contains(active)) {
			names.add(active);
		}
		names.sort(String.CASE_INSENSITIVE_ORDER);
		return names;
	}

	public static void switchTo(String name) {
		String target = sanitizeName(name);
		if (target.equals(active)) {
			return;
		}
		save();
		load(target);
		save();
	}

	/** Creates a new profile as a copy of the current state and switches to it. */
	public static String create(String desiredName) {
		String name = uniqueName(sanitizeName(desiredName));
		save();
		active = name;
		save();
		return name;
	}

	public static boolean rename(String from, String to) {
		String source = sanitizeName(from);
		String target = sanitizeName(to);
		if (Files.exists(profilePath(target)) || !Files.exists(profilePath(source))) {
			return false;
		}
		try {
			Files.move(profilePath(source), profilePath(target));
			if (active.equals(source)) {
				active = target;
				writeState();
			}
			return true;
		} catch (IOException e) {
			Xeys.LOG.warn("Could not rename profile {} to {}", source, target, e);
			return false;
		}
	}

	public static boolean delete(String name) {
		String target = sanitizeName(name);
		List<String> all = listProfiles();
		if (all.size() <= 1) {
			return false;
		}
		try {
			Files.deleteIfExists(profilePath(target));
			if (active.equals(target)) {
				all.remove(target);
				load(all.get(0));
			}
			return true;
		} catch (IOException e) {
			Xeys.LOG.warn("Could not delete profile {}", target, e);
			return false;
		}
	}

	// ------------------------------------------------------------------ export / import

	public static String exportString() {
		return GSON.toJson(serialize());
	}

	/** Writes the current profile to {@code config/xeys/exports} and returns the file. */
	public static Path exportFile() throws IOException {
		Files.createDirectories(exports);
		String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT));
		Path file = exports.resolve(active + "-" + stamp + ".json");
		Files.writeString(file, exportString(), StandardCharsets.UTF_8);
		return file;
	}

	/** Imports profile JSON as a new profile and switches to it. Returns the profile name, or null if invalid. */
	public static String importString(String json, String desiredName) {
		JsonObject parsed;
		try {
			JsonElement element = JsonParser.parseString(json.trim());
			if (!element.isJsonObject() || !element.getAsJsonObject().has("modules")) {
				return null;
			}
			parsed = element.getAsJsonObject();
		} catch (RuntimeException e) {
			return null;
		}
		save();
		String name = uniqueName(sanitizeName(desiredName));
		writeJson(profilePath(name), parsed);
		load(name);
		save();
		return name;
	}

	// ------------------------------------------------------------------ io

	private static void load(String name) {
		active = sanitizeName(name);
		JsonObject json = readJson(profilePath(active));
		loading = true;
		try {
			apply(json);
		} finally {
			loading = false;
		}
		savedVersion = SettingChanges.version();
		lastSeenVersion = savedVersion;
		writeState();
	}

	public static void save() {
		if (loading || root == null) {
			return;
		}
		writeJson(profilePath(active), serialize());
		writeState();
		savedVersion = SettingChanges.version();
	}

	private static JsonObject serialize() {
		JsonObject out = new JsonObject();
		out.addProperty("format", FORMAT);
		out.addProperty("client", Xeys.NAME);
		JsonObject modules = new JsonObject();
		for (Module m : ModuleManager.all()) {
			JsonObject mj = new JsonObject();
			mj.addProperty("enabled", m.isEnabled());
			JsonObject settings = new JsonObject();
			settings.add(m.keybind.key(), m.keybind.toJson());
			for (Setting<?> s : m.settings()) {
				JsonElement v = s.toJson();
				if (!v.isJsonNull()) {
					settings.add(s.key(), v);
				}
			}
			mj.add("settings", settings);
			if (m instanceof HudModule hud) {
				mj.add("hud", hud.position.toJson());
			}
			modules.add(m.id(), mj);
		}
		out.add("modules", modules);
		return out;
	}

	/** Applies a profile; null applies factory defaults. Unknown keys are ignored, missing ones reset. */
	private static void apply(JsonObject json) {
		JsonObject modules = json != null && json.has("modules") && json.get("modules").isJsonObject() ? json.getAsJsonObject("modules") : new JsonObject();
		for (Module m : ModuleManager.all()) {
			JsonObject mj = modules.has(m.id()) && modules.get(m.id()).isJsonObject() ? modules.getAsJsonObject(m.id()) : null;
			JsonObject settings = mj != null && mj.has("settings") && mj.get("settings").isJsonObject() ? mj.getAsJsonObject("settings") : new JsonObject();
			for (Setting<?> s : m.settings()) {
				if (settings.has(s.key())) {
					s.fromJson(settings.get(s.key()));
				} else {
					s.reset();
				}
			}
			if (settings.has(m.keybind.key())) {
				m.keybind.fromJson(settings.get(m.keybind.key()));
			} else {
				m.keybind.reset();
			}
			if (m instanceof HudModule hud) {
				if (mj != null && mj.has("hud") && mj.get("hud").isJsonObject()) {
					hud.position.fromJson(mj.getAsJsonObject("hud"));
				} else {
					hud.position.reset();
				}
			}
			boolean enabled = mj != null && mj.has("enabled") ? mj.get("enabled").getAsBoolean() : m.defaultEnabled();
			m.setEnabled(enabled);
		}
	}

	private static void writeState() {
		if (root == null) {
			return;
		}
		JsonObject state = new JsonObject();
		state.addProperty("activeProfile", active);
		writeJson(root.resolve("xeys.json"), state);
	}

	private static Path profilePath(String name) {
		return profiles.resolve(name + ".json");
	}

	private static String uniqueName(String base) {
		String name = base;
		int i = 2;
		while (Files.exists(profilePath(name))) {
			name = base + " " + i++;
		}
		return name;
	}

	/** Keeps profile names filesystem-safe on every platform. */
	public static String sanitizeName(String name) {
		String s = name == null ? "" : name.replaceAll("[^A-Za-z0-9 _\\-]", "").trim();
		if (s.length() > 32) {
			s = s.substring(0, 32).trim();
		}
		return s.isEmpty() ? DEFAULT_PROFILE : s;
	}

	private static JsonObject readJson(Path path) {
		if (!Files.isRegularFile(path)) {
			return null;
		}
		try {
			JsonElement e = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
			return e.isJsonObject() ? e.getAsJsonObject() : null;
		} catch (IOException | RuntimeException e) {
			Xeys.LOG.error("Could not read {}; using defaults", path, e);
			return null;
		}
	}

	private static void writeJson(Path path, JsonObject json) {
		try {
			Files.createDirectories(path.getParent());
			Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
			Files.writeString(tmp, GSON.toJson(json), StandardCharsets.UTF_8);
			Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			Xeys.LOG.error("Could not write {}", path, e);
		}
	}
}
