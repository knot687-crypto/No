package dev.xeys.visuals.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.util.Colors;

/** A two-stop gradient that can optionally flow over time. */
public final class GradientSetting extends Setting<GradientSetting.Gradient> {
	public record Gradient(int from, int to, boolean flowing, float speed) {
	}

	public GradientSetting(String key, String name, int from, int to) {
		super(key, name, new Gradient(from, to, false, 1f));
	}

	public int from() {
		return this.value.from;
	}

	public int to() {
		return this.value.to;
	}

	public void from(int color) {
		this.set(new Gradient(color, this.value.to, this.value.flowing, this.value.speed));
	}

	public void to(int color) {
		this.set(new Gradient(this.value.from, color, this.value.flowing, this.value.speed));
	}

	public void flowing(boolean flowing) {
		this.set(new Gradient(this.value.from, this.value.to, flowing, this.value.speed));
	}

	/** Color at position t in [0, 1]; a flowing gradient ping-pongs smoothly along the axis. */
	public int at(float t) {
		Gradient g = this.value;
		if (g.flowing) {
			double phase = Clock.time() * 0.35 * g.speed + t;
			t = (float) (0.5 - 0.5 * Math.cos(phase * Math.PI * 2.0));
		}
		return Colors.lerpOklab(g.from, g.to, Math.max(0f, Math.min(1f, t)));
	}

	@Override
	public JsonElement toJson() {
		JsonObject o = new JsonObject();
		o.addProperty("from", Colors.toHex(this.value.from, true));
		o.addProperty("to", Colors.toHex(this.value.to, true));
		o.addProperty("flowing", this.value.flowing);
		o.addProperty("speed", this.value.speed);
		return o;
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json instanceof JsonObject o) {
			Gradient d = this.defaultValue;
			this.set(new Gradient(
				o.has("from") ? Colors.parseHex(o.get("from").getAsString(), d.from) : d.from,
				o.has("to") ? Colors.parseHex(o.get("to").getAsString(), d.to) : d.to,
				o.has("flowing") && o.get("flowing").getAsBoolean(),
				o.has("speed") ? o.get("speed").getAsFloat() : d.speed));
		} else {
			this.reset();
		}
	}
}
