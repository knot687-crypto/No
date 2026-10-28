package dev.xeys.visuals.modules.screen;

import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.post.ScreenFx;
import dev.xeys.visuals.ui.Icons;
import net.minecraft.client.Minecraft;

/** Soft horizontal scanlines for a retro display look. */
public final class ScanlinesModule extends ScreenEffectModule {
	private final NumberSetting strength = this.add(new NumberSetting("strength", "Strength", 22f, 5f, 100f, 1f).unit("%").group("Scanlines"));
	private final NumberSetting spacing = this.add(new NumberSetting("spacing", "Spacing", 3f, 2f, 8f, 0.5f).unit("px").decimals(1)
		.describe("At 1080p").group("Scanlines"));
	private final NumberSetting scroll = this.add(new NumberSetting("scroll", "Roll Speed", 10f, 0f, 60f, 1f).unit("px/s").group("Scanlines"));

	public ScanlinesModule() {
		super("scanlines", "Scanlines", Icons.SCAN_LINE, "Retro display scanlines");
	}

	@Override
	public void contribute(ScreenFx fx, float partialTick) {
		float k = Minecraft.getInstance().getWindow().getHeight() / 1080f;
		fx.scanlines = this.strength.value() / 100f;
		fx.scanSpacing = Math.max(2f, this.spacing.value() * k);
		fx.scanSpeed = this.scroll.value() * k;
	}
}
