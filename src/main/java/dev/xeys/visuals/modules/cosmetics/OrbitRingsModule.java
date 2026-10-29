package dev.xeys.visuals.modules.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.cosmetic.Cosmetics;
import dev.xeys.visuals.render.world.WorldGlow;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Rings of light turn around you like a gyroscope, each on its own tilted axis. */
public final class OrbitRingsModule extends Module implements WorldEvents.EndMain {
	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFFFFFFF, ColorSetting.Mode.ACCENT).noAlpha().group("Rings"));
	private final NumberSetting count = this.add(new NumberSetting("count", "Rings", 2f, 1f, 4f, 1f).group("Rings"));
	private final NumberSetting radius = this.add(new NumberSetting("radius", "Radius", 0.8f, 0.5f, 1.6f, 0.05f).unit(" blocks").decimals(2).group("Rings"));
	private final NumberSetting speed = this.add(new NumberSetting("speed", "Speed", 100f, 10f, 300f, 5f).unit("%").group("Rings"));
	private final NumberSetting opacity = this.add(new NumberSetting("opacity", "Opacity", 80f, 20f, 100f, 5f).unit("%").group("Rings"));
	private final BoolSetting firstPerson = this.add(new BoolSetting("first_person", "Show in First Person", false).group("Rings"));

	public OrbitRingsModule() {
		super("orbit_rings", "Orbit Rings", Category.COSMETICS, Icons.ROTATE_3D, "Rings of light turning around you");
	}

	@Override
	public void onEndMain(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || player.isInvisible() || !WorldGlow.ringsUsable() || (!this.firstPerson.on() && Cosmetics.firstPerson())) {
			return;
		}
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Vec3 cam = context.worldState().cameraRenderState.pos;
		float cx = (float) (Mth.lerp(partial, player.xOld, player.getX()) - cam.x);
		float cy = (float) (Mth.lerp(partial, player.yOld, player.getY()) - cam.y) + player.getBbHeight() * 0.5f;
		float cz = (float) (Mth.lerp(partial, player.zOld, player.getZ()) - cam.z);
		VertexConsumer vc = context.consumers().getBuffer(WorldGlow.RINGS);
		PoseStack.Pose pose = context.matrices().last();
		int n = this.count.intValue();
		float r = this.radius.value() / 0.8f;
		double t = Clock.time() * this.speed.value() / 100f;
		for (int i = 0; i < n; i++) {
			// Each ring's normal precesses around the vertical at its own tilt and pace.
			double tilt = Math.toRadians(35.0 + i * 25.0);
			double turn = t * (0.9 + i * 0.35) + i * 2.1;
			float nx = (float) (Math.sin(tilt) * Math.cos(turn));
			float ny = (float) Math.cos(tilt);
			float nz = (float) (Math.sin(tilt) * Math.sin(turn));
			// Two in-plane axes: across the horizontal, then their cross product.
			float ax = nz;
			float az = -nx;
			float al = (float) Math.sqrt(ax * ax + az * az);
			if (al < 1e-4f) {
				ax = 1f;
				az = 0f;
				al = 1f;
			}
			ax /= al;
			az /= al;
			float bx = ny * az;
			float by = nz * ax - nx * az;
			float bz = -ny * ax;
			int col = Colors.withAlpha(this.color.color((float) i / n), this.opacity.value() / 100f);
			WorldGlow.sprite(vc, pose, cx, cy, cz, ax * r, 0f, az * r, bx * r, by * r, bz * r, col);
		}
	}
}
