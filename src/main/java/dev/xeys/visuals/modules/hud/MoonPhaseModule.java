package dev.xeys.visuals.modules.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.hud.HudModule;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.HudStyle;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.render.texture.CpuTexture;
import dev.xeys.visuals.render.texture.MipTexture;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.ui.menu.MoonSky;
import dev.xeys.visuals.util.Colors;
import java.nio.ByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.MoonPhase;
import org.lwjgl.system.MemoryUtil;

/**
 * Tonight's moon phase, drawn as a tiny render of the same moon as the main menu: the real surface,
 * lit from the phase angle, with a faint earthshine on the dark side.
 */
public final class MoonPhaseModule extends HudModule {
	private static final String[] NAMES = {"Full Moon", "Waning Gibbous", "Third Quarter", "Waning Crescent", "New Moon", "Waxing Crescent",
		"First Quarter", "Waxing Gibbous"};
	private static final float ICON = 22f;

	private final BoolSetting countdown = this.add(new BoolSetting("countdown", "Full Moon Countdown", true)
		.describe("Nights until the next full moon").group("Display"));
	private final BoolSetting halo = this.add(new BoolSetting("halo", "Halo", true).describe("A soft glow around the moon").group("Display"));

	private final CpuTexture texture = new CpuTexture("XEYS moon phase");
	private ByteBuffer pixels;
	private int renderedPhase = -1;
	private int renderedSize = -1;
	private boolean renderedWithSurface;
	private int phase;
	private final StringBuilder detail = new StringBuilder(24);

	public MoonPhaseModule() {
		super("moon_phase", "Moon Phase", Icons.MOON, "Tonight's moon phase, rendered from the real lunar surface", HudPosition.Anchor.TOP_RIGHT,
			-6f, 210f);
	}

	@Override
	protected void layout(boolean preview) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && mc.player != null) {
			MoonPhase p = mc.level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, mc.player.position(), null);
			this.phase = p.index();
		} else {
			// Menu preview: step through the cycle.
			this.phase = (int) ((Clock.time() / 1.6) % 8.0);
		}
		this.detail.setLength(0);
		if (this.countdown.on()) {
			int nights = (8 - this.phase) % 8;
			if (nights == 0) {
				this.detail.append("Full tonight");
			} else {
				this.detail.append("Full in ").append(nights).append(nights == 1 ? " night" : " nights");
			}
		}
		float size = HudStyle.textSize();
		float textW = Math.max(HudStyle.fontStrong().width(NAMES[this.phase], size), HudStyle.font().width(this.detail, size * 0.86f));
		this.width = HudStyle.pad() * 2f + ICON + 8f + textW;
		this.height = Math.max(ICON + HudStyle.pad() * 2f - 2f, size * 2f + HudStyle.pad() * 2f + 3f);
	}

	@Override
	protected void draw(GuiGraphics g, float partialTick, boolean preview) {
		HudStyle.panel(g, 0f, 0f, this.width, this.height);
		float pad = HudStyle.pad();
		float size = HudStyle.textSize();
		float ix = pad;
		float iy = (this.height - ICON) * 0.5f;
		float px = g.pose().m00() * Minecraft.getInstance().getWindow().getGuiScale();
		int texSize = Math.max(16, Math.min(128, (int) Math.ceil(ICON * px)));
		this.ensureTexture(texSize);
		if (this.halo.on()) {
			float lit = this.phase == 4 ? 0.25f : (this.phase == 0 ? 1f : 0.6f);
			Draw.glow(g, ix, iy, ICON, ICON, ICON * 0.5f, 5f, 0f, Colors.withAlpha(0xB8C8FF, 0.22f * lit));
		}
		if (this.texture.view() != null) {
			Draw.image(g, this.texture.view(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR), ix, iy, ICON, ICON, 0f, 0f, 1f, 1f)
				.submit();
		}
		float tx = ix + ICON + 8f;
		if (this.detail.isEmpty()) {
			HudStyle.label(g, HudStyle.fontStrong(), NAMES[this.phase], tx, this.height * 0.5f, size, HudStyle.text(), Text.Align.LEFT);
		} else {
			float cy = this.height * 0.5f;
			HudStyle.label(g, HudStyle.fontStrong(), NAMES[this.phase], tx, cy - size * 0.55f - 0.5f, size, HudStyle.text(), Text.Align.LEFT);
			HudStyle.label(g, HudStyle.font(), this.detail, tx, cy + size * 0.55f + 0.5f, size * 0.86f, HudStyle.textSecondary(), Text.Align.LEFT);
		}
	}

	@Override
	protected void onDisable() {
		this.texture.close();
		if (this.pixels != null) {
			MemoryUtil.memFree(this.pixels);
			this.pixels = null;
		}
		this.renderedPhase = -1;
	}

	private void ensureTexture(int n) {
		MipTexture.CpuImage surface = MoonSky.albedoCpu();
		boolean withSurface = surface != null;
		if (n == this.renderedSize && this.phase == this.renderedPhase && withSurface == this.renderedWithSurface && this.texture.view() != null) {
			return;
		}
		if (this.pixels == null || this.pixels.capacity() < n * n * 4) {
			if (this.pixels != null) {
				MemoryUtil.memFree(this.pixels);
			}
			this.pixels = MemoryUtil.memAlloc(128 * 128 * 4);
		}
		renderMoon(this.pixels, n, this.phase, surface);
		this.texture.upload(this.pixels, n, n);
		this.renderedSize = n;
		this.renderedPhase = this.phase;
		this.renderedWithSurface = withSurface;
	}

	/** Renders the lit disc with 3x3 supersampling (straight alpha). Runs only when phase or size changes. */
	private static void renderMoon(ByteBuffer out, int n, int phase, MipTexture.CpuImage surface) {
		// Phase angle: 0 at full, 180 at new; waning phases are lit from the left, waxing from the right.
		double angle = Math.toRadians(phase <= 4 ? phase * 45.0 : (8 - phase) * 45.0);
		float side = phase > 0 && phase < 4 ? -1f : 1f;
		float lx = (float) Math.sin(angle) * side;
		float lz = (float) Math.cos(angle);
		long base = MemoryUtil.memAddress(out);
		for (int py = 0; py < n; py++) {
			for (int px = 0; px < n; px++) {
				float r = 0f;
				float gr = 0f;
				float b = 0f;
				int inside = 0;
				for (int sy = 0; sy < 3; sy++) {
					for (int sx = 0; sx < 3; sx++) {
						float x = ((px + (sx + 0.5f) / 3f) / n) * 2f - 1f;
						float y = ((py + (sy + 0.5f) / 3f) / n) * 2f - 1f;
						float r2 = x * x + y * y;
						if (r2 >= 1f) {
							continue;
						}
						inside++;
						float nx = x;
						float ny = -y;
						float nz = (float) Math.sqrt(1f - r2);
						float u = (float) (Math.atan2(nx, nz) / (Math.PI * 2.0) + 0.5);
						float v = (float) (0.5 - Math.asin(Math.max(-1f, Math.min(ny, 1f))) / Math.PI);
						float ar;
						float ag;
						float ab;
						if (surface != null) {
							ar = srgbToLinear(surface.sample(u, v, 0));
							ag = srgbToLinear(surface.sample(u, v, 1));
							ab = srgbToLinear(surface.sample(u, v, 2));
						} else {
							ar = 0.16f;
							ag = 0.16f;
							ab = 0.17f;
						}
						float geo = nx * lx + nz * lz;
						float mu0 = Math.max(geo, 0f);
						float mu = Math.max(nz, 0.05f);
						float lommel = mu0 + mu > 0f ? 2f * mu0 / (mu0 + mu) : 0f;
						float brdf = mu0 + (lommel - mu0) * 0.55f;
						float t = Math.max(0f, Math.min((geo + 0.08f) / 0.16f, 1f));
						float term = t * t * (3f - 2f * t);
						float lit = brdf * term * 2.6f;
						// Earthshine: brighter than physical so a new moon still reads on the HUD.
						float shine = (1f - term) * 0.13f;
						r += tonemap(ar * lit + ar * shine * 0.92f);
						gr += tonemap(ag * lit + ag * shine * 0.97f);
						b += tonemap(ab * lit + ab * shine * 1.12f);
					}
				}
				long o = base + ((long) py * n + px) * 4L;
				if (inside == 0) {
					MemoryUtil.memPutInt(o, 0);
					continue;
				}
				MemoryUtil.memPutByte(o, (byte) Math.round(linearToSrgb(r / inside) * 255f));
				MemoryUtil.memPutByte(o + 1, (byte) Math.round(linearToSrgb(gr / inside) * 255f));
				MemoryUtil.memPutByte(o + 2, (byte) Math.round(linearToSrgb(b / inside) * 255f));
				MemoryUtil.memPutByte(o + 3, (byte) Math.round(inside / 9f * 255f));
			}
		}
	}

	private static float tonemap(float x) {
		return 1f - (float) Math.exp(-x * 1.15f);
	}

	private static float srgbToLinear(float c) {
		return c <= 0.04045f ? c / 12.92f : (float) Math.pow((c + 0.055f) / 1.055f, 2.4);
	}

	private static float linearToSrgb(float c) {
		float v = Math.max(0f, Math.min(c, 1f));
		return v <= 0.0031308f ? v * 12.92f : (float) (1.055 * Math.pow(v, 1.0 / 2.4) - 0.055);
	}
}
