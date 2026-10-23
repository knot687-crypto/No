package dev.xeys.visuals.ui.clickgui.control;

import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.clickgui.GuiContext;
import dev.xeys.visuals.ui.core.UiScale;
import dev.xeys.visuals.ui.core.UiSounds;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.gui.GuiGraphics;

public final class ColorRow extends SettingRow<ColorSetting> {
	private float sx;
	private float sy;

	public ColorRow(ColorSetting setting, GuiContext ctx) {
		super(setting, ctx);
	}

	@Override
	public float preferredHeight() {
		return this.setting.description().isEmpty() ? 46f : 56f;
	}

	@Override
	protected boolean clickable() {
		return true;
	}

	@Override
	protected void drawControl(GuiGraphics g, float mx, float my) {
		float cy = this.y + this.h * 0.5f;
		String label = this.setting.mode() == ColorSetting.Mode.STATIC ? Colors.toHex(this.setting.base(), this.setting.allowsAlpha() && Colors.alpha(this.setting.base()) < 255)
			: EnumSetting.label(this.setting.mode());
		float lw = Fonts.MEDIUM.width(label, 11.5f);
		this.drawLabel(g, cy, this.w - lw - 80f - PAD * 2f);
		this.sx = this.x + this.w - PAD - 36f;
		this.sy = cy - 13f;
		Text.drawMiddle(g, Fonts.MEDIUM, label, this.sx - 10f, cy, 11.5f, Theme.TEXT_MUTED, Text.Align.RIGHT);
		float hv = this.hover.get();
		Draw.rect(g, this.sx, this.sy, 36f, 26f).radius(8f).color(0xFF2A2A2E).submit();
		Draw.rect(g, this.sx, this.sy, 36f, 26f).radius(8f).color(this.setting.color())
			.border(1f, Colors.lerp(0x26FFFFFF, 0x80FFFFFF, hv)).submit();
	}

	@Override
	protected boolean clicked(float mx, float my, int button) {
		if (button == 0 && this.contains(mx, my)) {
			ColorPickerPopover pop = new ColorPickerPopover(new ColorPickerPopover.Target() {
				@Override
				public int base() {
					return ColorRow.this.setting.base();
				}

				@Override
				public void set(int argb) {
					ColorRow.this.setting.set(argb);
				}

				@Override
				public boolean alpha() {
					return ColorRow.this.setting.allowsAlpha();
				}

				@Override
				public ColorSetting.Mode mode() {
					return ColorRow.this.setting.mode();
				}

				@Override
				public void mode(ColorSetting.Mode mode) {
					ColorRow.this.setting.mode(mode);
				}

				@Override
				public float speed() {
					return ColorRow.this.setting.speed();
				}

				@Override
				public void speed(float speed) {
					ColorRow.this.setting.speed(speed);
				}
			});
			pop.place(this.sx, this.sy, 36f, 26f, UiScale.width(), UiScale.height());
			this.ctx.openPopover(pop);
			UiSounds.soft();
			return true;
		}
		return false;
	}
}
