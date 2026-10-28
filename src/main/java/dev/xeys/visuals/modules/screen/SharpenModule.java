package dev.xeys.visuals.modules.screen;

import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.post.ScreenFx;
import dev.xeys.visuals.ui.Icons;

/** Crisper edges and textures with a light unsharp mask. */
public final class SharpenModule extends ScreenEffectModule {
	private final NumberSetting strength = this.add(new NumberSetting("strength", "Strength", 35f, 5f, 100f, 1f).unit("%").group("Sharpen"));

	public SharpenModule() {
		super("sharpen", "Sharpen", Icons.FOCUS, "Crisper textures and edges");
	}

	@Override
	public void contribute(ScreenFx fx, float partialTick) {
		fx.sharpen = this.strength.value() / 100f * 1.2f;
	}
}
