package dev.xeys.visuals.modules.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.combat.HitTracker;
import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.render.world.WorldGlow;
import dev.xeys.visuals.util.Colors;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Marks the creature you are fighting (your last hit, or what is under your crosshair): a ring at its
 * feet, orbiting lights or a marker above its head. It flashes when your hits land. Hidden by walls.
 */
public final class TargetIndicatorModule extends Module implements Events.FrameStart, WorldEvents.EndMain {
	public enum Style {
		RING,
		ORBIT,
		MARKER
	}

	private final EnumSetting<Style> style = this.add(new EnumSetting<>("style", "Style", Style.RING).group("Indicator"));
	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFFFFFFF, ColorSetting.Mode.ACCENT).noAlpha().group("Indicator"));
	private final ColorSetting hitColor = this.add(new ColorSetting("hit_color", "Hit Flash", 0xFFFF4D5E).noAlpha().group("Indicator"));
	private @Nullable LivingEntity target;
	private float shown;
	private float flash;

	public TargetIndicatorModule() {
		super("target_indicator", "Target Indicator", Category.COSMETICS, Icons.TARGET, "Mark the creature you are fighting");
		HitTracker.listen((target, kind) -> {
			if (this.isEnabled()) {
				this.flash = 1f;
			}
		});
	}

	@Override
	public void onFrameStart() {
		Minecraft mc = Minecraft.getInstance();
		LivingEntity next = HitTracker.lastTarget(4.0);
		Entity aimed = mc.crosshairPickEntity;
		if (next == null && aimed instanceof LivingEntity living && living.isAlive()) {
			next = living;
		}
		if (next != null && next.isAlive()) {
			this.target = next;
		}
		float dt = Math.min(Clock.delta(), 0.1f);
		boolean live = this.target != null && this.target.isAlive() && !this.target.isRemoved() && next != null;
		this.shown += ((live ? 1f : 0f) - this.shown) * Clock.smoothing(10f);
		this.flash = Math.max(0f, this.flash - dt * 3.5f);
		if (this.shown < 0.01f && !live) {
			this.target = null;
		}
	}

	@Override
	public void onEndMain(WorldRenderContext context) {
		LivingEntity t = this.target;
		if (t == null || this.shown < 0.01f) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Vec3 cam = context.worldState().cameraRenderState.pos;
		float x = (float) (Mth.lerp(partial, t.xOld, t.getX()) - cam.x);
		float y = (float) (Mth.lerp(partial, t.yOld, t.getY()) - cam.y);
		float z = (float) (Mth.lerp(partial, t.zOld, t.getZ()) - cam.z);
		float w = Math.max(0.4f, t.getBbWidth());
		float h = t.getBbHeight();
		int rgb = Colors.lerp(this.color.color() | 0xFF000000, this.hitColor.color() | 0xFF000000, this.flash) & 0xFFFFFF;
		float a = this.shown;
		double time = Clock.time();
		PoseStack.Pose pose = context.matrices().last();
		switch (this.style.get()) {
			case RING -> {
				if (!WorldGlow.ringsUsable()) {
					return;
				}
				VertexConsumer vc = context.consumers().getBuffer(WorldGlow.RINGS);
				float r = w * 0.85f * (1f + this.flash * 0.15f);
				WorldGlow.flatRing(vc, pose, x, y + 0.03f, z, r, Colors.withAlpha(rgb, a));
				// A slower, wider ring breathing outward.
				float b = (float) ((time * 0.8) % 1.0);
				WorldGlow.flatRing(vc, pose, x, y + 0.03f, z, r * (1f + b * 0.6f), Colors.withAlpha(rgb, a * (1f - b) * 0.5f));
			}
			case ORBIT -> {
				if (!WorldGlow.dotsUsable()) {
					return;
				}
				WorldGlow.face(mc.gameRenderer.getMainCamera());
				VertexConsumer vc = context.consumers().getBuffer(WorldGlow.DOTS);
				for (int i = 0; i < 3; i++) {
					double ang = time * 3.0 + i * Math.PI * 2.0 / 3.0;
					float oy = y + h * (0.35f + 0.25f * (float) Math.sin(time * 1.7 + i * 2.0));
					float ox = x + (float) Math.cos(ang) * w * 0.9f;
					float oz = z + (float) Math.sin(ang) * w * 0.9f;
					WorldGlow.dot(vc, pose, ox, oy, oz, 0.22f, Colors.withAlpha(rgb, a * 0.4f));
					WorldGlow.dot(vc, pose, ox, oy, oz, 0.06f, Colors.withAlpha(rgb, a));
				}
			}
			case MARKER -> {
				if (!WorldGlow.beamsUsable()) {
					return;
				}
				VertexConsumer vc = context.consumers().getBuffer(WorldGlow.BEAMS);
				// A downward chevron bobbing above the head.
				float top = y + h + 0.45f + 0.08f * (float) Math.sin(time * 3.0);
				float s = 0.18f * (1f + this.flash * 0.3f);
				int c = Colors.withAlpha(rgb, a);
				WorldGlow.segment(vc, pose, x - s, top + s, z, x, top, z, 0.05f, c);
				WorldGlow.segment(vc, pose, x, top, z, x + s, top + s, z, 0.05f, c);
			}
		}
	}

	@Override
	protected void onDisable() {
		this.target = null;
		this.shown = 0f;
	}
}
