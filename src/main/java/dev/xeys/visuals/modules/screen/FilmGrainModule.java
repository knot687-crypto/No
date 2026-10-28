package dev.xeys.visuals.modules.screen;

import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.post.ScreenFx;
import dev.xeys.visuals.ui.Icons;
import net.minecraft.client.Minecraft;

/** Fine photographic grain, strongest in the midtones. */
public final class FilmGrainModule extends ScreenEffectModule {
	private final NumberSetting amount = this.add(new NumberSetting("amount", "Amount", 7f, 1f, 30f, 0.5f).unit("%").decimals(1).group("Grain"));
	private final NumberSetting size = this.add(new NumberSetting("size", "Grain Size", 1.5f, 1f, 4f, 0.1f).unit("px").decimals(1)
		.describe("At 1080p; scales with resolution").group("Grain"));
	private final BoolSetting animated = this.add(new BoolSetting("animated", "Animated", true).describe("The grain shimmers like film")
		.group("Grain"));

	public FilmGrainModule() {
		super("film_grain", "Film Grain", Icons.FILM, "Subtle animated film grain");
	}

	@Override
	public void contribute(ScreenFx fx, float partialTick) {
		float k = Minecraft.getInstance().getWindow().getHeight() / 1080f;
		fx.grain = this.amount.value() / 100f;
		fx.grainSize = Math.max(1f, this.size.value() * k);
		fx.grainSpeed = this.animated.on() ? 1f : 0f;
	}
}
