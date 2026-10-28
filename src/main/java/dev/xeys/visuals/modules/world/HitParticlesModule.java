package dev.xeys.visuals.modules.world;

import dev.xeys.visuals.core.combat.HitTracker;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/** A burst of particles each time one of your hits lands, bigger on critical hits and kills. */
public final class HitParticlesModule extends Module {
	public enum Style {
		SPARKS,
		MAGIC,
		HEARTS,
		FLAMES,
		SOUL_FLAMES,
		SNOW,
		NOTES
	}

	private final EnumSetting<Style> style = this.add(new EnumSetting<>("style", "Style", Style.SPARKS).group("Particles"));
	private final NumberSetting amount = this.add(new NumberSetting("amount", "Amount", 8f, 1f, 30f, 1f).group("Particles"));
	private final BoolSetting critBoost = this.add(new BoolSetting("crit_boost", "Bigger on Crits", true).group("Particles"));
	private final BoolSetting killBurst = this.add(new BoolSetting("kill_burst", "Kill Burst", true).describe("A large burst on the final hit")
		.group("Particles"));
	private final RandomSource random = RandomSource.create();

	public HitParticlesModule() {
		super("hit_particles", "Hit Particles", Category.WORLD, Icons.SPARKLE, "Particle bursts when your hits land");
		HitTracker.listen(this::onHit);
	}

	private ParticleOptions particle() {
		return switch (this.style.get()) {
			case SPARKS -> ParticleTypes.CRIT;
			case MAGIC -> ParticleTypes.ENCHANTED_HIT;
			case HEARTS -> ParticleTypes.HEART;
			case FLAMES -> ParticleTypes.FLAME;
			case SOUL_FLAMES -> ParticleTypes.SOUL_FIRE_FLAME;
			case SNOW -> ParticleTypes.SNOWFLAKE;
			case NOTES -> ParticleTypes.NOTE;
		};
	}

	private void onHit(LivingEntity target, HitTracker.Kind kind) {
		ClientLevel level = Minecraft.getInstance().level;
		if (!this.isEnabled() || level == null) {
			return;
		}
		int n = this.amount.intValue();
		if (kind == HitTracker.Kind.CRITICAL && this.critBoost.on()) {
			n = n * 2;
		} else if (kind == HitTracker.Kind.KILL && this.killBurst.on()) {
			n = n * 3;
		}
		AABB box = target.getBoundingBox();
		double cx = (box.minX + box.maxX) * 0.5;
		double cy = box.minY + box.getYsize() * 0.6;
		double cz = (box.minZ + box.maxZ) * 0.5;
		double spread = Math.max(box.getXsize(), 0.4) * 0.5;
		ParticleOptions p = this.particle();
		for (int i = 0; i < n; i++) {
			double x = cx + (this.random.nextDouble() - 0.5) * spread * 2.0;
			double y = cy + (this.random.nextDouble() - 0.5) * box.getYsize() * 0.6;
			double z = cz + (this.random.nextDouble() - 0.5) * spread * 2.0;
			double vx = (this.random.nextDouble() - 0.5) * 0.35;
			double vy = this.random.nextDouble() * 0.25 + 0.05;
			double vz = (this.random.nextDouble() - 0.5) * 0.35;
			level.addParticle(p, x, y, z, vx, vy, vz);
		}
	}
}
