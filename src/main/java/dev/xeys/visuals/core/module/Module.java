package dev.xeys.visuals.core.module;

import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.setting.KeybindSetting;
import dev.xeys.visuals.core.setting.Setting;
import dev.xeys.visuals.core.setting.SettingChanges;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * A feature of the client. Subclasses declare their settings in the constructor with {@link #add} and
 * receive events by implementing interfaces from {@code Events}; they are only subscribed while enabled.
 */
public abstract class Module {
	private static final List<Consumer<Module>> TOGGLE_LISTENERS = new ArrayList<>();

	private final String id;
	private final String name;
	private final String description;
	private final Category category;
	private final char icon;
	private final List<Setting<?>> settings = new ArrayList<>();
	private final List<Setting<?>> settingsView = Collections.unmodifiableList(this.settings);
	private boolean enabled;
	private boolean defaultEnabled;
	private boolean alwaysOn;
	public final KeybindSetting keybind;

	protected Module(String id, String name, Category category, char icon, String description) {
		this.id = id;
		this.name = name;
		this.category = category;
		this.icon = icon;
		this.description = description;
		this.keybind = new KeybindSetting("keybind", "Keybind", KeybindSetting.NONE).describe("Toggle this module with a key");
	}

	public static void onAnyToggle(Consumer<Module> listener) {
		TOGGLE_LISTENERS.add(listener);
	}

	protected final <S extends Setting<?>> S add(S setting) {
		this.settings.add(setting);
		return setting;
	}

	/** Enabled on first launch (before any profile exists). */
	protected final void enabledByDefault() {
		this.defaultEnabled = true;
	}

	/** A settings-only module that cannot be switched off (e.g. Theme). */
	protected final void alwaysOn() {
		this.alwaysOn = true;
		this.defaultEnabled = true;
	}

	public final String id() {
		return this.id;
	}

	public final String name() {
		return this.name;
	}

	public final String description() {
		return this.description;
	}

	public final Category category() {
		return this.category;
	}

	public final char icon() {
		return this.icon;
	}

	public final List<Setting<?>> settings() {
		return this.settingsView;
	}

	public final boolean isEnabled() {
		return this.enabled;
	}

	public final boolean defaultEnabled() {
		return this.defaultEnabled;
	}

	public final boolean isAlwaysOn() {
		return this.alwaysOn;
	}

	public final void toggle() {
		this.setEnabled(!this.enabled);
	}

	public final void setEnabled(boolean on) {
		if (this.alwaysOn) {
			on = true;
		}
		if (on == this.enabled) {
			return;
		}
		this.enabled = on;
		if (on) {
			Events.subscribe(this);
			this.onEnable();
		} else {
			this.onDisable();
			Events.unsubscribe(this);
		}
		SettingChanges.markDirty();
		for (Consumer<Module> l : TOGGLE_LISTENERS) {
			l.accept(this);
		}
	}

	protected void onEnable() {
	}

	protected void onDisable() {
	}

	/** Lower-cased text the ClickGUI search matches against. */
	public String searchText() {
		return (this.name + " " + this.description + " " + this.category.title()).toLowerCase(java.util.Locale.ROOT);
	}
}
