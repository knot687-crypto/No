package dev.xeys.visuals.modules.screen;

import dev.xeys.visuals.anim.Anim;
import dev.xeys.visuals.anim.Easing;
import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.post.ScreenFx;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/**
 * Replaces the flat dark gradient behind inventories and containers: frosted blur of the world, a soft
 * dim or a tinted gradient, with a vignette and a fade-in when the screen opens.
 */
public final class InventoryBackgroundModule extends Module implements Events.ScreenEffect {
	public enum Style {
		BLUR,
		DIM,
		GRADIENT,
		CLEAR
	}

	private static InventoryBackgroundModule instance;

	private final EnumSetting<Style> style = this.add(new EnumSetting<>("style", "Style", Style.BLUR).group("Background"));
	private final NumberSetting dim = this.add(new NumberSetting("dim", "Darken", 35f, 0f, 90f, 1f).unit("%").group("Background"));
	private final ColorSetting tint = this.add(new ColorSetting("tint", "Tint", 0xFF000000).noAlpha()
		.describe("Color mixed into the background").group("Background"));
	private final BoolSetting vignette = this.add(new BoolSetting("vignette", "Vignette", true).group("Background"));
	private final BoolSetting fade = this.add(new BoolSetting("fade", "Fade In", true).describe("The background eases in when a screen opens")
		.group("Background"));

	private final Anim appear = new Anim(1f);
	private Screen lastScreen;

	public InventoryBackgroundModule() {
		super("inventory_background", "Inventory Background", Category.SCREEN, Icons.BACKPACK, "Frosted, dimmed or tinted backgrounds behind inventories");
		instance = this;
	}

	/** Draws the background for an in-game screen; returns false to keep vanilla's. */
	public static boolean render(GuiGraphics g, Screen screen) {
		InventoryBackgroundModule m = instance;
		if (m == null || !m.isEnabled() || Minecraft.getInstance().level == null) {
			return false;
		}
		if (screen != m.lastScreen) {
			m.lastScreen = screen;
			if (m.fade.on()) {
				m.appear.restart(0f, 1f, 0.28f, Easing.OUT_CUBIC);
			} else {
				m.appear.snap(1f);
			}
		}
		float a = m.appear.get();
		float w = screen.width;
		float h = screen.height;
		float d = m.dim.value() / 100f;
		int tintColor = m.tint.color();
		switch (m.style.get()) {
			case BLUR -> Draw.glass(g, 0f, 0f, w, h, Colors.withAlpha(tintColor, d * 0.9f)).alpha(a).submit();
			case DIM -> Draw.rect(g, 0f, 0f, w, h, 0f, Colors.withAlpha(tintColor, d * a));
			case GRADIENT -> Draw.rect(g, 0f, 0f, w, h).gradientV(Colors.withAlpha(tintColor, d * 0.45f * a), Colors.withAlpha(tintColor, Math.min(1f, d * 1.5f) * a))
				.submit();
			case CLEAR -> {
			}
		}
		return true;
	}

	/** The vignette lives in the world image, under the inventory itself. */
	@Override
	public void contribute(ScreenFx fx, float partialTick) {
		Screen s = Minecraft.getInstance().screen;
		if (!this.vignette.on() || s == null || !s.isInGameUi() || s != this.lastScreen) {
			return;
		}
		float v = 0.55f * this.appear.get();
		if (v > fx.vignette) {
			fx.vignette = v;
			fx.vignetteRadius = 0.62f;
			fx.vignetteSoftness = 0.5f;
			fx.vignetteRoundness = 1f;
			fx.vignetteColor = 0xFF000000;
		}
	}
}
