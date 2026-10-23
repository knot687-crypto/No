package dev.xeys.visuals.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;

/**
 * A key or mouse button. Stored as an int: keyboard key codes as-is, mouse buttons as
 * {@code MOUSE_BASE - button}, {@link #NONE} when unbound.
 */
public final class KeybindSetting extends Setting<Integer> {
	public static final int NONE = InputConstants.UNKNOWN.getValue();
	public static final int MOUSE_BASE = -100;

	public enum Behavior {
		TOGGLE,
		HOLD
	}

	private Behavior behavior = Behavior.TOGGLE;

	public KeybindSetting(String key, String name, int defaultKey) {
		super(key, name, defaultKey);
	}

	public int code() {
		return this.value;
	}

	public boolean isBound() {
		return this.value != NONE;
	}

	public Behavior behavior() {
		return this.behavior;
	}

	public KeybindSetting behavior(Behavior behavior) {
		this.behavior = behavior;
		return this;
	}

	public boolean matchesKey(int keyCode) {
		return this.value == keyCode && keyCode != NONE;
	}

	public boolean matchesMouse(int button) {
		return this.value == MOUSE_BASE - button;
	}

	public String displayName() {
		int code = this.value;
		if (code == NONE) {
			return "None";
		}
		if (code <= MOUSE_BASE) {
			int button = MOUSE_BASE - code;
			return switch (button) {
				case 0 -> "Left Mouse";
				case 1 -> "Right Mouse";
				case 2 -> "Middle Mouse";
				default -> "Mouse " + (button + 1);
			};
		}
		return InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString();
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(this.value);
	}

	@Override
	public void fromJson(JsonElement json) {
		this.set(json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber() ? json.getAsInt() : this.defaultValue);
	}
}
