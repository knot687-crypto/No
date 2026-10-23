package dev.xeys.visuals.modules.hud;

import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.hud.HudMath;
import dev.xeys.visuals.hud.HudModule;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.HudStyle;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.render.text.XeysFont;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.gui.GuiGraphics;

/** A horizontal compass tape: ticks and compass points slide past a fixed center marker. */
public final class HeadingBarModule extends HudModule {
	private static final String[] NUMBERS = new String[24];

	static {
		for (int i = 0; i < 24; i++) {
			NUMBERS[i] = Integer.toString(i * 15);
		}
	}

	private final NumberSetting barWidth = this.add(new NumberSetting("width", "Width", 220f, 120f, 420f, 4f).unit("px").group("Display"));
	private final NumberSetting span = this.add(new NumberSetting("span", "Field of View", 150f, 60f, 300f, 5f).unit("°")
		.describe("How many degrees the tape shows").group("Display"));
	private final BoolSetting numbers = this.add(new BoolSetting("numbers", "Degree Numbers", true).describe("Numbers every 15 degrees")
		.group("Display"));
	private final BoolSetting readout = this.add(new BoolSetting("readout", "Bearing Readout", true).describe("Exact bearing at the marker")
		.group("Display"));
	private final ColorSetting north = this.add(new ColorSetting("north", "North", 0xFFFF6B6B).group("Colors"));
	private final ColorSetting marker = this.add(new ColorSetting("marker", "Marker", 0xFFFFFFFF, ColorSetting.Mode.ACCENT).group("Colors"));
	private final StringBuilder value = new StringBuilder(8);

	public HeadingBarModule() {
		super("heading_bar", "Heading Bar", Icons.NAVIGATION, "A compass tape with a fixed center marker", HudPosition.Anchor.TOP_CENTER, 0f, 6f);
	}

	@Override
	protected void layout(boolean preview) {
		this.width = this.barWidth.value();
		this.height = Math.round(HudStyle.textSize() * 1.35f + 14f);
	}

	@Override
	protected void draw(GuiGraphics g, float partialTick, boolean preview) {
		float w = this.width;
		float h = this.height;
		float half = w * 0.5f;
		float bearing = HudMath.heading(partialTick);
		float ppd = w / this.span.value();
		float size = HudStyle.textSize();
		XeysFont font = HudStyle.font();
		XeysFont strong = HudStyle.fontStrong();
		int text = HudStyle.text();
		int muted = HudStyle.textSecondary();
		int markerColor = this.marker.color();
		float labelY = h * 0.42f;
		float tickBase = h - 3f;
		float fade = w * 0.22f;

		HudStyle.panel(g, 0f, 0f, w, h);

		// The readout box hides labels that would sit underneath it.
		float boxHalf = 0f;
		if (this.readout.on()) {
			this.value.setLength(0);
			this.value.append(Math.floorMod(Math.round(bearing), 360));
			boxHalf = strong.width(this.value, size) * 0.5f + 7f;
		}

		Draw.pushClip(g, 1f, 0f, w - 1f, h);
		float halfSpan = this.span.value() * 0.5f + 5f;
		int first = (int) Math.floor((bearing - halfSpan) / 5f) * 5;
		for (int b = first; b <= bearing + halfSpan; b += 5) {
			float x = half + (b - bearing) * ppd;
			float dist = Math.abs(x - half);
			float a = HudMath.smoothstep(half, half - fade, dist);
			if (a <= 0.01f) {
				continue;
			}
			int deg = Math.floorMod(b, 360);
			boolean point = deg % 45 == 0;
			boolean major = deg % 15 == 0;
			float len = point ? 5.5f : (major ? 4f : 2.2f);
			int tickColor = deg == 0 ? this.north.color() : (point ? muted : Colors.mulAlpha(text, 0.28f));
			Draw.line(g, x, tickBase - len, x, tickBase, point ? 1.3f : 1f, Colors.mulAlpha(tickColor, a));
			if (!point && !(major && this.numbers.on())) {
				continue;
			}
			// Fade labels out under the readout box.
			float la = boxHalf > 0f ? a * HudMath.smoothstep(boxHalf, boxHalf + 8f, dist) : a;
			if (la <= 0.01f) {
				continue;
			}
			if (point) {
				int idx = deg / 45;
				int c = deg == 0 ? this.north.color() : (idx % 2 == 0 ? text : muted);
				float ls = idx % 2 == 0 ? size : size * 0.84f;
				HudStyle.label(g, idx % 2 == 0 ? strong : font, HudMath.pointLabel(idx), x, labelY, ls, Colors.mulAlpha(c, la), Text.Align.CENTER);
			} else {
				HudStyle.label(g, font, NUMBERS[deg / 15], x, labelY, size * 0.74f, Colors.mulAlpha(muted, la * 0.85f), Text.Align.CENTER);
			}
		}
		Draw.popClip();

		// Center marker: caps top and bottom with a soft glow.
		Draw.glow(g, half - 1.5f, h - 7.5f, 3f, 6f, 1.5f, 3f, 0f, Colors.mulAlpha(markerColor, 0.5f));
		Draw.line(g, half, h - 7f, half, h - 1.8f, 2f, markerColor);
		if (this.readout.on()) {
			HudStyle.label(g, strong, this.value, half, labelY, size, markerColor, Text.Align.CENTER);
		} else {
			Draw.line(g, half, 1.8f, half, 5f, 2f, markerColor);
		}
	}
}
