package dev.xeys.visuals.modules.hud;

import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.anim.Easing;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.hud.HudModule;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.HudStyle;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import dev.xeys.visuals.util.Fmt;
import java.time.LocalTime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ServerData;

/** The XEYS wordmark with a periodic light sheen, or a compact info bar. */
public final class WatermarkModule extends HudModule {
	public enum Style {
		WORDMARK,
		BAR
	}

	private static final String MARK = "XEYS";
	private static final String SUB = "VISUALS";
	private static final float TRACKING = 0.2f;
	private static final float SUB_TRACKING = 0.55f;

	private final EnumSetting<Style> style = this.add(new EnumSetting<>("style", "Style", Style.WORDMARK).group("Display"));
	private final NumberSetting markSize = this.add(new NumberSetting("size", "Size", 22f, 14f, 44f, 1f).unit("px")
		.visibleWhen(() -> this.style.is(Style.WORDMARK)).group("Display"));
	private final BoolSetting subtitle = this.add(new BoolSetting("subtitle", "Subtitle", true)
		.visibleWhen(() -> this.style.is(Style.WORDMARK)).group("Display"));
	private final BoolSetting fps = this.add(new BoolSetting("fps", "FPS", true).visibleWhen(() -> this.style.is(Style.BAR)).group("Display"));
	private final BoolSetting time = this.add(new BoolSetting("time", "Time", true).visibleWhen(() -> this.style.is(Style.BAR)).group("Display"));
	private final BoolSetting user = this.add(new BoolSetting("username", "Username", false).visibleWhen(() -> this.style.is(Style.BAR))
		.group("Display"));
	private final BoolSetting server = this.add(new BoolSetting("server", "Server", false).visibleWhen(() -> this.style.is(Style.BAR))
		.group("Display"));
	private final BoolSetting sheen = this.add(new BoolSetting("sheen", "Light Sheen", true).describe("A band of light sweeps across the logo")
		.group("Animation"));
	private final NumberSetting interval = this.add(new NumberSetting("interval", "Sheen Interval", 6f, 2f, 20f, 0.5f).unit("s").decimals(1)
		.visibleWhen(this.sheen::on).group("Animation"));
	private final BoolSetting glow = this.add(new BoolSetting("glow", "Accent Glow", true).group("Animation"));

	private final StringBuilder[] segments = {new StringBuilder(12), new StringBuilder(8), new StringBuilder(20), new StringBuilder(32)};
	private int segmentCount;
	private long refreshedAt;

	public WatermarkModule() {
		super("watermark", "Watermark", Icons.SPARKLES, "The XEYS logo with an animated sheen", HudPosition.Anchor.TOP_LEFT, 8f, 6f);
		this.enabledByDefault();
	}

	private void refreshSegments() {
		long now = System.nanoTime();
		if (now - this.refreshedAt < 500_000_000L && this.segmentCount > 0) {
			return;
		}
		this.refreshedAt = now;
		Minecraft mc = Minecraft.getInstance();
		this.segmentCount = 0;
		if (this.fps.on()) {
			this.segments[this.segmentCount++].setLength(0);
			this.segments[this.segmentCount - 1].append(mc.getFps()).append(" fps");
		}
		if (this.time.on()) {
			StringBuilder sb = this.segments[this.segmentCount++];
			sb.setLength(0);
			LocalTime t = LocalTime.now();
			Fmt.pad2(sb, t.getHour()).append(':');
			Fmt.pad2(sb, t.getMinute());
		}
		if (this.user.on()) {
			StringBuilder sb = this.segments[this.segmentCount++];
			sb.setLength(0);
			sb.append(mc.getUser().getName());
		}
		if (this.server.on()) {
			StringBuilder sb = this.segments[this.segmentCount++];
			sb.setLength(0);
			ServerData data = mc.getCurrentServer();
			sb.append(mc.hasSingleplayerServer() || data == null ? "Singleplayer" : data.ip);
		}
	}

	@Override
	protected void layout(boolean preview) {
		if (this.style.is(Style.WORDMARK)) {
			float s = this.markSize.value();
			float markW = Fonts.DISPLAY.width(MARK, s, TRACKING);
			float cap = Fonts.DISPLAY.capHeight(s);
			float w = markW;
			float h = cap;
			if (this.subtitle.on()) {
				float ss = s * 0.34f;
				w = Math.max(w, Fonts.SEMIBOLD.width(SUB, ss, SUB_TRACKING));
				h += s * 0.3f + Fonts.SEMIBOLD.capHeight(ss);
			}
			this.width = w + 4f;
			this.height = h + 4f;
		} else {
			this.refreshSegments();
			float size = HudStyle.textSize();
			float w = HudStyle.pad() * 2f + Fonts.DISPLAY.width(MARK, size * 1.05f, 0.12f);
			for (int i = 0; i < this.segmentCount; i++) {
				w += 15f + HudStyle.font().width(this.segments[i], size);
			}
			this.width = w;
			this.height = size + HudStyle.pad() * 2f + 2f;
		}
	}

	@Override
	protected void draw(GuiGraphics g, float partialTick, boolean preview) {
		if (this.style.is(Style.WORDMARK)) {
			this.drawWordmark(g);
		} else {
			this.drawBar(g);
		}
	}

	private void drawWordmark(GuiGraphics g) {
		float s = this.markSize.value();
		float cap = Fonts.DISPLAY.capHeight(s);
		float left = 2f;
		float baseline = 2f + cap;
		float markW = Fonts.DISPLAY.width(MARK, s, TRACKING);
		if (this.glow.on()) {
			Text.shadow(g, Fonts.DISPLAY, MARK, left, baseline, s, Colors.withAlpha(Theme.accent(), 0.34f), 0f, 0f, s * 0.22f, TRACKING);
		} else {
			Text.shadow(g, Fonts.DISPLAY, MARK, left, baseline, s, Colors.withAlpha(0x000000, 0.5f), 0f, s * 0.04f, s * 0.12f, TRACKING);
		}
		Text.drawBaseline(g, Fonts.DISPLAY, MARK, left, baseline, s, 0xFFDDE2EC, TRACKING, 0f);
		if (this.sheen.on()) {
			this.drawSheen(g, left, baseline, s, markW, cap);
		}
		if (this.subtitle.on()) {
			float ss = s * 0.34f;
			float sw = Fonts.SEMIBOLD.width(SUB, ss, SUB_TRACKING);
			float sy = baseline + s * 0.3f + Fonts.SEMIBOLD.capHeight(ss) * 0.5f;
			float sx = left + (markW - sw) * 0.5f;
			Text.drawMiddle(g, Fonts.SEMIBOLD, SUB, sx, sy, ss, Theme.TEXT_SECONDARY, Text.Align.LEFT, SUB_TRACKING);
			float line = Math.max(0f, (markW - sw) * 0.5f - 5f);
			if (line > 2f) {
				int accent = Theme.accent();
				Draw.rect(g, left, sy - 0.5f, line, 1f).gradientH(Colors.withAlpha(accent, 0f), Colors.withAlpha(accent, 0.8f)).submit();
				Draw.rect(g, left + markW - line, sy - 0.5f, line, 1f).gradientH(Colors.withAlpha(accent, 0.8f), Colors.withAlpha(accent, 0f)).submit();
			}
		}
	}

	private void drawSheen(GuiGraphics g, float left, float baseline, float s, float markW, float cap) {
		double period = this.interval.value();
		double t = Clock.time() % period;
		float dur = 1.1f;
		if (t >= dur) {
			return;
		}
		float p = Easing.IN_OUT_SINE.apply((float) (t / dur));
		float band = s * 0.8f;
		float bx = left - band * 2f + (markW + band * 4f) * p;
		float px = g.pose().m00() * Minecraft.getInstance().getWindow().getGuiScale();
		Draw.pushClip(g, bx, baseline - cap - s * 0.5f, bx + band, baseline + s * 0.5f, band * px * 0.45f);
		Text.drawBaseline(g, Fonts.DISPLAY, MARK, left, baseline, s, 0xFFFFFFFF, TRACKING, 0.55f);
		Text.shadow(g, Fonts.DISPLAY, MARK, left, baseline, s, Colors.withAlpha(0xFFFFFF, 0.4f), 0f, 0f, s * 0.14f, TRACKING);
		Draw.popClip();
	}

	private void drawBar(GuiGraphics g) {
		HudStyle.panel(g, 0f, 0f, this.width, this.height);
		float size = HudStyle.textSize();
		float cy = this.height * 0.5f;
		float x = HudStyle.pad();
		float ms = size * 1.05f;
		float markW = Fonts.DISPLAY.width(MARK, ms, 0.12f);
		float base = cy + Fonts.DISPLAY.capHeight(ms) * 0.5f;
		if (this.glow.on()) {
			Text.shadow(g, Fonts.DISPLAY, MARK, x, base, ms, Colors.withAlpha(Theme.accent(), 0.45f), 0f, 0f, 3f, 0.12f);
		}
		Text.drawBaseline(g, Fonts.DISPLAY, MARK, x, base, ms, Theme.accent(), 0.12f, 0f);
		if (this.sheen.on()) {
			this.drawSheenSmall(g, x, base, ms, markW);
		}
		x += markW;
		for (int i = 0; i < this.segmentCount; i++) {
			Draw.circle(g, x + 7.5f, cy, 1.1f, HudStyle.textSecondary());
			x += 15f;
			x += HudStyle.label(g, HudStyle.font(), this.segments[i], x, cy, size, HudStyle.text(), Text.Align.LEFT);
		}
	}

	private void drawSheenSmall(GuiGraphics g, float left, float baseline, float s, float markW) {
		double period = this.interval.value();
		double t = Clock.time() % period;
		float dur = 0.9f;
		if (t >= dur) {
			return;
		}
		float p = Easing.IN_OUT_SINE.apply((float) (t / dur));
		float band = s * 0.9f;
		float bx = left - band * 2f + (markW + band * 4f) * p;
		float px = g.pose().m00() * Minecraft.getInstance().getWindow().getGuiScale();
		Draw.pushClip(g, bx, baseline - s, bx + band, baseline + s * 0.4f, band * px * 0.45f);
		Text.drawBaseline(g, Fonts.DISPLAY, MARK, left, baseline, s, 0xFFFFFFFF, 0.12f, 0.4f);
		Draw.popClip();
	}
}
