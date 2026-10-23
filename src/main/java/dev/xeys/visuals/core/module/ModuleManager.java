package dev.xeys.visuals.core.module;

import dev.xeys.visuals.hud.HudModule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Registry of every module, by id, category and HUD subset. Populated once at startup. */
public final class ModuleManager {
	private static final List<Module> ALL = new ArrayList<>();
	private static final Map<String, Module> BY_ID = new HashMap<>();
	private static final Map<Category, List<Module>> BY_CATEGORY = new EnumMap<>(Category.class);
	private static final List<HudModule> HUD = new ArrayList<>();

	static {
		for (Category c : Category.values()) {
			BY_CATEGORY.put(c, new ArrayList<>());
		}
	}

	private ModuleManager() {
	}

	public static <M extends Module> M register(M module) {
		if (BY_ID.putIfAbsent(module.id(), module) != null) {
			throw new IllegalStateException("Duplicate module id " + module.id());
		}
		ALL.add(module);
		BY_CATEGORY.get(module.category()).add(module);
		if (module instanceof HudModule hud) {
			HUD.add(hud);
		}
		return module;
	}

	public static List<Module> all() {
		return Collections.unmodifiableList(ALL);
	}

	public static List<Module> byCategory(Category category) {
		return Collections.unmodifiableList(BY_CATEGORY.get(category));
	}

	public static List<HudModule> hudModules() {
		return HUD;
	}

	public static Module get(String id) {
		return BY_ID.get(id);
	}

	public static int count() {
		return ALL.size();
	}

	public static int enabledCount(Category category) {
		int n = 0;
		for (Module m : BY_CATEGORY.get(category)) {
			if (m.isEnabled() && !m.isAlwaysOn()) {
				n++;
			}
		}
		return n;
	}
}
