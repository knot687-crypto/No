package dev.xeys.visuals.modules.screen;

import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.anim.Easing;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.hud.HudEditorScreen;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.ui.clickgui.ClickGuiScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.jspecify.annotations.Nullable;

/**
 * Menus and inventories ease in when they open. The motion only ever scales down to the final size, so
 * full-screen backgrounds never leave a gap at the edges.
 */
public final class ScreenTransitionsModule extends Module {
	public enum Style {
		ZOOM,
		RISE,
		DROP
	}

	private static ScreenTransitionsModule instance;

	private final EnumSetting<Style> style = this.add(new EnumSetting<>("style", "Style", Style.ZOOM).group("Transitions"));
	private final NumberSetting duration = this.add(new NumberSetting("duration", "Duration", 0.24f, 0.1f, 0.6f, 0.02f).unit("s").decimals(2)
		.group("Transitions"));

	private @Nullable Screen last;
	private double openedAt;
	private boolean animating;

	public ScreenTransitionsModule() {
		super("screen_transitions", "Screen Transitions", Category.SCREEN, Icons.MAXIMIZE_2, "Menus and inventories ease in when opened");
		instance = this;
	}

	private static boolean eligible(Screen screen) {
		// Client screens animate themselves; chat, loading and title screens should appear instantly.
		return !(screen instanceof ClickGuiScreen || screen instanceof HudEditorScreen || screen instanceof TitleScreen || screen instanceof ChatScreen
			|| screen instanceof LevelLoadingScreen || screen instanceof DeathScreen);
	}

	/** Pushes the transition transform; returns true if the caller must pop it after rendering. */
	public static boolean begin(GuiGraphics g, Screen screen) {
		ScreenTransitionsModule m = instance;
		if (m == null || !m.isEnabled()) {
			return false;
		}
		if (screen != m.last) {
			m.last = screen;
			m.openedAt = Clock.time();
			m.animating = eligible(screen);
		}
		if (!m.animating) {
			return false;
		}
		float t = (float) ((Clock.time() - m.openedAt) / m.duration.value());
		if (t >= 1f) {
			m.animating = false;
			return false;
		}
		float e = Easing.OUT_CUBIC.apply(Math.max(0f, t));
		float w = g.guiWidth();
		float h = g.guiHeight();
		float scale = 1f;
		float dy = 0f;
		switch (m.style.get()) {
			case ZOOM -> scale = 1.04f - 0.04f * e;
			case RISE -> {
				scale = 1.02f - 0.02f * e;
				dy = (1f - e) * 10f;
			}
			case DROP -> {
				scale = 1.02f - 0.02f * e;
				dy = -(1f - e) * 10f;
			}
		}
		g.pose().pushMatrix();
		g.pose().translate(w * 0.5f, h * 0.5f + dy);
		g.pose().scale(scale, scale);
		g.pose().translate(-w * 0.5f, -h * 0.5f);
		return true;
	}
}
