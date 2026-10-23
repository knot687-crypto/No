package dev.xeys.visuals.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class TextSetting extends Setting<String> {
	private final int maxLength;

	public TextSetting(String key, String name, String defaultValue, int maxLength) {
		super(key, name, defaultValue);
		this.maxLength = maxLength;
	}

	public int maxLength() {
		return this.maxLength;
	}

	@Override
	protected String sanitize(String v) {
		String s = v == null ? "" : v.replace('\n', ' ').replace('§', ' ');
		return s.length() > this.maxLength ? s.substring(0, this.maxLength) : s;
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(this.value);
	}

	@Override
	public void fromJson(JsonElement json) {
		this.set(json.isJsonPrimitive() ? json.getAsString() : this.defaultValue);
	}
}
