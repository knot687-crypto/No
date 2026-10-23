package dev.xeys.visuals.modules.hud;

import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.hud.HudMath;
import dev.xeys.visuals.hud.HudModule;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.HudStyle;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.gui.GuiGraphics;

/** A round compass: a dial that turns under a fixed pointer, or a needle over a fixed dial. */
public final class CompassModule extends HudModule {
	public enum Style {
		DIAL,
		NEEDLE
	}

	private static final String[] CARDINALS = {"N", "E", "S", "W"};
	private static final float RAD = (float) (Math.PI / 180.0);

	private final EnumSetting<Style> style = this.add(new EnumSetting<>("style", "Style", Style.DIAL)
		.describe("Turn the dial under a fixed pointer, or swing a needle over a fixed dial").group("Display"));
	private final NumberSetting size = this.add(new NumberSetting("size", "Size", 64f, 40f, 128f, 2f).unit("px").group("Display"));
	private final BoolSetting heading = this.add(new BoolSetting("heading", "Bearing", true).describe("Degrees in the center").group("Display"));
	private final BoolSetting intercardinals = this.add(new BoolSetting("intercardinals", "Intercardinal Marks", true)
		.describe("Longer ticks at NE, SE, SW and NW").group("Display"));
	private final ColorSetting north = this.add(new ColorSetting("north", "North", 0xFFFF6B6B).group("Colors"));
	private final ColorSetting pointer = this.add(new ColorSetting("pointer", "Pointer", 0xFFFFFFFF, ColorSetting.Mode.ACCENT).group("Colors"));
	private final StringBuilder readout = new StringBuilder(8);

	public CompassModule() {
		super("compass", "Compass", Icons.COMPASS, "A round compass that turns with you", HudPosition.Anchor.TOP_RIGHT, -6f, 140f);
	}

	@Override
	protected void layout(boolean preview) {
		this.width = this.size.value();
		this.height = this.width;
	}

	@Override
	protected void draw(GuiGraphics g, float partialTick, boolean preview) {
		float s = this.width;
		float r = s * 0.5f;
		float k = s / 64f;
		float bearing = HudMath.heading(partialTick);
		boolean dial = this.style.is(Style.DIAL);
		// Screen angle (clockwise from 12 o'clock) of a world bearing.
		float turn = dial ? -bearing : 0f;
		int text = HudStyle.text();
		int muted = HudStyle.textSecondary();
		int faint = Colors.mulAlpha(text, 0.3f);
		int pointerColor = this.pointer.color();

		HudStyle.panel(g, 0f, 0f, s, s, r);

		// Tick ring every 15 degrees; cardinals carry letters instead of ticks.
		float outer = r - 5.5f * k;
		for (int b = 15; b < 360; b += 15) {
			if (b % 90 == 0) {
				continue;
			}
			boolean inter = b % 45 == 0 && this.intercardinals.on();
			float len = (inter ? 4.5f : 2.6f) * k;
			float a = (b + turn) * RAD;
			float sin = (float) Math.sin(a);
			float cos = (float) Math.cos(a);
			Draw.line(g, r + sin * outer, r - cos * outer, r + sin * (outer - len), r - cos * (outer - len), (inter ? 1.3f : 0.9f) * k,
				inter ? muted : faint);
		}

		float letterR = outer - 9.5f * k;
		float letterSize = 8.5f * k;
		for (int i = 0; i < 4; i++) {
			float a = (i * 90f + turn) * RAD;
			float lx = r + (float) Math.sin(a) * letterR;
			float ly = r - (float) Math.cos(a) * letterR;
			int c = i == 0 ? this.north.color() : (dial ? muted : text);
			HudStyle.label(g, i == 0 ? HudStyle.fontStrong() : HudStyle.font(), CARDINALS[i], lx, ly, letterSize, c, Text.Align.CENTER);
		}

		if (dial) {
			// Fixed lubber line at the top: what you are facing.
			Draw.glow(g, r - 1.2f * k, 1.5f * k, 2.4f * k, 6.5f * k, 1.2f * k, 3f * k, 0f, Colors.mulAlpha(pointerColor, 0.55f));
			Draw.line(g, r, 2.5f * k, r, 7.5f * k, 2.2f * k, pointerColor);
		} else {
			float a = bearing * RAD;
			float sin = (float) Math.sin(a);
			float cos = (float) Math.cos(a);
			float tip = outer - 3f * k;
			Draw.line(g, r - sin * 7f * k, r + cos * 7f * k, r, r, 1.6f * k, faint);
			Draw.line(g, r, r, r + sin * tip, r - cos * tip, 2.2f * k, pointerColor);
			Draw.circle(g, r, r, 2.6f * k, text);
		}

		if (this.heading.on() && dial) {
			this.readout.setLength(0);
			this.readout.append(Math.floorMod(Math.round(bearing), 360)).append('°');
			// Optical centering: the degree sign hangs right, so nudge left by half its width.
			float shift = HudStyle.fontStrong().width("°", 9.5f * k) * 0.5f;
			HudStyle.label(g, HudStyle.fontStrong(), this.readout, r + shift, r, 9.5f * k, text, Text.Align.CENTER);
		} else if (dial) {
			Draw.circle(g, r, r, 1.6f * k, faint);
		}
	}
}
