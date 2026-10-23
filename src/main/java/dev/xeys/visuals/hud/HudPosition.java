package dev.xeys.visuals.hud;

import com.google.gson.JsonObject;
import dev.xeys.visuals.core.setting.SettingChanges;

/**
 * Where a HUD element sits: one of nine screen anchors plus an offset in GUI pixels, and a scale. The
 * element's matching corner/edge is pinned to the anchor, so elements stay put across resolutions and
 * GUI scales (a top-right element stays top-right).
 */
public final class HudPosition {
	public enum Anchor {
		TOP_LEFT(0f, 0f),
		TOP_CENTER(0.5f, 0f),
		TOP_RIGHT(1f, 0f),
		MIDDLE_LEFT(0f, 0.5f),
		CENTER(0.5f, 0.5f),
		MIDDLE_RIGHT(1f, 0.5f),
		BOTTOM_LEFT(0f, 1f),
		BOTTOM_CENTER(0.5f, 1f),
		BOTTOM_RIGHT(1f, 1f);

		public final float fx;
		public final float fy;

		Anchor(float fx, float fy) {
			this.fx = fx;
			this.fy = fy;
		}

		static Anchor of(int col, int row) {
			return values()[row * 3 + col];
		}
	}

	private Anchor anchor;
	private float x;
	private float y;
	private float scale;
	private final Anchor defaultAnchor;
	private final float defaultX;
	private final float defaultY;

	public HudPosition(Anchor anchor, float x, float y) {
		this.anchor = anchor;
		this.x = x;
		this.y = y;
		this.scale = 1f;
		this.defaultAnchor = anchor;
		this.defaultX = x;
		this.defaultY = y;
	}

	public Anchor anchor() {
		return this.anchor;
	}

	public float scale() {
		return this.scale;
	}

	public void setScale(float scale) {
		float s = Math.max(0.4f, Math.min(scale, 3f));
		if (s != this.scale) {
			this.scale = s;
			SettingChanges.markDirty();
		}
	}

	/** Left edge in GUI pixels for an element of the given (scaled) width. */
	public float left(float screenW, float elemW) {
		return this.anchor.fx * screenW + this.x - this.anchor.fx * elemW;
	}

	public float top(float screenH, float elemH) {
		return this.anchor.fy * screenH + this.y - this.anchor.fy * elemH;
	}

	/**
	 * Re-anchors from a top-left position (e.g. after dragging): the anchor follows whichever third of
	 * the screen holds the element's center, then the offset is recomputed relative to it.
	 */
	public void setFromTopLeft(float left, float top, float elemW, float elemH, float screenW, float screenH) {
		float cx = left + elemW * 0.5f;
		float cy = top + elemH * 0.5f;
		int col = cx < screenW / 3f ? 0 : (cx > screenW * 2f / 3f ? 2 : 1);
		int row = cy < screenH / 3f ? 0 : (cy > screenH * 2f / 3f ? 2 : 1);
		Anchor a = Anchor.of(col, row);
		this.anchor = a;
		this.x = left - a.fx * screenW + a.fx * elemW;
		this.y = top - a.fy * screenH + a.fy * elemH;
		SettingChanges.markDirty();
	}

	public void reset() {
		this.anchor = this.defaultAnchor;
		this.x = this.defaultX;
		this.y = this.defaultY;
		this.scale = 1f;
		SettingChanges.markDirty();
	}

	public JsonObject toJson() {
		JsonObject o = new JsonObject();
		o.addProperty("anchor", this.anchor.name().toLowerCase(java.util.Locale.ROOT));
		o.addProperty("x", this.x);
		o.addProperty("y", this.y);
		o.addProperty("scale", this.scale);
		return o;
	}

	public void fromJson(JsonObject o) {
		try {
			this.anchor = Anchor.valueOf(o.get("anchor").getAsString().toUpperCase(java.util.Locale.ROOT));
			this.x = o.get("x").getAsFloat();
			this.y = o.get("y").getAsFloat();
			this.scale = Math.max(0.4f, Math.min(o.get("scale").getAsFloat(), 3f));
		} catch (RuntimeException e) {
			this.reset();
		}
	}
}
