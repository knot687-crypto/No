package dev.xeys.visuals.modules.world;

import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.post.WorldFx;
import dev.xeys.visuals.ui.Icons;

/**
 * Cinematic camera motion blur. Each pixel is traced back to where it was on the previous frame (from
 * the depth buffer), so only real camera motion blurs, the same at any frame rate. The hand stays sharp.
 */
public final class MotionBlurModule extends Module implements WorldEvents.WorldPostEffect {
	public enum Quality {
		LOW(6),
		MEDIUM(10),
		HIGH(16);

		final int samples;

		Quality(int samples) {
			this.samples = samples;
		}
	}

	private final NumberSetting strength = this.add(new NumberSetting("strength", "Strength", 50f, 10f, 100f, 1f).unit("%")
		.describe("Shutter length: how much of each moment is smeared").group("Motion Blur"));
	private final NumberSetting limit = this.add(new NumberSetting("limit", "Max Length", 6f, 1f, 12f, 0.5f).unit("%").decimals(1)
		.describe("Longest streak, as a share of the screen width").group("Motion Blur"));
	private final EnumSetting<Quality> quality = this.add(new EnumSetting<>("quality", "Quality", Quality.MEDIUM).group("Motion Blur"));

	public MotionBlurModule() {
		super("motion_blur", "Motion Blur", Category.WORLD, Icons.WIND, "Smooth, cinematic blur when the camera moves");
	}

	@Override
	public void contributeWorld(WorldFx fx, float partialTick) {
		fx.motion = this.strength.value() / 100f;
		fx.motionMax = this.limit.value() / 100f;
		fx.motionSamples = this.quality.get().samples;
	}
}
