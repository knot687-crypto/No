package dev.xeys.visuals.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class BoolSetting extends Setting<Boolean> {
	public BoolSetting(String key, String name, boolean defaultValue) {
		super(key, name, defaultValue);
	}

	public boolean on() {
		return this.value;
	}

	public void toggle() {
		this.set(!this.value);
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(this.value);
	}

	@Override
	public void fromJson(JsonElement json) {
		this.set(json.isJsonPrimitive() && json.getAsJsonPrimitive().isBoolean() ? json.getAsBoolean() : this.defaultValue);
	}
}
