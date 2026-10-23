package dev.xeys.visuals.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.Locale;

/** A slider value with range, step and a display unit. */
public final class NumberSetting extends Setting<Float> {
	private final float min;
	private final float max;
	private final float step;
	private String unit = "";
	private int decimals;

	public NumberSetting(String key, String name, float defaultValue, float min, float max, float step) {
		super(key, name, defaultValue);
		this.min = min;
		this.max = max;
		this.step = step;
		this.decimals = step >= 1f ? 0 : (step >= 0.1f ? 1 : 2);
	}

	public NumberSetting unit(String unit) {
		this.unit = unit;
		return this;
	}

	public NumberSetting decimals(int decimals) {
		this.decimals = decimals;
		return this;
	}

	public float value() {
		return this.value;
	}

	public int intValue() {
		return Math.round(this.value);
	}

	public float min() {
		return this.min;
	}

	public float max() {
		return this.max;
	}

	public float step() {
		return this.step;
	}

	/** Position in [0, 1] across the range. */
	public float fraction() {
		return this.max > this.min ? (this.value - this.min) / (this.max - this.min) : 0f;
	}

	public void setFraction(float t) {
		this.set(this.min + (this.max - this.min) * Math.max(0f, Math.min(1f, t)));
	}

	@Override
	protected Float sanitize(Float v) {
		float f = v.isNaN() ? this.defaultValue : v;
		if (this.step > 0f) {
			f = this.min + Math.round((f - this.min) / this.step) * this.step;
		}
		f = Math.max(this.min, Math.min(this.max, f));
		// Quantize to avoid float noise like 0.30000001 in configs.
		float q = (float) Math.pow(10, Math.max(this.decimals, 2));
		return Math.round(f * q) / q;
	}

	public String format() {
		String number = this.decimals == 0 ? Integer.toString(Math.round(this.value))
			: String.format(Locale.ROOT, "%." + this.decimals + "f", this.value);
		return number + this.unit;
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(this.value);
	}

	@Override
	public void fromJson(JsonElement json) {
		this.set(json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber() ? json.getAsFloat() : this.defaultValue);
	}
}
