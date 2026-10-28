package dev.xeys.visuals.hud;

import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.module.ModuleManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds a free spot for an element that is switched on at its shipped position but would land on top
 * of another element (or vanilla's hotbar and chat). It keeps the default spot when that is free, else
 * slides along the same screen edge, then tries the other anchors. Elements the user has moved are
 * never touched.
 */
public final class HudPlacement {
	private static final float GAP = 3f;
	private static final float STEP = 2f;
	private static final float EDGE = 6f;
	private static final Set<HudModule> PENDING = new LinkedHashSet<>();
	private static final List<float[]> OBSTACLES = new ArrayList<>();
	private static boolean listening;

	private HudPlacement() {
	}

	/** Starts watching toggles. Called once the configuration is loaded, so startup state is not "new". */
	public static void install() {
		if (!listening) {
			listening = true;
			Module.onAnyToggle(m -> {
				if (m instanceof HudModule h && h.isEnabled() && h.position.isDefault()) {
					PENDING.add(h);
				}
			});
		}
	}

	public static void request(HudModule m) {
		PENDING.add(m);
	}

	/** Places pending elements. Call after the frame's elements were laid out, so sizes are current. */
	public static void resolve(float sw, float sh, boolean preview) {
		if (PENDING.isEmpty()) {
			return;
		}
		Iterator<HudModule> it = PENDING.iterator();
		while (it.hasNext()) {
			HudModule m = it.next();
			if (!m.isEnabled() || !m.position.isDefault()) {
				it.remove();
				continue;
			}
			if (!m.laidOut() || !m.hasContent(preview)) {
				// Wait until it has something to show (e.g. the Target HUD needs a target) and a real size.
				continue;
			}
			collectObstacles(m, sw, sh, preview);
			place(m, sw, sh);
			it.remove();
		}
	}

	private static void collectObstacles(HudModule self, float sw, float sh, boolean preview) {
		OBSTACLES.clear();
		for (HudModule o : ModuleManager.hudModules()) {
			if (o == self || !o.isEnabled() || !o.laidOut() || PENDING.contains(o) || !o.hasContent(preview)) {
				continue;
			}
			float w = o.scaledWidth();
			float h = o.scaledHeight();
			OBSTACLES.add(new float[] {o.position.left(sw, w), o.position.top(sh, h), w, h});
		}
		// Vanilla: hotbar with health, food and experience above it, and the chat area.
		OBSTACLES.add(new float[] {sw * 0.5f - 100f, sh - 58f, 200f, 58f});
		OBSTACLES.add(new float[] {0f, sh - 150f, Math.min(330f, sw * 0.5f), 114f});
	}

	private static boolean free(float x, float y, float w, float h, float sw, float sh) {
		if (x < 0f || y < 0f || x + w > sw || y + h > sh) {
			return false;
		}
		for (int i = 0; i < OBSTACLES.size(); i++) {
			float[] o = OBSTACLES.get(i);
			if (x < o[0] + o[2] + GAP && x + w + GAP > o[0] && y < o[1] + o[3] + GAP && y + h + GAP > o[1]) {
				return false;
			}
		}
		return true;
	}

	private static void place(HudModule m, float sw, float sh) {
		HudPosition p = m.position;
		float w = m.scaledWidth();
		float h = m.scaledHeight();
		HudPosition.Anchor home = p.defaultAnchor();
		if (free(p.left(sw, w), p.top(sh, h), w, h, sw, sh)) {
			return;
		}
		for (HudPosition.Anchor a : preference(home)) {
			float x = a == home ? p.defaultX() : (a.fx == 0f ? EDGE : (a.fx == 1f ? -EDGE : 0f));
			float y0 = a == home ? p.defaultY() : (a.fy == 0f ? EDGE : (a.fy == 1f ? -EDGE : 0f));
			float left = a.fx * sw + x - a.fx * w;
			int steps = (int) (sh / STEP);
			for (int k = 0; k <= steps; k++) {
				float y;
				if (a.fy == 0.5f) {
					// Middle anchors search outwards: 0, +2, -2, +4, ...
					y = y0 + ((k & 1) == 0 ? 1f : -1f) * ((k + 1) / 2) * STEP;
				} else {
					y = y0 + (a.fy == 0f ? 1f : -1f) * k * STEP;
				}
				float top = a.fy * sh + y - a.fy * h;
				if (top < 0f || top + h > sh) {
					if (a.fy != 0.5f) {
						break;
					}
					continue;
				}
				if (free(left, top, w, h, sw, sh)) {
					p.place(a, x, y);
					return;
				}
			}
		}
	}

	/** The home anchor, then its screen side top to bottom, then the opposite side, then the center column. */
	private static List<HudPosition.Anchor> preference(HudPosition.Anchor home) {
		List<HudPosition.Anchor> list = new ArrayList<>(9);
		list.add(home);
		float[] columns = home.fx == 0.5f ? new float[] {0.5f, 1f, 0f} : new float[] {home.fx, 1f - home.fx, 0.5f};
		for (float col : columns) {
			for (HudPosition.Anchor a : HudPosition.Anchor.values()) {
				if (a.fx == col && !list.contains(a)) {
					list.add(a);
				}
			}
		}
		return list;
	}
}
