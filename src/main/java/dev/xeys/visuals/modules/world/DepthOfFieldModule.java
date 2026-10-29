package dev.xeys.visuals.modules.world;

import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.post.WorldFx;
import dev.xeys.visuals.ui.Icons;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

/**
 * Lens-like depth of field: what you look at stays sharp and the rest softens with distance from it.
 * Autofocus follows the crosshair and racks smoothly between distances.
 */
public final class DepthOfFieldModule extends Module implements WorldEvents.WorldPostEffect {
	public enum Focus {
		CROSSHAIR,
		FIXED
	}

	private static final double MAX_RANGE = 256.0;

	private final EnumSetting<Focus> focus = this.add(new EnumSetting<>("focus", "Focus", Focus.CROSSHAIR).group("Focus"));
	private final NumberSetting distance = this.add(new NumberSetting("distance", "Focus Distance", 12f, 1f, 128f, 1f).unit(" blocks")
		.visibleWhen(() -> this.focus.is(Focus.FIXED)).group("Focus"));
	private final NumberSetting speed = this.add(new NumberSetting("speed", "Focus Speed", 100f, 20f, 300f, 5f).unit("%")
		.describe("How quickly autofocus adjusts").visibleWhen(() -> this.focus.is(Focus.CROSSHAIR)).group("Focus"));
	private final NumberSetting strength = this.add(new NumberSetting("strength", "Strength", 80f, 10f, 100f, 1f).unit("%").group("Blur"));
	private final NumberSetting aperture = this.add(new NumberSetting("aperture", "Aperture", 6f, 1f, 20f, 0.5f).decimals(1)
		.describe("Wider apertures blur more, closer to the focus").group("Blur"));
	/** Focus distance, eased in log space so racking near and far feels the same. */
	private double logFocus = Math.log(8.0);
	private long lastFrame = -1L;

	public DepthOfFieldModule() {
		super("depth_of_field", "Depth of Field", Category.WORLD, Icons.APERTURE, "Focus on what you look at, soften the rest");
	}

	@Override
	public void contributeWorld(WorldFx fx, float partialTick) {
		double target = this.focus.is(Focus.FIXED) ? this.distance.value() : this.measure(partialTick);
		double logTarget = Math.log(Math.max(0.3, target));
		if (this.lastFrame < 0L) {
			this.logFocus = logTarget;
		} else {
			this.logFocus += (logTarget - this.logFocus) * Clock.smoothing(4.5f * this.speed.value() / 100f);
		}
		this.lastFrame = Clock.frame();
		fx.dof = this.strength.value() / 100f;
		fx.focus = (float) Math.exp(this.logFocus);
		fx.aperture = this.aperture.value();
	}

	/** Distance to whatever is under the crosshair: an entity in reach, else the first block along the view. */
	private double measure(float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		Camera camera = mc.gameRenderer.getMainCamera();
		Entity viewer = mc.getCameraEntity();
		if (mc.level == null || viewer == null) {
			return MAX_RANGE;
		}
		Vec3 eye = camera.position();
		if (mc.hitResult instanceof EntityHitResult hit && hit.getType() == HitResult.Type.ENTITY) {
			return Math.max(0.3, hit.getLocation().distanceTo(eye));
		}
		Vector3fc f = camera.forwardVector();
		Vec3 end = eye.add(f.x() * MAX_RANGE, f.y() * MAX_RANGE, f.z() * MAX_RANGE);
		HitResult hit = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, viewer));
		if (hit.getType() == HitResult.Type.MISS) {
			return MAX_RANGE;
		}
		return Math.max(0.3, hit.getLocation().distanceTo(eye));
	}

	@Override
	protected void onDisable() {
		this.lastFrame = -1L;
	}
}
