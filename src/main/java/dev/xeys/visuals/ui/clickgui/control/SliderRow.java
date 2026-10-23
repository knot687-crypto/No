package dev.xeys.visuals.ui.clickgui.control;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.xeys.visuals.anim.Spring;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.clickgui.GuiContext;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.gui.GuiGraphics;

/** Slider with a smoothly following fill, a value pill and a knob that grows while dragged. */
public final class SliderRow extends SettingRow<NumberSetting> {
	private final Spring fill;
	private final Spring grab = new Spring(0f, 420f, 0.85f);
	private boolean dragging;
	private float trackX;
	private float trackW;
	private float trackY;

	public SliderRow(NumberSetting setting, GuiContext ctx) {
		super(setting, ctx);
		this.fill = new Spring(setting.fraction(), 420f, 1f);
	}

	@Override
	public float preferredHeight() {
		return this.setting.description().isEmpty() ? 58f : 68f;
	}

	@Override
	protected void drawControl(GuiGraphics g, float mx, float my) {
		boolean desc = !this.setting.description().isEmpty();
		float labelY = this.y + (desc ? 22f : 18f);
		this.drawLabel(g, labelY, this.w - PAD * 2f - 80f);

		String value = this.setting.format();
		float vw = Fonts.SEMIBOLD.width(value, 12f) + 16f;
		float vx = this.x + this.w - PAD - vw;
		float vy = this.y + (desc ? 12f : 8f);
		Draw.rect(g, vx, vy, vw, 20f).pill().color(Colors.lerp(0x14FFFFFF, Theme.accentSoft(), this.grab.get())).submit();
		Text.drawMiddle(g, Fonts.SEMIBOLD, value, vx + vw * 0.5f, vy + 10f, 12f, Colors.lerpOklab(Theme.TEXT_SECONDARY, Theme.accentText(), this.grab.get()), Text.Align.CENTER);

		this.trackX = this.x + PAD;
		this.trackW = this.w - PAD * 2f;
		this.trackY = this.y + this.h - 17f;
		boolean overTrack = mx >= this.trackX - 6f && mx <= this.trackX + this.trackW + 6f && Math.abs(my - this.trackY) < 11f;
		if (overTrack || this.dragging) {
			g.requestCursor(CursorTypes.RESIZE_EW);
		}
		if (this.dragging) {
			this.setting.setFraction((mx - this.trackX) / this.trackW);
		}
		this.fill.target(this.setting.fraction());
		this.grab.target(this.dragging ? 1f : (overTrack ? 0.45f : 0f));

		float f = Math.max(0f, Math.min(1f, this.fill.get()));
		float th = 5f;
		Draw.rect(g, this.trackX, this.trackY - th * 0.5f, this.trackW, th).pill().color(0xFF1C1C21).border(1f, 0x0FFFFFFF).submit();
		float fw = Math.max(th, this.trackW * f);
		Draw.rect(g, this.trackX, this.trackY - th * 0.5f, fw, th).pill().gradientH(Theme.accentPressed(), Theme.accent()).submit();
		float kx = this.trackX + this.trackW * f;
		float gr = this.grab.get();
		float kr = 7f + 1.8f * gr;
		Draw.glow(g, kx - kr, this.trackY - kr, kr * 2f, kr * 2f, kr, 12f, 0f, Colors.withAlpha(Theme.accent(), 0.35f * gr));
		Draw.shadow(g, kx - kr, this.trackY - kr, kr * 2f, kr * 2f, kr, 0f, 1.5f, 5f, 0f, 0x73000000);
		Draw.circle(g, kx, this.trackY, kr, 0xFFFFFFFF);
		Draw.circle(g, kx, this.trackY, 2.4f * gr, Colors.withAlpha(Theme.accent(), gr));
	}

	@Override
	protected boolean clicked(float mx, float my, int button) {
		if (button == 0 && mx >= this.trackX - 8f && mx <= this.trackX + this.trackW + 8f && Math.abs(my - this.trackY) < 13f) {
			this.dragging = true;
			this.setting.setFraction((mx - this.trackX) / this.trackW);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(float mx, float my, int button) {
		boolean was = this.dragging;
		this.dragging = false;
		return was;
	}

	@Override
	public boolean mouseDragged(float mx, float my, int button, float dx, float dy) {
		return this.dragging;
	}

	@Override
	public boolean mouseScrolled(float mx, float my, float amount) {
		if (this.contains(mx, my) && Math.abs(my - this.trackY) < 14f) {
			this.setting.set(this.setting.value() + Math.signum(amount) * Math.max(this.setting.step(), (this.setting.max() - this.setting.min()) / 100f));
			return true;
		}
		return false;
	}
}
