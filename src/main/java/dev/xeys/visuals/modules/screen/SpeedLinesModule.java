package dev.xeys.visuals.modules.screen;

import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Anime-style speed streaks that rush in from the screen edges when you move fast (elytra, speed). */
public final class SpeedLinesModule extends Module implements Events.Overlay, Events.ClientTick {
	private static final int MAX_LINES = 96;

	private final NumberSetting threshold = this.add(new NumberSetting("threshold", "Start At", 12f, 4f, 40f, 0.5f).unit(" b/s").decimals(1)
		.describe("Speed where the streaks begin").group("Lines"));
	private final NumberSetting intensity = this.add(new NumberSetting("intensity", "Intensity", 55f, 10f, 100f, 1f).unit("%").group("Lines"));
	private final NumberSetting density = this.add(new NumberSetting("density", "Density", 56f, 16f, 96f, 1f).group("Lines"));
	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFFFFFFF).group("Lines"));
	private float speed;
	private float amount;

	public SpeedLinesModule() {
		super("speed_lines", "Speed Lines", Category.SCREEN, Icons.WIND, "Streaks from the edges when moving fast");
	}

	@Override
	public void onClientTick() {
		Player p = Minecraft.getInstance().player;
		if (p == null) {
			this.speed = 0f;
			return;
		}
		Vec3 v = p.getDeltaMovement();
		float bps = (float) v.length() * 20f;
		this.speed += (bps - this.speed) * 0.4f;
	}

	@Override
	public void renderOverlay(GuiGraphics g, float partialTick) {
		float start = this.threshold.value();
		float t = Math.max(0f, Math.min((this.speed - start) / (start * 1.2f), 1f));
		this.amount += (t * t * (3f - 2f * t) - this.amount) * Clock.smoothing(6f);
		if (this.amount < 0.01f) {
			return;
		}
		float w = g.guiWidth();
		float h = g.guiHeight();
		float cx = w * 0.5f;
		float cy = h * 0.5f;
		int n = Math.min(MAX_LINES, this.density.intValue());
		// Re-roll the pattern a dozen times a second, like hand-drawn speed lines.
		long epoch = (long) (Clock.time() * 12.0);
		int base = this.color.color();
		float strength = this.intensity.value() / 100f * this.amount;
		for (int i = 0; i < n; i++) {
			float r0 = rand(i, epoch, 1);
			float r1 = rand(i, epoch, 2);
			float r2 = rand(i, epoch, 3);
			if (r2 > 0.35f + 0.65f * this.amount) {
				continue;
			}
			double angle = (i + r0 * 0.9) / n * Math.PI * 2.0;
			float cos = (float) Math.cos(angle);
			float sin = (float) Math.sin(angle);
			// Start right at the screen edge along this direction (ray to rectangle), slightly beyond it.
			float edge = Math.min(cx / Math.max(Math.abs(cos), 1e-4f), cy / Math.max(Math.abs(sin), 1e-4f));
			float outer = edge + 2f;
			float inner = edge * (0.5f + 0.3f * r1 - 0.12f * this.amount);
			float x0 = cx + cos * outer;
			float y0 = cy + sin * outer;
			float x1 = cx + cos * inner;
			float y1 = cy + sin * inner;
			float thick = 0.8f + 1.6f * r2;
			Draw.line(g, x0, y0, x1, y1, thick).gradientH(Colors.mulAlpha(base, strength * (0.55f + 0.45f * r1)), Colors.withAlpha(base, 0f))
				.submit();
		}
	}

	private static float rand(int i, long epoch, int salt) {
		long x = (i * 0x9E3779B97F4A7C15L) ^ (epoch * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
		x ^= x >>> 33;
		x *= 0xFF51AFD7ED558CCDL;
		x ^= x >>> 33;
		return (x >>> 40) / (float) (1L << 24);
	}
}
