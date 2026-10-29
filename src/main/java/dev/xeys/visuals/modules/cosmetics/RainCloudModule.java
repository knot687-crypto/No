package dev.xeys.visuals.modules.cosmetics;

import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.cosmetic.CosmeticContext;
import dev.xeys.visuals.cosmetic.CosmeticMesh;
import dev.xeys.visuals.cosmetic.Cosmetics;
import dev.xeys.visuals.ui.Icons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

/** Your own little cloud follows above your head, raining, snowing or storming on you alone. */
public final class RainCloudModule extends Module implements WorldEvents.PlayerLayer, Events.ClientTick {
	public enum Weather {
		RAIN(0xFFE9ECF2, 0xFFC4CAD6),
		SNOW(0xFFF8FAFF, 0xFFDDE4F0),
		STORM(0xFF5A5F6B, 0xFF3A3E47);

		final int top;
		final int bottom;

		Weather(int top, int bottom) {
			this.top = top;
			this.bottom = bottom;
		}
	}

	/** Puffs: x, y, z, half-size, in pixels around the cloud's center. */
	private static final float[][] PUFFS = {
		{0f, 0f, 0f, 3.2f}, {-3.6f, 0.8f, 0.4f, 2.4f}, {3.4f, 0.6f, -0.3f, 2.6f}, {1.2f, -1.4f, 1.6f, 2.2f},
		{-1.6f, -1.0f, -1.8f, 2.1f}, {5.6f, 1.4f, 0.6f, 1.6f}, {-5.8f, 1.6f, -0.4f, 1.5f},
	};

	private final EnumSetting<Weather> weather = this.add(new EnumSetting<>("weather", "Weather", Weather.RAIN).group("Cloud"));
	private final RandomSource random = RandomSource.create();

	public RainCloudModule() {
		super("rain_cloud", "Rain Cloud", Category.COSMETICS, Icons.CLOUD_RAIN, "A little cloud that rains on you alone");
	}

	@Override
	public void submitOnPlayer(CosmeticContext c) {
		if (!c.self) {
			return;
		}
		Weather w = this.weather.get();
		// In the model's root space (the neck), so it floats level whatever the head does.
		c.pose.translate(0f, -1.12f, 0f);
		c.pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(c.time * 6f));
		float bob = (float) Math.sin(c.time * 1.3) * 0.02f;
		int light = c.light;
		c.collector.submitCustomGeometry(c.pose, Cosmetics.SOLID, (pose, vc) -> {
			for (float[] p : PUFFS) {
				float s = p[3] / 16f;
				float x = p[0] / 16f;
				float y = p[1] / 16f + bob;
				float z = p[2] / 16f;
				CosmeticMesh.box(vc, pose, x - s, y - s * 0.8f, z - s, x + s, y + s * 0.8f, z + s, w.top, w.bottom, light);
			}
		});
	}

	@Override
	public void onClientTick() {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		LocalPlayer player = mc.player;
		if (level == null || player == null || mc.isPaused()) {
			return;
		}
		Weather w = this.weather.get();
		ParticleOptions drop = w == Weather.SNOW ? ParticleTypes.SNOWFLAKE : ParticleTypes.FALLING_WATER;
		int n = w == Weather.STORM ? 4 : 2;
		double top = player.getY() + player.getBbHeight() + 0.45;
		for (int i = 0; i < n; i++) {
			double x = player.getX() + (this.random.nextDouble() - 0.5) * 0.7;
			double z = player.getZ() + (this.random.nextDouble() - 0.5) * 0.7;
			level.addParticle(drop, x, top, z, 0.0, w == Weather.SNOW ? -0.03 : 0.0, 0.0);
		}
		if (w == Weather.STORM && this.random.nextFloat() < 0.02f) {
			level.addParticle(ParticleTypes.ELECTRIC_SPARK, player.getX(), top + 0.15, player.getZ(), 0.0, 0.0, 0.0);
		}
	}
}
