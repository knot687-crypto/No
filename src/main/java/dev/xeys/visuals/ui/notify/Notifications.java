package dev.xeys.visuals.ui.notify;

import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.anim.Spring;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.modules.client.NotificationsModule;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.core.UiScale;
import dev.xeys.visuals.util.Colors;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.jspecify.annotations.Nullable;

/** Glass toast notifications, drawn above every screen. */
public final class Notifications {
	private static final float WIDTH = 252f;
	private static final float HEIGHT = 56f;
	private static final float GAP = 10f;
	private static final float MARGIN = 22f;
	private static final int MAX = 5;
	private static final List<Toast> TOASTS = new ArrayList<>();

	private Notifications() {
	}

	private static final class Toast {
		String title;
		String message;
		char icon;
		int color;
		double born;
		float duration;
		@Nullable Object key;
		boolean leaving;
		final Spring slide = new Spring(1f, 260f, 0.86f);
		final Spring y = new Spring(0f, 300f, 1f);
		boolean placed;
	}

	public static void push(String title, String message, char icon, int color) {
		push(title, message, icon, color, null);
	}

	/** Toasts with the same key replace each other instead of stacking (e.g. repeated toggles). */
	public static void push(String title, String message, char icon, int color, @Nullable Object key) {
		NotificationsModule cfg = NotificationsModule.get();
		if (cfg != null && !cfg.isEnabled()) {
			return;
		}
		float duration = cfg == null ? 2.4f : cfg.duration.value();
		if (key != null) {
			for (Toast t : TOASTS) {
				if (key.equals(t.key) && !t.leaving) {
					t.title = title;
					t.message = message;
					t.icon = icon;
					t.color = color;
					t.born = Clock.time();
					t.duration = duration;
					return;
				}
			}
		}
		Toast t = new Toast();
		t.title = title;
		t.message = message;
		t.icon = icon;
		t.color = color;
		t.key = key;
		t.born = Clock.time();
		t.duration = duration;
		t.slide.target(0f);
		TOASTS.add(0, t);
		while (TOASTS.size() > MAX) {
			TOASTS.get(TOASTS.size() - 1).leaving = true;
			break;
		}
	}

	public static void moduleToggled(Module module) {
		NotificationsModule cfg = NotificationsModule.get();
		if (cfg == null || !cfg.moduleToggles.on() || module.isAlwaysOn()) {
			return;
		}
		boolean on = module.isEnabled();
		push(module.name(), on ? "Enabled" : "Disabled", module.icon(), on ? Theme.accent() : Theme.TEXT_MUTED, module);
	}

	public static void render(GuiRenderState state) {
		if (TOASTS.isEmpty()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		NotificationsModule cfg = NotificationsModule.get();
		NotificationsModule.Corner corner = cfg == null ? NotificationsModule.Corner.TOP_RIGHT : cfg.corner.get();
		state.nextStratum();
		GuiGraphics g = new GuiGraphics(mc, state, 0, 0);
		UiScale.push(g);
		float W = UiScale.width();
		float H = UiScale.height();
		double now = Clock.time();

		float stack = 0f;
		for (int i = 0; i < TOASTS.size(); i++) {
			Toast t = TOASTS.get(i);
			if (!t.leaving && now - t.born > t.duration) {
				t.leaving = true;
			}
			if (t.leaving) {
				t.slide.target(1.05f);
			}
			t.y.target(stack);
			if (!t.placed) {
				t.y.snap(stack);
				t.placed = true;
			}
			if (!t.leaving) {
				stack += HEIGHT + GAP;
			}
		}

		for (int i = TOASTS.size() - 1; i >= 0; i--) {
			Toast t = TOASTS.get(i);
			float slide = t.slide.get();
			if (t.leaving && slide >= 1.0f) {
				TOASTS.remove(i);
				continue;
			}
			float alpha = Math.max(0f, Math.min(1f, 1f - slide * 1.15f));
			float x;
			float y;
			switch (corner) {
				case BOTTOM_RIGHT -> {
					x = W - MARGIN - WIDTH + slide * (WIDTH + MARGIN);
					y = H - MARGIN - HEIGHT - t.y.get();
				}
				case TOP_CENTER -> {
					x = (W - WIDTH) * 0.5f;
					y = MARGIN + t.y.get() - slide * 30f;
				}
				default -> {
					x = W - MARGIN - WIDTH + slide * (WIDTH + MARGIN);
					y = MARGIN + t.y.get();
				}
			}
			drawToast(g, t, x, y, alpha, now);
		}
		UiScale.pop(g);
	}

	private static void drawToast(GuiGraphics g, Toast t, float x, float y, float a, double now) {
		float r = 14f;
		Draw.shadow(g, x, y, WIDTH, HEIGHT, r, 0f, 8f, 26f, -2f, Colors.withAlpha(0x000000, 0.5f * a));
		Draw.glass(g, x, y, WIDTH, HEIGHT, 0xD6070708).radius(r).border(1f, Theme.GLASS_BORDER).borderFade(0.3f).innerGlow(12f, 0.4f).alpha(a).submit();
		float ix = x + 14f + 15f;
		float iy = y + HEIGHT * 0.5f;
		Draw.circleShape(g, ix, iy, 15f).color(Colors.withAlpha(t.color, 0.16f * a)).border(1f, Colors.withAlpha(t.color, 0.35f * a)).submit();
		Text.icon(g, t.icon, ix, iy, 15f, Colors.mulAlpha(t.color, a));
		float tx = ix + 15f + 12f;
		Text.drawMiddle(g, Fonts.SEMIBOLD, t.title, tx, y + 21f, 13f, Colors.mulAlpha(Theme.TEXT, a), Text.Align.LEFT);
		Text.drawMiddle(g, Fonts.REGULAR, t.message, tx, y + 37f, 11.5f, Colors.mulAlpha(Theme.TEXT_MUTED, a), Text.Align.LEFT);
		float life = (float) Math.max(0.0, Math.min(1.0, (now - t.born) / t.duration));
		float barW = (WIDTH - 2f * r) * (1f - life);
		if (barW > 0.5f) {
			Draw.rect(g, x + r, y + HEIGHT - 2.5f, barW, 1.5f).pill().color(Colors.withAlpha(t.color, 0.55f * a)).submit();
		}
	}
}
