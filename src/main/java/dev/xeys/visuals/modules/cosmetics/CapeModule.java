package dev.xeys.visuals.modules.cosmetics;

import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.cosmetic.CapeTextures;
import dev.xeys.visuals.cosmetic.CosmeticHooks;
import dev.xeys.visuals.ui.Icons;

/** Wear an XEYS cape (and matching elytra): obsidian, gradient, galaxy, aurora, carbon or sunset. */
public final class CapeModule extends Module implements Events.FrameStart {
	private final EnumSetting<CapeTextures.Design> design = this.add(new EnumSetting<>("design", "Design", CapeTextures.Design.OBSIDIAN)
		.onChange(v -> this.dirty = true).group("Cape"));
	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFFFFFFF, ColorSetting.Mode.ACCENT).noAlpha().group("Cape"));
	private boolean dirty = true;
	private int drawnColor;
	private double drawnAt = -1.0;

	public CapeModule() {
		super("cape", "Cape", Category.COSMETICS, Icons.FLAG, "Wear an XEYS cape, with matching elytra");
	}

	@Override
	public void onFrameStart() {
		CapeTextures.Design d = this.design.get();
		int c = this.color.color() | 0xFF000000;
		double now = Clock.time();
		// Static designs redraw only on change; animated ones a dozen times a second.
		if (this.dirty || c != this.drawnColor || (d.animated() && now - this.drawnAt > 1.0 / 12.0)) {
			CapeTextures.draw(d, c, (float) (now % 3600.0));
			this.drawnColor = c;
			this.drawnAt = now;
			this.dirty = false;
		}
		CosmeticHooks.cape = CapeTextures.ASSET;
	}

	@Override
	protected void onDisable() {
		CosmeticHooks.cape = null;
		CapeTextures.release();
		this.dirty = true;
	}
}
