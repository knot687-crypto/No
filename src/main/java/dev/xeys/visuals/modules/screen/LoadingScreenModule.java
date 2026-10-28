package dev.xeys.visuals.modules.screen;

import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.network.chat.Component;

/** World loading with a progress ring and percentage instead of the chunk map. */
public final class LoadingScreenModule extends Module {
	private static final Component LABEL = Component.translatable("multiplayer.downloadingTerrain");
	private static LoadingScreenModule instance;
	private final StringBuilder percent = new StringBuilder(5);
	private String label;

	public LoadingScreenModule() {
		super("loading_screen", "Loading Screen", Category.SCREEN, Icons.HOURGLASS, "A progress ring while worlds load");
		instance = this;
		this.enabledByDefault();
	}

	public static boolean active() {
		return instance != null && instance.isEnabled();
	}

	/** {@code progress} in 0..1, or negative when unknown (an indeterminate spinner). */
	public static void draw(GuiGraphics g, LevelLoadingScreen screen, float progress) {
		if (!active()) {
			return;
		}
		LoadingScreenModule m = instance;
		if (m.label == null) {
			// The animated dots replace any trailing ellipsis in the translation.
			m.label = LABEL.getString().replaceAll("[.\u2026\s]+$", "");
		}
		float cx = screen.width * 0.5f;
		// Below the menu moon (centered at 40% height, radius 26%).
		float cy = screen.height * 0.76f;
		float r = 22f;
		Draw.ring(g, cx, cy, r, 3f, Colors.withAlpha(0xFFFFFF, 0.1f));
		if (progress >= 0f) {
			float p = Math.max(0.02f, Math.min(progress, 1f));
			Draw.arcGradient(g, cx, cy, r, 3f, 0f, 360f * p, Theme.accent(0.55f), Theme.accent());
			m.percent.setLength(0);
			m.percent.append(Math.round(p * 100f)).append('%');
			Text.drawMiddle(g, Fonts.SEMIBOLD, m.percent, cx, cy, 10f, Theme.TEXT, Text.Align.CENTER);
		} else {
			float spin = (float) ((Clock.time() * 300.0) % 360.0);
			Draw.arcGradient(g, cx, cy, r, 3f, spin, 100f, Theme.accent(0f), Theme.accent());
		}
		int dots = (int) (Clock.time() * 2.5) % 4;
		float lw = Fonts.MEDIUM.width(m.label, 10f);
		Text.drawMiddle(g, Fonts.MEDIUM, m.label, cx - lw * 0.5f, cy + r + 18f, 10f, Theme.TEXT_SECONDARY, Text.Align.LEFT);
		float dx = cx + lw * 0.5f + 1f;
		for (int i = 0; i < 3; i++) {
			Draw.circle(g, dx + 2f + i * 4f, cy + r + 21f, 1f, Colors.mulAlpha(Theme.TEXT_SECONDARY, i < dots ? 1f : 0.25f));
		}
	}
}
