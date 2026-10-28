package dev.xeys.visuals.modules.screen;

import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.anim.Easing;
import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

/** Glowing arcs around the crosshair pointing toward whatever just hurt you; they track it as you turn. */
public final class DamageDirectionModule extends Module implements Events.Overlay {
	private static final int SLOTS = 8;
	private static DamageDirectionModule instance;

	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFFF4D4D).group("Indicator"));
	private final NumberSetting radius = this.add(new NumberSetting("radius", "Distance", 46f, 24f, 120f, 1f).unit("px")
		.describe("From the crosshair").group("Indicator"));
	private final NumberSetting length = this.add(new NumberSetting("length", "Arc Length", 50f, 15f, 100f, 1f).unit("°").group("Indicator"));
	private final NumberSetting thickness = this.add(new NumberSetting("thickness", "Thickness", 3f, 1f, 8f, 0.5f).unit("px").decimals(1)
		.group("Indicator"));
	private final NumberSetting duration = this.add(new NumberSetting("duration", "Duration", 1.6f, 0.4f, 4f, 0.1f).unit("s").decimals(1)
		.group("Indicator"));
	private final BoolSetting track = this.add(new BoolSetting("track", "Track Source", true)
		.describe("Arcs keep pointing at the source as you turn").group("Indicator"));

	/** World-space direction of each hit (degrees, same frame as entity yaw) and its time. */
	private final float[] directions = new float[SLOTS];
	private final float[] yawAtHit = new float[SLOTS];
	private final double[] times = new double[SLOTS];
	private int next;

	public DamageDirectionModule() {
		super("damage_direction", "Damage Direction", Category.SCREEN, Icons.RADAR, "Arcs around the crosshair pointing at what hurt you");
		instance = this;
		for (int i = 0; i < SLOTS; i++) {
			this.times[i] = -100.0;
		}
	}

	public static void onLocalHurt(float hurtDir) {
		DamageDirectionModule m = instance;
		Player p = Minecraft.getInstance().player;
		if (m == null || !m.isEnabled() || p == null) {
			return;
		}
		m.directions[m.next] = hurtDir + p.getYRot();
		m.yawAtHit[m.next] = p.getYRot();
		m.times[m.next] = Clock.time();
		m.next = (m.next + 1) % SLOTS;
	}

	@Override
	public void renderOverlay(GuiGraphics g, float partialTick) {
		Player p = Minecraft.getInstance().player;
		if (p == null) {
			return;
		}
		float cx = g.guiWidth() * 0.5f;
		float cy = g.guiHeight() * 0.5f;
		float yaw = p.getViewYRot(partialTick);
		float dur = this.duration.value();
		float half = this.length.value() * 0.5f;
		int base = this.color.color();
		for (int i = 0; i < SLOTS; i++) {
			float age = (float) (Clock.time() - this.times[i]);
			if (age < 0f || age > dur) {
				continue;
			}
			float reference = this.track.on() ? yaw : this.yawAtHit[i];
			// Hurt direction is measured from +X toward +Z; the screen angle is clockwise from straight ahead.
			float angle = this.directions[i] - reference - 90f;
			float in = Math.min(age / 0.08f, 1f);
			float out = 1f - Easing.IN_CUBIC.apply(Math.max(0f, (age - dur * 0.35f) / (dur * 0.65f)));
			float a = in * out;
			float r = this.radius.value() * (0.86f + 0.14f * Easing.OUT_BACK_SOFT.apply(Math.min(age / 0.25f, 1f)));
			float th = this.thickness.value();
			int solid = Colors.mulAlpha(base, a);
			int clear = Colors.withAlpha(base, 0f);
			// Soft glow, then the crisp arc; each half fades toward its tip.
			Draw.arc(g, cx, cy, r, th * 2.2f, angle - half, half).color(clear).secondary(Colors.mulAlpha(base, 0.3f * a))
				.paramsB(1f, th * 2.4f, 0f, 0f).margin(th * 2.6f).submit();
			Draw.arc(g, cx, cy, r, th * 2.2f, angle, half).color(Colors.mulAlpha(base, 0.3f * a)).secondary(clear)
				.paramsB(1f, th * 2.4f, 0f, 0f).margin(th * 2.6f).submit();
			Draw.arcGradient(g, cx, cy, r, th, angle - half, half, clear, solid);
			Draw.arcGradient(g, cx, cy, r, th, angle, half, solid, clear);
		}
	}
}
