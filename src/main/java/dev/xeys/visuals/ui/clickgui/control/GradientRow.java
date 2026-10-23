package dev.xeys.visuals.ui.clickgui.control;

import dev.xeys.visuals.core.setting.GradientSetting;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.ui.clickgui.GuiContext;
import dev.xeys.visuals.ui.core.UiScale;
import dev.xeys.visuals.ui.core.UiSounds;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.gui.GuiGraphics;

/** Gradient preview bar with a swatch for each stop. */
public final class GradientRow extends SettingRow<GradientSetting> {
	private float barX;
	private float barW;
	private float cy;

	public GradientRow(GradientSetting setting, GuiContext ctx) {
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
		this.cy = this.y + this.h * 0.5f;
		this.barW = 150f;
		this.drawLabel(g, this.cy, this.w - this.barW - PAD * 3f);
		this.barX = this.x + this.w - PAD - this.barW;
		int segments = 12;
		float sw = this.barW / segments;
		for (int i = 0; i < segments; i++) {
			var e = Draw.rect(g, this.barX + i * sw - (i > 0 ? 0.5f : 0f), this.cy - 11f, sw + 1f, 22f)
				.gradientH(this.setting.at(i / (float) segments), this.setting.at((i + 1) / (float) segments));
			if (i == 0) {
				e.radii(8f, 0f, 0f, 8f);
			} else if (i == segments - 1) {
				e.radii(0f, 8f, 8f, 0f);
			}
			e.submit();
		}
		Draw.outline(g, this.barX, this.cy - 11f, this.barW, 22f, 8f, 1f, 0x26FFFFFF);
		for (int end = 0; end < 2; end++) {
			float kx = end == 0 ? this.barX + 11f : this.barX + this.barW - 11f;
			boolean hov = Math.abs(mx - kx) < 10f && Math.abs(my - this.cy) < 10f;
			Draw.circleShape(g, kx, this.cy, hov ? 7.5f : 6.5f).color(end == 0 ? this.setting.from() : this.setting.to())
				.border(2f, Colors.withAlpha(0xFFFFFF, hov ? 1f : 0.85f)).submit();
		}
	}

	@Override
	protected boolean clicked(float mx, float my, int button) {
		if (button != 0 || !this.contains(mx, my) || mx < this.barX) {
			return this.contains(mx, my);
		}
		boolean first = mx < this.barX + this.barW * 0.5f;
		ColorPickerPopover pop = new ColorPickerPopover(new ColorPickerPopover.Target() {
			@Override
			public int base() {
				return first ? GradientRow.this.setting.from() : GradientRow.this.setting.to();
			}

			@Override
			public void set(int argb) {
				if (first) {
					GradientRow.this.setting.from(argb);
				} else {
					GradientRow.this.setting.to(argb);
				}
			}

			@Override
			public boolean alpha() {
				return true;
			}
		});
		pop.place(first ? this.barX : this.barX + this.barW - 22f, this.cy - 11f, 22f, 22f, UiScale.width(), UiScale.height());
		this.ctx.openPopover(pop);
		UiSounds.soft();
		return true;
	}
}
