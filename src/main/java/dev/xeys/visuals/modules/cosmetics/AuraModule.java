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

/** Glowing orbs circle your body on a gently rising and falling path, each trailing light. */
public final class AuraModule extends Module implements WorldEvents.EndMain {
	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFFFFFFF, ColorSetting.Mode.ACCENT).noAlpha().group("Orbs"));
	private final NumberSetting count = this.add(new NumberSetting("count", "Orbs", 5f, 1f, 12f, 1f).group("Orbs"));
	private final NumberSetting radius = this.add(new NumberSetting("radius", "Radius", 0.85f, 0.5f, 2f, 0.05f).unit(" blocks").decimals(2).group("Orbs"));
	private final NumberSetting speed = this.add(new NumberSetting("speed", "Speed", 90f, 10f, 360f, 5f).unit("°/s").group("Orbs"));
	private final NumberSetting trail = this.add(new NumberSetting("trail", "Trail", 6f, 0f, 12f, 1f).group("Orbs"));
	private final BoolSetting firstPerson = this.add(new BoolSetting("first_person", "Show in First Person", false).group("Orbs"));

	public AuraModule() {
		super("aura", "Aura", Category.COSMETICS, Icons.ORBIT, "Glowing orbs circle around you");
	}

	@Override
	public void onEndMain(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || player.isInvisible() || !WorldGlow.dotsUsable() || (!this.firstPerson.on() && Cosmetics.firstPerson())) {
			return;
		}
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Vec3 cam = context.worldState().cameraRenderState.pos;
		float cx = (float) (Mth.lerp(partial, player.xOld, player.getX()) - cam.x);
		float cy = (float) (Mth.lerp(partial, player.yOld, player.getY()) - cam.y) + player.getBbHeight() * 0.55f;
		float cz = (float) (Mth.lerp(partial, player.zOld, player.getZ()) - cam.z);
		WorldGlow.face(mc.gameRenderer.getMainCamera());
		VertexConsumer vc = context.consumers().getBuffer(WorldGlow.DOTS);
		PoseStack.Pose pose = context.matrices().last();
		int n = this.count.intValue();
		float r = this.radius.value();
		double t = Clock.time();
		double spin = Math.toRadians(this.speed.value());
		int trails = this.trail.intValue();
		for (int i = 0; i < n; i++) {
			float phase = (float) (Math.PI * 2.0 * i / n);
			int col = this.color.color((float) i / n) & 0xFFFFFF;
			for (int k = trails; k >= 0; k--) {
				double a = t * spin + phase - k * 0.11;
				float h = (float) Math.sin(t * 0.9 - k * 0.11 * 0.9 + phase * 1.7) * 0.38f;
				float x = cx + (float) Math.cos(a) * r;
				float y = cy + h;
				float z = cz + (float) Math.sin(a) * r;
				float fade = 1f - (float) k / (trails + 1);
				if (k == 0) {
					WorldGlow.dot(vc, pose, x, y, z, 0.3f, Colors.withAlpha(col, 0.42f));
					WorldGlow.dot(vc, pose, x, y, z, 0.075f, Colors.withAlpha(col, 1f));
				} else {
					WorldGlow.dot(vc, pose, x, y, z, 0.07f * (0.4f + 0.6f * fade), Colors.withAlpha(col, 0.55f * fade * fade));
				}
			}
		}
	}
}
