package dev.xeys.visuals.modules.hud;

import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.hud.HudModule;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.HudStyle;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.render.text.XeysFont;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Fmt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Player position with axis labels, optional Nether/Overworld conversion and facing axis. */
public final class CoordinatesModule extends HudModule {
	public enum Layout {
		SINGLE_LINE,
		STACKED
	}

	private final EnumSetting<Layout> layout = this.add(new EnumSetting<>("layout", "Layout", Layout.STACKED).group("Display"));
	private final NumberSetting decimals = this.add(new NumberSetting("decimals", "Decimals", 0f, 0f, 2f, 1f).group("Display"));
	private final BoolSetting conversion = this.add(new BoolSetting("conversion", "Dimension Conversion", true)
		.describe("Matching Nether or Overworld coordinates").group("Display"));
	private final BoolSetting facing = this.add(new BoolSetting("facing", "Facing Axis", true).describe("Which axis you are moving along").group("Display"));

	private final StringBuilder x = new StringBuilder();
	private final StringBuilder y = new StringBuilder();
	private final StringBuilder z = new StringBuilder();
	private final StringBuilder other = new StringBuilder();
	private String facingLabel = "+Z";
	private float valueW;

	public CoordinatesModule() {
		super("coordinates", "Coordinates", Icons.MAP_PIN, "Your position, with Nether/Overworld conversion", HudPosition.Anchor.TOP_LEFT, 6f, 66f);
		this.enabledByDefault();
	}

	@Override
	protected void layout(boolean preview) {
		Minecraft mc = Minecraft.getInstance();
		Player p = mc.player;
		double px = p != null ? p.getX() : 128.4;
		double py = p != null ? p.getY() : 64.0;
		double pz = p != null ? p.getZ() : -256.7;
		int d = this.decimals.intValue();
		this.x.setLength(0);
		Fmt.fixed(this.x, px, d);
		this.y.setLength(0);
		Fmt.fixed(this.y, py, d);
		this.z.setLength(0);
		Fmt.fixed(this.z, pz, d);
		boolean nether = p != null && p.level().dimension() == Level.NETHER;
		boolean showConv = this.conversion.on() && (p == null || p.level().dimension() == Level.OVERWORLD || nether);
		this.other.setLength(0);
		if (showConv) {
			double f = nether ? 8.0 : 0.125;
			this.other.append(nether ? "Overworld  " : "Nether  ");
			Fmt.fixed(this.other, px * f, 0).append(", ");
			Fmt.fixed(this.other, pz * f, 0);
		}
		if (p != null) {
			Direction dir = p.getDirection();
			this.facingLabel = switch (dir) {
				case NORTH -> "-Z";
				case SOUTH -> "+Z";
				case WEST -> "-X";
				case EAST -> "+X";
				default -> "";
			};
		}

		float size = HudStyle.textSize();
		float pad = HudStyle.pad();
		XeysFont strong = HudStyle.fontStrong();
		XeysFont font = HudStyle.font();
		float axisW = font.width("X", size) + 5f;
		if (this.layout.is(Layout.STACKED)) {
			this.valueW = Math.max(strong.width(this.x, size), Math.max(strong.width(this.y, size), strong.width(this.z, size)));
			float w = axisW + this.valueW;
			if (this.facing.on()) {
				w += font.width("+Z", size) + 8f;
			}
			if (showConv) {
				w = Math.max(w, font.width(this.other, size * 0.86f));
			}
			this.width = Math.max(w + pad * 2f, 60f);
			float line = size * 1.45f;
			this.height = pad * 2f + line * 3f + (showConv ? line * 0.95f : 0f) - (line - size) + 1f;
		} else {
			float w = axisW * 3f + strong.width(this.x, size) + strong.width(this.y, size) + strong.width(this.z, size) + 16f;
			if (this.facing.on()) {
				w += font.width("+Z", size) + 8f;
			}
			if (showConv) {
				w += font.width(this.other, size) + 10f;
			}
			this.width = w + pad * 2f;
			this.height = size + pad * 2f + 1f;
		}
	}

	@Override
	protected void draw(GuiGraphics g, float partialTick, boolean preview) {
		HudStyle.panel(g, 0f, 0f, this.width, this.height);
		float size = HudStyle.textSize();
		float pad = HudStyle.pad();
		XeysFont font = HudStyle.font();
		XeysFont strong = HudStyle.fontStrong();
		int accent = HudStyle.accent();
		int text = HudStyle.text();
		int muted = HudStyle.textSecondary();
		float axisW = font.width("X", size) + 5f;
		if (this.layout.is(Layout.STACKED)) {
			float line = size * 1.45f;
			float cy = pad + size * 0.5f + 0.5f;
			StringBuilder[] values = {this.x, this.y, this.z};
			String[] axes = {"X", "Y", "Z"};
			for (int i = 0; i < 3; i++) {
				HudStyle.label(g, font, axes[i], pad, cy, size, accent, Text.Align.LEFT);
				HudStyle.label(g, strong, values[i], pad + axisW, cy, size, text, Text.Align.LEFT);
				if (i == 0 && this.facing.on() && !this.facingLabel.isEmpty()) {
					HudStyle.label(g, font, this.facingLabel, this.width - pad, cy, size, muted, Text.Align.RIGHT);
				}
				cy += line;
			}
			if (!this.other.isEmpty()) {
				HudStyle.label(g, font, this.other, pad, cy - line * 0.05f, size * 0.86f, muted, Text.Align.LEFT);
			}
		} else {
			float cx = pad;
			float cy = this.height * 0.5f;
			StringBuilder[] values = {this.x, this.y, this.z};
			String[] axes = {"X", "Y", "Z"};
			for (int i = 0; i < 3; i++) {
				cx += HudStyle.label(g, font, axes[i], cx, cy, size, accent, Text.Align.LEFT) + 5f;
				cx += HudStyle.label(g, strong, values[i], cx, cy, size, text, Text.Align.LEFT) + 8f;
			}
			if (this.facing.on() && !this.facingLabel.isEmpty()) {
				cx += HudStyle.label(g, font, this.facingLabel, cx, cy, size, muted, Text.Align.LEFT) + 10f;
			}
			if (!this.other.isEmpty()) {
				HudStyle.label(g, font, this.other, cx, cy, size, muted, Text.Align.LEFT);
			}
		}
	}
}
