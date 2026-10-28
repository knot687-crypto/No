package dev.xeys.visuals.modules.screen;

import dev.xeys.visuals.anim.Easing;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** A cinematic death screen: a red-tinted frosted world, a large title that settles in, cause and score. */
public final class DeathScreenModule extends Module {
	private static DeathScreenModule instance;

	private final BoolSetting blur = this.add(new BoolSetting("blur", "Blur World", true).group("Death Screen"));
	private final BoolSetting score = this.add(new BoolSetting("score", "Show Score", true).group("Death Screen"));

	public DeathScreenModule() {
		super("death_screen", "Death Screen", Category.SCREEN, Icons.SKULL, "A cinematic death screen");
		instance = this;
		this.enabledByDefault();
	}

	private static boolean on() {
		return instance != null && instance.isEnabled();
	}

	public static boolean replacesText() {
		return on();
	}

	/** Buttons go below the centered title block. */
	public static void arrange(DeathScreen screen) {
		if (!on()) {
			return;
		}
		int y = Math.round(screen.height * 0.58f);
		for (GuiEventListener l : screen.children()) {
			if (l instanceof AbstractWidget w) {
				w.setX(screen.width / 2 - w.getWidth() / 2);
				w.setY(y);
				y += w.getHeight() + 6;
			}
		}
	}

	public static boolean drawBackground(GuiGraphics g, DeathScreen screen, float ticks) {
		if (!on()) {
			return false;
		}
		float a = Easing.OUT_CUBIC.apply(Math.min(ticks / 18f, 1f));
		float w = screen.width;
		float h = screen.height;
		if (instance.blur.on()) {
			Draw.glass(g, 0f, 0f, w, h, Colors.withAlpha(0x2A0505, 0.55f)).alpha(a).submit();
		}
		Draw.rect(g, 0f, 0f, w, h).gradientV(Colors.withAlpha(0x3A0000, 0.35f * a), Colors.withAlpha(0x0A0000, 0.8f * a)).submit();
		return true;
	}

	public static void drawText(GuiGraphics g, DeathScreen screen, @Nullable Component cause, Component deathScore, boolean hardcore, float ticks) {
		if (!on()) {
			return;
		}
		float t = Math.min(ticks / 22f, 1f);
		float a = Easing.OUT_CUBIC.apply(t);
		float cx = screen.width * 0.5f;
		float cy = screen.height * 0.36f;
		String title = screen.getTitle().getString().toUpperCase(Locale.ROOT);
		float size = Math.min(44f, screen.width / 9f);
		float tracking = 0.12f;
		float k = 1.12f - 0.12f * Easing.OUT_BACK_SOFT.apply(t);
		g.pose().pushMatrix();
		g.pose().translate(cx, cy);
		g.pose().scale(k, k);
		float tw = Fonts.DISPLAY.width(title, size, tracking);
		float base = Fonts.DISPLAY.capHeight(size) * 0.5f;
		Text.shadow(g, Fonts.DISPLAY, title, -tw * 0.5f, base, size, Colors.withAlpha(0xFF2A2A, 0.55f * a), 0f, 0f, size * 0.3f, tracking);
		Text.drawBaseline(g, Fonts.DISPLAY, title, -tw * 0.5f, base, size, Colors.mulAlpha(0xFFF4F0F0, a), tracking, 0f);
		g.pose().popMatrix();
		float y = cy + size * 0.6f + 12f;
		if (cause != null) {
			Text.drawMiddle(g, Fonts.MEDIUM, cause.getString(), cx, y, 10f, Colors.mulAlpha(Theme.TEXT_SECONDARY, a), Text.Align.CENTER);
			y += 16f;
		}
		if (instance.score.on() && !hardcore) {
			Text.drawMiddle(g, Fonts.SEMIBOLD, deathScore.getString(), cx, y, 9f, Colors.mulAlpha(0xFFFFC94D, a), Text.Align.CENTER);
		}
	}
}
