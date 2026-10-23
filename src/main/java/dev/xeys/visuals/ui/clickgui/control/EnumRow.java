package dev.xeys.visuals.ui.clickgui.control;

import dev.xeys.visuals.anim.Spring;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.ui.clickgui.GuiContext;
import dev.xeys.visuals.ui.core.UiScale;
import dev.xeys.visuals.ui.core.UiSounds;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.gui.GuiGraphics;

/** Segmented control when the options fit, otherwise a dropdown button. */
public final class EnumRow<E extends Enum<E>> extends SettingRow<EnumSetting<E>> {
	private static final float SEG_TEXT = 12f;
	private final Spring slide;
	private final Spring slideW;
	private final String[] labels;
	private float segX;
	private float segW;
	private final float[] cellX;
	private final float[] cellW;
	private float buttonX;
	private float buttonY;
	private float buttonW;

	public EnumRow(EnumSetting<E> setting, GuiContext ctx) {
		super(setting, ctx);
		E[] options = setting.options();
		this.labels = new String[options.length];
		for (int i = 0; i < options.length; i++) {
			this.labels[i] = EnumSetting.label(options[i]);
		}
		this.cellX = new float[options.length];
		this.cellW = new float[options.length];
		this.slide = new Spring(0f, 380f, 0.9f);
		this.slideW = new Spring(0f, 380f, 0.9f);
	}

	@Override
	public float preferredHeight() {
		return this.setting.description().isEmpty() ? 46f : 56f;
	}

	private float segmentedWidth() {
		float total = 6f;
		for (String l : this.labels) {
			total += Fonts.MEDIUM.width(l, SEG_TEXT) + 22f;
		}
		return total;
	}

	private boolean segmented() {
		return this.labels.length <= 4 && this.segmentedWidth() <= this.w * 0.58f;
	}

	@Override
	protected boolean clickable() {
		return true;
	}

	@Override
	protected void drawControl(GuiGraphics g, float mx, float my) {
		float cy = this.y + this.h * 0.5f;
		if (this.segmented()) {
			float total = this.segmentedWidth();
			this.drawLabel(g, cy, this.w - total - PAD * 3f);
			this.segW = total;
			this.segX = this.x + this.w - PAD - total;
			float sh = 30f;
			float sy = cy - sh * 0.5f;
			Draw.rect(g, this.segX, sy, total, sh).radius(9f).color(0xFF141418).border(1f, 0x12FFFFFF).submit();
			float cx = this.segX + 3f;
			int selected = this.setting.get().ordinal();
			for (int i = 0; i < this.labels.length; i++) {
				this.cellW[i] = Fonts.MEDIUM.width(this.labels[i], SEG_TEXT) + 22f;
				this.cellX[i] = cx;
				cx += this.cellW[i];
			}
			if (this.slideW.target() == 0f) {
				this.slide.snap(this.cellX[selected] - this.segX);
				this.slideW.snap(this.cellW[selected]);
			}
			this.slide.target(this.cellX[selected] - this.segX);
			this.slideW.target(this.cellW[selected]);
			float px = this.segX + this.slide.get();
			Draw.shadow(g, px, sy + 3f, this.slideW.get(), sh - 6f, 7f, 0f, 1f, 4f, 0f, 0x66000000);
			Draw.rect(g, px, sy + 3f, this.slideW.get(), sh - 6f).radius(7f).gradientV(0xFF2A2A31, 0xFF222228).border(1f, 0x1AFFFFFF).submit();
			for (int i = 0; i < this.labels.length; i++) {
				boolean sel = i == selected;
				boolean hov = mx >= this.cellX[i] && mx < this.cellX[i] + this.cellW[i] && Math.abs(my - cy) < sh * 0.5f;
				int c = sel ? Theme.TEXT : (hov ? Theme.TEXT_SECONDARY : Theme.TEXT_MUTED);
				Text.drawMiddle(g, Fonts.MEDIUM, this.labels[i], this.cellX[i] + this.cellW[i] * 0.5f, cy, SEG_TEXT, c, Text.Align.CENTER);
			}
		} else {
			String current = this.labels[this.setting.get().ordinal()];
			this.buttonW = Math.max(120f, Fonts.MEDIUM.width(current, 12.5f) + 46f);
			this.drawLabel(g, cy, this.w - this.buttonW - PAD * 3f);
			this.buttonX = this.x + this.w - PAD - this.buttonW;
			this.buttonY = cy - 16f;
			boolean hov = mx >= this.buttonX && mx < this.buttonX + this.buttonW && Math.abs(my - cy) < 16f;
			Draw.rect(g, this.buttonX, this.buttonY, this.buttonW, 32f).radius(9f).color(hov ? 0xFF1B1B20 : 0xFF141418).border(1f, hov ? 0x26FFFFFF : 0x12FFFFFF).submit();
			Text.drawMiddle(g, Fonts.MEDIUM, current, this.buttonX + 14f, cy, 12.5f, Theme.TEXT, Text.Align.LEFT);
			Text.icon(g, Icons.CHEVRON_DOWN, this.buttonX + this.buttonW - 16f, cy, 13f, Theme.TEXT_MUTED);
		}
	}

	@Override
	protected boolean clicked(float mx, float my, int button) {
		if (button != 0 || !this.contains(mx, my)) {
			return false;
		}
		if (this.segmented()) {
			for (int i = 0; i < this.labels.length; i++) {
				if (mx >= this.cellX[i] && mx < this.cellX[i] + this.cellW[i]) {
					if (this.setting.get().ordinal() != i) {
						this.setting.set(this.setting.options()[i]);
						UiSounds.soft();
					}
					return true;
				}
			}
			return true;
		}
		if (mx >= this.buttonX && mx < this.buttonX + this.buttonW) {
			DropdownPopover<E> pop = new DropdownPopover<>(this.setting, this.labels, Math.max(this.buttonW, 170f));
			pop.place(this.buttonX, this.buttonY, this.buttonW, 32f, UiScale.width(), UiScale.height());
			this.ctx.openPopover(pop);
			UiSounds.soft();
		}
		return true;
	}

	/** Scrolling over the control steps through options. */
	@Override
	public boolean mouseScrolled(float mx, float my, float amount) {
		if (this.contains(mx, my) && mx > this.x + this.w * 0.45f) {
			this.setting.cycle(amount > 0 ? -1 : 1);
			return true;
		}
		return false;
	}

	static int itemColor(boolean selected, boolean hovered) {
		return selected ? Theme.accentText() : (hovered ? Theme.TEXT : Theme.TEXT_SECONDARY);
	}

	static int itemBackground(boolean selected, boolean hovered) {
		return selected ? Theme.accentSoft() : (hovered ? Colors.withAlpha(0xFFFFFF, 0.05f) : 0);
	}
}
