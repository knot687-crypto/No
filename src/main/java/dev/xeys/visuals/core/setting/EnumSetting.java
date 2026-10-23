package dev.xeys.visuals.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.Locale;

/** A choice between style variants. Display names come from {@link #label(Enum)}. */
public final class EnumSetting<E extends Enum<E>> extends Setting<E> {
	private final E[] options;

	public EnumSetting(String key, String name, E defaultValue) {
		super(key, name, defaultValue);
		this.options = defaultValue.getDeclaringClass().getEnumConstants();
	}

	public E[] options() {
		return this.options;
	}

	public boolean is(E option) {
		return this.value == option;
	}

	public void cycle(int direction) {
		int i = Math.floorMod(this.value.ordinal() + direction, this.options.length);
		this.set(this.options[i]);
	}

	/** "SOFT_GLOW" -> "Soft Glow", unless the enum overrides toString with a display name. */
	public static String label(Enum<?> e) {
		String s = e.toString();
		if (!s.equals(e.name())) {
			return s;
		}
		StringBuilder out = new StringBuilder(s.length());
		boolean upper = true;
		for (char c : s.toCharArray()) {
			if (c == '_') {
				out.append(' ');
				upper = true;
			} else {
				out.append(upper ? c : Character.toLowerCase(c));
				upper = false;
			}
		}
		return out.toString();
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(this.value.name().toLowerCase(Locale.ROOT));
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json.isJsonPrimitive()) {
			String name = json.getAsString();
			for (E option : this.options) {
				if (option.name().equalsIgnoreCase(name)) {
					this.set(option);
					return;
				}
			}
		}
		this.set(this.defaultValue);
	}
}
