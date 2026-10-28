package dev.xeys.visuals.hud;

import dev.xeys.visuals.core.module.ModuleManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.ChatComponent;

/**
 * Decides where every visible HUD element sits each frame.
 *
 * <p>Elements the user has placed stay exactly where they were put. Elements still at their shipped
 * position flow instead: they stack along the screen edge of their anchor in a fixed order, wrap into a
 * second column when an edge fills up, keep clear of the crosshair, hotbar and chat, and glide when a
 * neighbour appears, disappears or changes size. So switching elements on never piles them on top of
 * each other, and a growing potion list pushes the elements below it down instead of covering them.
 */
public final class HudLayout {
	private static final float EDGE = 6f;
	private static final float GAP = 4f;
	/** Clearance kept around the crosshair, and above the hotbar with its health and food rows. */
	private static final float CROSSHAIR = 44f;
	private static final float HOTBAR = 60f;
	private static final List<HudModule> VISIBLE = new ArrayList<>();
	/** Rectangles side stacks must not enter: the center stacks and the crosshair (x, y, w, h). */
	private static final float[] KEEP_OUT = new float[4 * 8];
	private static int keepOuts;

	private HudLayout() {
	}

	/** Lays out the elements that are on and have content, places them, and returns them in draw order. */
	public static List<HudModule> compute(float sw, float sh, boolean preview) {
		VISIBLE.clear();
		List<HudModule> all = ModuleManager.hudModules();
		for (int i = 0, n = all.size(); i < n; i++) {
			HudModule m = all.get(i);
			if (m.isEnabled() && m.hasContent(preview)) {
				m.layoutFrame(preview);
				VISIBLE.add(m);
			} else {
				m.invalidateFrame();
			}
		}
		for (int i = 0, n = VISIBLE.size(); i < n; i++) {
			HudModule m = VISIBLE.get(i);
			if (!m.position.isDefault()) {
				m.placeFrame(m.position.left(sw, m.scaledWidth()), m.position.top(sh, m.scaledHeight()), true);
			}
		}
		Options o = Minecraft.getInstance().options;
		float chatTop = sh - 40f - (float) (ChatComponent.getHeight(o.chatHeightUnfocused().get()) * o.chatScale().get());
		keepOuts = 0;
		flowCenter(sw, sh);
		flowSide(0f, sw, sh, chatTop);
		flowSide(1f, sw, sh, sh);
		return VISIBLE;
	}

	private static boolean flows(HudModule m, float fx, float fy) {
		HudPosition.Anchor a = m.position.defaultAnchor();
		return a.fx == fx && a.fy == fy && m.position.isDefault();
	}

	private static float sideX(float fx, float w, float sw, float column) {
		return fx == 0f ? EDGE + column : sw - EDGE - column - w;
	}

	/**
	 * One screen side: the bottom stack grows up from the bottom edge, the top stack grows down and wraps
	 * into another column where it would meet the bottom stack (or the chat), and middle elements center
	 * in whatever room is left.
	 */
	private static void flowSide(float fx, float sw, float sh, float topLimit) {
		float bottom = sh - EDGE;
		for (int i = 0, n = VISIBLE.size(); i < n; i++) {
			HudModule m = VISIBLE.get(i);
			if (flows(m, fx, 1f)) {
				float w = m.scaledWidth();
				float h = m.scaledHeight();
				float py = clear(sideX(fx, w, sw, 0f), bottom - h, w, h, false);
				m.placeFrame(sideX(fx, w, sw, 0f), py, false);
				bottom = py - GAP;
			}
		}
		float limit = Math.min(bottom, topLimit - GAP);
		float y = EDGE;
		float column = 0f;
		float columnW = 0f;
		float firstColumnEnd = EDGE;
		for (int i = 0, n = VISIBLE.size(); i < n; i++) {
			HudModule m = VISIBLE.get(i);
			if (!flows(m, fx, 0f)) {
				continue;
			}
			float w = m.scaledWidth();
			float h = m.scaledHeight();
			float py = clear(sideX(fx, w, sw, column), y, w, h, true);
			// Wrap into further columns until it fits, without crossing the middle of the screen.
			for (int guard = 0; py + h > limit && guard < 6; guard++) {
				float next = column + columnW + GAP;
				if (py <= EDGE || next + w > sw * 0.5f - EDGE) {
					break;
				}
				column = next;
				columnW = 0f;
				py = clear(sideX(fx, w, sw, column), EDGE, w, h, true);
			}
			// Too tall for any free column: stay on screen, even if that means overlapping.
			py = Math.max(EDGE, Math.min(py, sh - EDGE - h));
			m.placeFrame(sideX(fx, w, sw, column), py, false);
			y = py + h + GAP;
			columnW = Math.max(columnW, w);
			if (column == 0f) {
				firstColumnEnd = y;
			}
		}
		float middleH = -GAP;
		float middleW = 0f;
		for (int i = 0, n = VISIBLE.size(); i < n; i++) {
			HudModule m = VISIBLE.get(i);
			if (flows(m, fx, 0.5f)) {
				middleH += m.scaledHeight() + GAP;
				middleW = Math.max(middleW, m.scaledWidth());
			}
		}
		if (middleH <= 0f) {
			return;
		}
		// Centered on the screen if the first column leaves room, otherwise in a column of its own.
		float x = 0f;
		float top = Math.max(sh * 0.5f - middleH * 0.5f, firstColumnEnd);
		if (top + middleH > limit) {
			x = column + columnW + (columnW > 0f ? GAP : 0f);
			top = Math.max(EDGE, Math.min(sh * 0.5f - middleH * 0.5f, limit - middleH));
		}
		for (int i = 0, n = VISIBLE.size(); i < n; i++) {
			HudModule m = VISIBLE.get(i);
			if (flows(m, fx, 0.5f)) {
				m.placeFrame(sideX(fx, m.scaledWidth(), sw, x), top, false);
				top += m.scaledHeight() + GAP;
			}
		}
	}

	/**
	 * The center column: a stack under the top edge, one above the hotbar, and one just below the
	 * crosshair that rises if it would meet the hotbar stack. All three become keep-out zones for the sides.
	 */
	private static void flowCenter(float sw, float sh) {
		float top = EDGE;
		float topW = 0f;
		float bottom = sh - HOTBAR;
		float bottomW = 0f;
		float middleH = -GAP;
		float middleW = 0f;
		for (int i = 0, n = VISIBLE.size(); i < n; i++) {
			HudModule m = VISIBLE.get(i);
			float w = m.scaledWidth();
			float h = m.scaledHeight();
			if (flows(m, 0.5f, 0f)) {
				m.placeFrame((sw - w) * 0.5f, top, false);
				top += h + GAP;
				topW = Math.max(topW, w);
			} else if (flows(m, 0.5f, 1f)) {
				bottom -= h;
				m.placeFrame((sw - w) * 0.5f, bottom, false);
				bottom -= GAP;
				bottomW = Math.max(bottomW, w);
			} else if (flows(m, 0.5f, 0.5f)) {
				middleH += h + GAP;
				middleW = Math.max(middleW, w);
			}
		}
		if (topW > 0f) {
			keepOut((sw - topW) * 0.5f, 0f, topW, top - GAP);
		}
		if (bottomW > 0f) {
			keepOut((sw - bottomW) * 0.5f, bottom + GAP, bottomW, sh - HOTBAR - bottom - GAP);
		}
		if (middleH > 0f) {
			float y = Math.max(sh * 0.5f + 14f, Math.min(sh * 0.5f + CROSSHAIR, bottom - middleH));
			keepOut((sw - middleW) * 0.5f, y, middleW, middleH);
			for (int i = 0, n = VISIBLE.size(); i < n; i++) {
				HudModule m = VISIBLE.get(i);
				if (flows(m, 0.5f, 0.5f)) {
					m.placeFrame((sw - m.scaledWidth()) * 0.5f, y, false);
					y += m.scaledHeight() + GAP;
				}
			}
		}
		keepOut(sw * 0.5f - 14f, sh * 0.5f - 14f, 28f, 28f);
	}

	private static void keepOut(float x, float y, float w, float h) {
		if (keepOuts < KEEP_OUT.length / 4) {
			int k = keepOuts++ * 4;
			KEEP_OUT[k] = x;
			KEEP_OUT[k + 1] = y;
			KEEP_OUT[k + 2] = w;
			KEEP_OUT[k + 3] = h;
		}
	}

	/** Moves a rectangle down (or up) past any keep-out zone it overlaps; returns the new top. */
	private static float clear(float x, float y, float w, float h, boolean down) {
		for (int pass = 0; pass < 8; pass++) {
			boolean moved = false;
			for (int i = 0; i < keepOuts; i++) {
				int k = i * 4;
				float kx = KEEP_OUT[k];
				float ky = KEEP_OUT[k + 1];
				float kw = KEEP_OUT[k + 2];
				float kh = KEEP_OUT[k + 3];
				if (x < kx + kw + GAP && x + w + GAP > kx && y < ky + kh + GAP && y + h + GAP > ky) {
					y = down ? ky + kh + GAP : ky - GAP - h;
					moved = true;
				}
			}
			if (!moved) {
				break;
			}
		}
		return y;
	}
}
