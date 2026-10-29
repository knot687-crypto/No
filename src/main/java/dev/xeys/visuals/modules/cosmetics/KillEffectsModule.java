package dev.xeys.visuals.modules.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.combat.HitTracker;
import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.render.world.WorldGlow;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.phys.Vec3;

/** A finishing flourish where your kills fall: lightning, fireworks, rising souls, hearts or a shockwave. */
public final class KillEffectsModule extends Module implements WorldEvents.EndMain {
	public enum Effect {
		LIGHTNING,
		FIREWORK,
		SOULS,
		HEARTS,
		SHOCKWAVE
	}

	private static final int BOLT = 14;
	private static final float BOLT_LIFE = 0.45f;
	private static final float WAVE_LIFE = 0.8f;

	private final EnumSetting<Effect> effect = this.add(new EnumSetting<>("effect", "Effect", Effect.LIGHTNING).group("Kill Effect"));
	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFFFFFFF, ColorSetting.Mode.ACCENT).noAlpha().group("Kill Effect"));
	private final BoolSetting sound = this.add(new BoolSetting("sound", "Sound", true).group("Kill Effect"));
	private final RandomSource random = RandomSource.create();
	private final float[] boltX = new float[BOLT + 1];
	private final float[] boltZ = new float[BOLT + 1];
	private double ex;
	private double ey;
	private double ez;
	private double startedAt = -10.0;
	private Effect running = Effect.LIGHTNING;

	public KillEffectsModule() {
		super("kill_effects", "Kill Effects", Category.COSMETICS, Icons.ZAP, "A flourish where your kills fall");
		HitTracker.listen((target, kind) -> {
			if (this.isEnabled() && kind == HitTracker.Kind.KILL) {
				this.play(target);
			}
		});
	}

	private void play(LivingEntity target) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return;
		}
		this.ex = target.getX();
		this.ey = target.getY();
		this.ez = target.getZ();
		this.startedAt = Clock.time();
		this.running = this.effect.get();
		double cy = this.ey + target.getBbHeight() * 0.5;
		switch (this.running) {
			case LIGHTNING -> {
				// A jagged path from high above down to the body, wandering less near the ground.
				for (int i = 0; i <= BOLT; i++) {
					float f = (float) i / BOLT;
					float wander = 0.9f * f;
					this.boltX[i] = i == 0 ? 0f : (this.random.nextFloat() - 0.5f) * wander;
					this.boltZ[i] = i == 0 ? 0f : (this.random.nextFloat() - 0.5f) * wander;
				}
				if (this.sound.on()) {
					level.playLocalSound(this.ex, this.ey, this.ez, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 0.6f, 1.4f, false);
				}
			}
			case FIREWORK -> {
				int c = this.color.color() & 0xFFFFFF;
				FireworkExplosion burst = new FireworkExplosion(FireworkExplosion.Shape.LARGE_BALL, IntList.of(c, 0xFFFFFF), IntList.of(c), true, true);
				level.createFireworks(this.ex, cy + 0.5, this.ez, 0.0, 0.0, 0.0, List.of(burst));
			}
			case SOULS -> {
				for (int i = 0; i < 14; i++) {
					level.addParticle(ParticleTypes.SOUL, this.ex + (this.random.nextDouble() - 0.5) * 0.8, cy + (this.random.nextDouble() - 0.5),
						this.ez + (this.random.nextDouble() - 0.5) * 0.8, 0.0, 0.05 + this.random.nextDouble() * 0.06, 0.0);
				}
				if (this.sound.on()) {
					level.playLocalSound(this.ex, cy, this.ez, SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1f, 1f, false);
				}
			}
			case HEARTS -> {
				for (int i = 0; i < 10; i++) {
					level.addParticle(ParticleTypes.HEART, this.ex + (this.random.nextDouble() - 0.5) * 1.2, cy + this.random.nextDouble() * 0.8,
						this.ez + (this.random.nextDouble() - 0.5) * 1.2, 0.0, 0.1, 0.0);
				}
			}
			case SHOCKWAVE -> {
				if (this.sound.on()) {
					level.playLocalSound(this.ex, this.ey, this.ez, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.25f, 1.6f, false);
				}
			}
		}
	}

	@Override
	public void onEndMain(WorldRenderContext context) {
		double age = Clock.time() - this.startedAt;
		Vec3 cam = context.worldState().cameraRenderState.pos;
		PoseStack.Pose pose = context.matrices().last();
		int rgb = this.color.color() & 0xFFFFFF;
		float x = (float) (this.ex - cam.x);
		float y = (float) (this.ey - cam.y);
		float z = (float) (this.ez - cam.z);
		if (this.running == Effect.LIGHTNING && age < BOLT_LIFE && WorldGlow.beamsUsable()) {
			float f = 1f - (float) (age / BOLT_LIFE);
			// Flickers as it fades.
			float flicker = (int) (age * 30.0) % 2 == 0 ? 1f : 0.55f;
			VertexConsumer vc = context.consumers().getBuffer(WorldGlow.BEAMS);
			float top = 14f;
			for (int i = 0; i < BOLT; i++) {
				float y0 = y + top * (1f - (float) i / BOLT);
				float y1 = y + top * (1f - (float) (i + 1) / BOLT);
				float x0 = x + this.boltX[BOLT - i];
				float z0 = z + this.boltZ[BOLT - i];
				float x1 = x + this.boltX[BOLT - i - 1];
				float z1 = z + this.boltZ[BOLT - i - 1];
				WorldGlow.segment(vc, pose, x0, y0, z0, x1, y1, z1, 0.55f, Colors.withAlpha(rgb, 0.55f * f * flicker));
				WorldGlow.segment(vc, pose, x0, y0, z0, x1, y1, z1, 0.14f, Colors.withAlpha(0xFFFFFF, f * flicker));
			}
		}
		if (this.running == Effect.SHOCKWAVE && age < WAVE_LIFE && WorldGlow.ringsUsable()) {
			float t = (float) (age / WAVE_LIFE);
			VertexConsumer vc = context.consumers().getBuffer(WorldGlow.RINGS);
			float r = 0.3f + 3.2f * (1f - (1f - t) * (1f - t));
			WorldGlow.flatRing(vc, pose, x, y + 0.04f, z, r, Colors.withAlpha(rgb, (1f - t) * (1f - t)));
			WorldGlow.flatRing(vc, pose, x, y + 0.04f, z, r * 0.65f, Colors.withAlpha(rgb, (1f - t) * 0.5f));
		}
	}
}
