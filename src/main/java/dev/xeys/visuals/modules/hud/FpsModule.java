package dev.xeys.visuals.modules.hud;

import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.HudStyle;
import dev.xeys.visuals.hud.InfoHudModule;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import dev.xeys.visuals.util.Fmt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Frame rate, optional frame time and a live frame-time graph. */
public final class FpsModule extends InfoHudModule implements Events.FrameStart {
	private static final int SAMPLES = 48;
	private final BoolSetting colorCoded = this.add(new BoolSetting("color_coded", "Performance Colors", false)
		.describe("Green when smooth, amber under 60, red under 30").group("Display"));
	private final BoolSetting frameTime = this.add(new BoolSetting("frame_time", "Frame Time", false)
		.describe("Show milliseconds per frame").group("Display"));
	private final BoolSetting graph = this.add(new BoolSetting("graph", "Frame Graph", false)
		.describe("A small live graph of recent frame times").group("Display"));
	private final float[] samples = new float[SAMPLES];
	private int head;
	private int fps;

	public FpsModule() {
		super("fps", "FPS", Icons.GAUGE, "Frames per second with optional frame-time graph", "FPS", 4f, HudPosition.Anchor.TOP_LEFT, 6f, 44f);
		this.enabledByDefault();
	}

	@Override
	public void onFrameStart() {
		this.samples[this.head] = Clock.delta() * 1000f;
		this.head = (this.head + 1) % SAMPLES;
	}

	@Override
	protected void update(StringBuilder out, boolean preview) {
		this.fps = preview && Minecraft.getInstance().level == null ? 144 : Minecraft.getInstance().getFps();
		out.append(this.fps);
		if (this.frameTime.on()) {
			out.append("  ");
			Fmt.fixed(out, this.fps > 0 ? 1000.0 / this.fps : 0.0, 1).append(" ms");
		}
	}

	@Override
	protected int valueColor() {
		if (!this.colorCoded.on()) {
			return HudStyle.text();
		}
		if (this.fps >= 60) {
			return this.fps >= 120 ? Theme.SUCCESS : HudStyle.text();
		}
		return this.fps >= 30 ? Theme.WARNING : Theme.DANGER;
	}

	@Override
	protected void layout(boolean preview) {
		super.layout(preview);
		if (this.graph.on()) {
			this.width = Math.max(this.width, 64f);
			this.height += 16f;
		}
	}

	@Override
	protected void draw(GuiGraphics g, float partialTick, boolean preview) {
		if (!this.graph.on()) {
			super.draw(g, partialTick, preview);
			return;
		}
		float textH = HudStyle.textSize() + HudStyle.pad() * 2f + 1f;
		float fullH = this.height;
		this.height = textH;
		HudStyle.panel(g, 0f, 0f, this.width, fullH);
		// Text row reuses the base layout without its panel.
		float pad = HudStyle.pad();
		g.pose().pushMatrix();
		super.drawContentOnly(g);
		g.pose().popMatrix();
		this.height = fullH;
		float gx = pad;
		float gw = this.width - pad * 2f;
		float gh = 11f;
		float gy = fullH - pad - gh;
		Draw.rect(g, gx, gy, gw, gh, 2f, Colors.withAlpha(0xFFFFFF, 0.05f));
		float bw = gw / SAMPLES;
		int accent = HudStyle.accent();
		for (int i = 0; i < SAMPLES; i++) {
			float ms = this.samples[(this.head + i) % SAMPLES];
			float t = Math.min(ms / 50f, 1f);
			float bh = Math.max(1f, gh * t);
			int c = ms > 33f ? Theme.DANGER : (ms > 17.5f ? Theme.WARNING : accent);
			Draw.rect(g, gx + i * bw, gy + gh - bh, Math.max(bw - 0.4f, 0.4f), bh).color(Colors.withAlpha(c, 0.85f)).submit();
		}
	}
}
