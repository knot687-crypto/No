package dev.xeys.visuals.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.util.Colors;
import java.util.Locale;

/**
 * A color with alpha and a dynamic mode: a fixed color, the theme accent (so the whole client recolors
 * with one change), a rainbow cycle or a breathing pulse. {@link #color()} resolves the current value.
 */
public final class ColorSetting extends Setting<Integer> {
	public enum Mode {
		STATIC,
		ACCENT,
		RAINBOW,
		PULSE
	}

	private Mode mode;
	private float speed = 1f;
	private boolean alpha = true;
	private final float[] hsv = new float[3];

	public ColorSetting(String key, String name, int defaultArgb) {
		this(key, name, defaultArgb, Mode.STATIC);
	}

	public ColorSetting(String key, String name, int defaultArgb, Mode defaultMode) {
		super(key, name, defaultArgb);
		this.mode = defaultMode;
		this.defaultMode = defaultMode;
	}

	private final Mode defaultMode;

	public ColorSetting noAlpha() {
		this.alpha = false;
		return this;
	}

	public boolean allowsAlpha() {
		return this.alpha;
	}

	public Mode mode() {
		return this.mode;
	}

	public void mode(Mode mode) {
		if (this.mode != mode) {
			this.mode = mode;
			SettingChanges.markDirty();
		}
	}

	public float speed() {
		return this.speed;
	}

	public void speed(float speed) {
		this.speed = Math.max(0.05f, Math.min(speed, 5f));
		SettingChanges.markDirty();
	}

	/** The stored base color (what the picker edits). */
	public int base() {
		return this.value;
	}

	/** The resolved color right now. */
	public int color() {
		return this.color(0f);
	}

	/** Resolved color with a phase offset (0..1), e.g. to spread a rainbow across several elements. */
	public int color(float offset) {
		int base = this.value;
		return switch (this.mode) {
			case STATIC -> base;
			case ACCENT -> Colors.withAlpha(Theme.accent(), Colors.alpha(base));
			case RAINBOW -> {
				Colors.toHsv(base, this.hsv);
				float hue = (float) ((Clock.time() * 0.12 * this.speed + offset) % 1.0);
				float sat = Math.max(this.hsv[1], 0.55f);
				float val = Math.max(this.hsv[2], 0.75f);
				yield Colors.fromHsv(hue, sat, val, Colors.alpha(base));
			}
			case PULSE -> {
				float t = (float) (0.5 + 0.5 * Math.sin((Clock.time() * this.speed + offset) * Math.PI * 1.4));
				yield Colors.mulAlpha(base, 0.35f + 0.65f * t);
			}
		};
	}

	@Override
	protected Integer sanitize(Integer v) {
		return this.alpha ? v : (v | 0xFF000000);
	}

	@Override
	public void reset() {
		super.reset();
		this.mode = this.defaultMode;
		this.speed = 1f;
	}

	@Override
	public boolean isDefault() {
		return super.isDefault() && this.mode == this.defaultMode && this.speed == 1f;
	}

	@Override
	public JsonElement toJson() {
		JsonObject o = new JsonObject();
		o.addProperty("color", Colors.toHex(this.value, true));
		o.addProperty("mode", this.mode.name().toLowerCase(Locale.ROOT));
		o.addProperty("speed", this.speed);
		return o;
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json instanceof JsonObject o) {
			this.set(o.has("color") ? Colors.parseHex(o.get("color").getAsString(), this.defaultValue) : this.defaultValue);
			Mode parsed = this.defaultMode;
			if (o.has("mode")) {
				for (Mode m : Mode.values()) {
					if (m.name().equalsIgnoreCase(o.get("mode").getAsString())) {
						parsed = m;
					}
				}
			}
			this.mode = parsed;
			this.speed = o.has("speed") ? Math.max(0.05f, Math.min(o.get("speed").getAsFloat(), 5f)) : 1f;
		} else if (json instanceof JsonPrimitive p && p.isString()) {
			this.set(Colors.parseHex(p.getAsString(), this.defaultValue));
		} else {
			this.reset();
		}
	}
}
