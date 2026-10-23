package dev.xeys.visuals.ui.dev;

import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.Icons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Developer screen exercising every rendering primitive; used by the client gametests. */
public final class RenderTestScreen extends Screen {
	public RenderTestScreen() {
		super(Component.literal("XEYS render test"));
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		// The panorama is drawn immediately (before the GUI flush), so glass panels blur it.
		this.renderPanorama(g, delta);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		float gui = Minecraft.getInstance().getWindow().getGuiScale();
		g.pose().pushMatrix();
		g.pose().scale(1f / gui, 1f / gui);
		float W = Minecraft.getInstance().getWindow().getWidth();
		float H = Minecraft.getInstance().getWindow().getHeight();

		// Deferred shapes (flushed together with the glass, so glass does not blur these).
		Draw.circle(g, W * 0.3f, H * 0.35f, 220, 0x00FF4D8D);
		Draw.circle(g, W * 0.55f, H * 0.6f, 260, 0xFF4D8DFF);
		Draw.circle(g, W * 0.75f, H * 0.3f, 160, 0xFFFFC14D);
		for (int i = 0; i < 24; i++) {
			Draw.rect(g, 40 + i * 76, H - 140, 60, 60, i % 2 == 0 ? 8 : 30, 0xFF000000 | (i * 0x0A1B2C));
		}

		// Glass panel with shadow, highlight border and inner glow.
		float px = 120;
		float py = 90;
		float pw = 760;
		float ph = 520;
		Draw.shadow(g, px, py, pw, ph, 20, 0, 24, 64, 0, Theme.SHADOW);
		Draw.glass(g, px, py, pw, ph, Theme.GLASS_TINT).radius(20).border(1f, Theme.GLASS_BORDER).borderFade(0.35f).innerGlow(18, 0.6f).submit();

		Text.draw(g, Fonts.DISPLAY, "XEYS VISUALS", px + 32, py + 28, 44, Theme.TEXT);
		Text.draw(g, Fonts.MEDIUM, "Frosted glass, SDF shapes and text at every scale", px + 34, py + 84, 16, Theme.TEXT_SECONDARY);

		float ty = py + 130;
		float[] sizes = {9, 11, 13, 16, 20, 28};
		for (float s : sizes) {
			Text.draw(g, Fonts.REGULAR, "Regular " + (int) s + "px  The quick brown fox jumps over 0123456789", px + 34, ty, s, Theme.TEXT);
			ty += s * 1.45f + 4;
		}
		Text.draw(g, Fonts.MEDIUM, "Medium weight label", px + 34, ty, 16, Theme.TEXT);
		Text.draw(g, Fonts.SEMIBOLD, "SemiBold weight label", px + 234, ty, 16, Theme.accentText());
		Text.draw(g, Fonts.BOLD, "Bold AVATAR Wave", px + 454, ty, 16, Theme.TEXT);
		ty += 34;
		Text.draw(g, Fonts.REGULAR, "Кириллица  Ελληνικά  Ünïcödé €  →  ✓", px + 34, ty, 16, Theme.TEXT_SECONDARY);

		// Icons row.
		char[] icons = {Icons.SEARCH, Icons.SETTINGS, Icons.LAYOUT_DASHBOARD, Icons.SPARKLES, Icons.GLOBE, Icons.MOON, Icons.STAR, Icons.PALETTE, Icons.EYE, Icons.ORBIT};
		for (int i = 0; i < icons.length; i++) {
			float cx = px + 50 + i * 44;
			float cy = py + ph - 46;
			Draw.rect(g, cx - 18, cy - 18, 36, 36, 10, Theme.SURFACE_RAISED);
			Text.icon(g, icons[i], cx, cy, 20, i == 3 ? Theme.accent() : Theme.TEXT);
		}

		// Controls column on the right.
		float cx0 = 940;
		float cy0 = 120;
		Draw.shadow(g, cx0, cy0, 380, 560, 16, 0, 16, 48, 0, Theme.SHADOW);
		Draw.rect(g, cx0, cy0, 380, 560).radius(16).color(0xF00C0C0E).border(1f, Theme.HAIRLINE_STRONG).borderFade(0.3f).submit();
		// Toggle on / off.
		Draw.glow(g, cx0 + 30, cy0 + 30, 52, 30, 15, 18, 0, Theme.accentGlow());
		Draw.rect(g, cx0 + 30, cy0 + 30, 52, 30).pill().color(Theme.accent()).submit();
		Draw.circle(g, cx0 + 30 + 52 - 15, cy0 + 45, 11, 0xFFFFFFFF);
		Draw.rect(g, cx0 + 110, cy0 + 30, 52, 30).pill().color(Theme.SURFACE_ACTIVE).border(1f, Theme.HAIRLINE_STRONG).submit();
		Draw.circle(g, cx0 + 110 + 15, cy0 + 45, 11, 0xFFB8B8C0);
		// Slider.
		Draw.rect(g, cx0 + 30, cy0 + 100, 320, 6, 3, Theme.SURFACE_ACTIVE);
		Draw.rect(g, cx0 + 30, cy0 + 100, 200, 6).pill().gradientH(Theme.accentPressed(), Theme.accent()).submit();
		Draw.glow(g, cx0 + 222, cy0 + 95, 16, 16, 8, 14, 0, Theme.accentGlow());
		Draw.circle(g, cx0 + 230, cy0 + 103, 9, 0xFFFFFFFF);
		// Button.
		Draw.rect(g, cx0 + 30, cy0 + 140, 320, 44).radius(12).gradientV(0xFF1E1E24, 0xFF141418).border(1f, Theme.HAIRLINE_STRONG).borderFade(0.25f).submit();
		Text.drawMiddle(g, Fonts.SEMIBOLD, "Glass Button", cx0 + 190, cy0 + 162, 15, Theme.TEXT, Text.Align.CENTER);
		Draw.rect(g, cx0 + 30, cy0 + 200, 320, 44).radius(12).color(Theme.accent()).submit();
		Text.drawMiddle(g, Fonts.SEMIBOLD, "Primary Action", cx0 + 190, cy0 + 222, 15, Theme.onAccent(), Text.Align.CENTER);
		// Arcs and rings.
		float spin = (float) (Clock.time() * 90.0 % 360.0);
		Draw.ring(g, cx0 + 80, cy0 + 330, 44, 8, Theme.SURFACE_ACTIVE);
		Draw.arcGradient(g, cx0 + 80, cy0 + 330, 44, 8, 0, 250, Theme.accentPressed(), Theme.accent());
		Text.drawMiddle(g, Fonts.BOLD, "69%", cx0 + 80, cy0 + 330, 18, Theme.TEXT, Text.Align.CENTER);
		Draw.arc(g, cx0 + 200, cy0 + 330, 36, 4, spin, 90).color(Theme.accent()).submit();
		Draw.ring(g, cx0 + 300, cy0 + 330, 30, 2, Theme.HAIRLINE_STRONG);
		Draw.circle(g, cx0 + 300, cy0 + 330, 6, Theme.WARNING);
		// Lines at angles.
		for (int i = 0; i < 8; i++) {
			double a = Math.toRadians(i * 22.5);
			Draw.line(g, cx0 + 60, cy0 + 470, cx0 + 60 + (float) Math.cos(a) * 60, cy0 + 470 - (float) Math.sin(a) * 60, 1.5f + i * 0.5f, Theme.TEXT_SECONDARY);
		}
		// Soft clip: fading list.
		Draw.pushClip(g, cx0 + 180, cy0 + 400, cx0 + 350, cy0 + 540, 28f);
		for (int i = 0; i < 8; i++) {
			Draw.rect(g, cx0 + 180, cy0 + 392 + i * 22, 170, 18, 6, Theme.SURFACE_HOVER);
			Text.drawMiddle(g, Fonts.MEDIUM, "Clipped row " + (i + 1), cx0 + 190, cy0 + 401 + i * 22, 11, Theme.TEXT_SECONDARY, Text.Align.LEFT);
		}
		Draw.popClip();

		g.pose().popMatrix();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
