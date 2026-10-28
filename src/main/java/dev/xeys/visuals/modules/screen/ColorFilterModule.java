package dev.xeys.visuals.modules.screen;

import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.post.ScreenFx;
import dev.xeys.visuals.ui.Icons;

/** A soft-light color filter that shifts the mood of the image while keeping highlights and shadows. */
public final class ColorFilterModule extends ScreenEffectModule {
	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFFFB06B).noAlpha().group("Filter"));
	private final NumberSetting amount = this.add(new NumberSetting("amount", "Amount", 30f, 0f, 100f, 1f).unit("%").group("Filter"));

	public ColorFilterModule() {
		super("color_filter", "Color Filter", Icons.PALETTE, "Tint the world with a soft color filter");
	}

	@Override
	public void contribute(ScreenFx fx, float partialTick) {
		fx.tint = this.color.color();
		fx.tintAmount = this.amount.value() / 100f;
	}
}
