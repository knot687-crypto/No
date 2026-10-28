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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * An X that pops around the crosshair when one of your hits lands: white for a hit, gold for a critical
 * hit and red when it was the finishing blow. Shown only once the game confirms the target was hurt.
 */
public final class HitMarkerModule extends Module implements Events.Overlay, Events.FrameStart {
	private static HitMarkerModule instance;

	private final ColorSetting hitColor = this.add(new ColorSetting("hit_color", "Hit", 0xFFFFFFFF).group("Colors"));
	private final ColorSetting critColor = this.add(new ColorSetting("crit_color", "Critical", 0xFFFFC94D).group("Colors"));
	private final ColorSetting killColor = this.add(new ColorSetting("kill_color", "Kill", 0xFFFF4D4D).group("Colors"));
	private final BoolSetting kills = this.add(new BoolSetting("kills", "Kill Markers", true).describe("A larger red marker on the final hit")
		.group("Marker"));
	private final NumberSetting size = this.add(new NumberSetting("size", "Size", 6f, 3f, 14f, 0.5f).unit("px").decimals(1).group("Marker"));
	private final NumberSetting gap = this.add(new NumberSetting("gap", "Gap", 4f, 1f, 12f, 0.5f).unit("px").decimals(1).group("Marker"));
	private final NumberSetting thickness = this.add(new NumberSetting("thickness", "Thickness", 1.5f, 0.75f, 4f, 0.25f).unit("px").decimals(2)
		.group("Marker"));
	private final NumberSetting duration = this.add(new NumberSetting("duration", "Duration", 0.35f, 0.15f, 1f, 0.05f).unit("s").decimals(2)
		.group("Marker"));

	private LivingEntity pending;
	private double pendingAt;
	private int pendingHurt;
	private boolean pendingCrit;
	private LivingEntity shown;
	private double shownAt = -10.0;
	private int kind;

	public HitMarkerModule() {
		super("hit_marker", "Hit Marker", Category.SCREEN, Icons.CROSSHAIR, "An X at the crosshair when your hits land");
		instance = this;
	}

	public static void onAttack(Player player, Entity target) {
		HitMarkerModule m = instance;
		if (m == null || !m.isEnabled() || !(target instanceof LivingEntity living)) {
			return;
		}
		m.pending = living;
		m.pendingAt = Clock.time();
		m.pendingHurt = living.hurtTime;
		m.pendingCrit = player.fallDistance > 0.0 && !player.onGround() && !player.onClimbable() && !player.isInWater()
			&& !player.hasEffect(MobEffects.BLINDNESS) && !player.isPassenger() && !player.isSprinting();
	}

	@Override
	public void onFrameStart() {
		LivingEntity p = this.pending;
		if (p != null) {
			if (Clock.time() - this.pendingAt > 0.6) {
				this.pending = null;
			} else if (p.hurtTime > this.pendingHurt || p.isDeadOrDying()) {
				this.shown = p;
				this.shownAt = Clock.time();
				this.kind = p.isDeadOrDying() && this.kills.on() ? 2 : (this.pendingCrit ? 1 : 0);
				this.pending = null;
			}
		}
		// A hit that turns out to be the kill a moment later upgrades to a kill marker.
		if (this.shown != null && this.kind != 2 && this.kills.on() && this.shown.isDeadOrDying() && Clock.time() - this.shownAt < 0.3) {
			this.kind = 2;
			this.shownAt = Clock.time();
		}
	}

	@Override
	public void renderOverlay(GuiGraphics g, float partialTick) {
		float dur = this.duration.value() * (this.kind == 2 ? 1.8f : 1f);
		float age = (float) (Clock.time() - this.shownAt);
		if (age < 0f || age > dur) {
			return;
		}
		float t = age / dur;
		float pop = 1f + (this.kind == 2 ? 0.55f : 0.35f) * (1f - Easing.OUT_CUBIC.apply(Math.min(age / 0.12f, 1f)));
		float a = 1f - Easing.IN_QUAD.apply(t);
		int color = switch (this.kind) {
			case 1 -> this.critColor.color();
			case 2 -> this.killColor.color();
			default -> this.hitColor.color();
		};
		float scale = this.kind == 2 ? 1.35f : 1f;
		float cx = g.guiWidth() * 0.5f;
		float cy = g.guiHeight() * 0.5f;
		float inner = this.gap.value() * pop * scale;
		float outer = inner + this.size.value() * scale;
		float th = this.thickness.value() * (this.kind == 2 ? 1.25f : 1f);
		int shadow = Colors.withAlpha(0x000000, 0.45f * a);
		for (int q = 0; q < 4; q++) {
			float sx = (q & 1) == 0 ? -0.7071f : 0.7071f;
			float sy = q < 2 ? -0.7071f : 0.7071f;
			Draw.line(g, cx + sx * inner, cy + sy * inner, cx + sx * outer, cy + sy * outer, th + 1.5f).color(shadow).soft(1.2f)
				.submit();
		}
		for (int q = 0; q < 4; q++) {
			float sx = (q & 1) == 0 ? -0.7071f : 0.7071f;
			float sy = q < 2 ? -0.7071f : 0.7071f;
			Draw.line(g, cx + sx * inner, cy + sy * inner, cx + sx * outer, cy + sy * outer, th, Colors.mulAlpha(color, a));
		}
	}
}
