package dev.xeys.visuals.ui.clickgui.control;

import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.ui.core.UiSounds;
import net.minecraft.client.gui.GuiGraphics;

final class DropdownPopover<E extends Enum<E>> extends Popover {
	private static final float ITEM = 32f;
	private final EnumSetting<E> setting;
	private final String[] labels;

	DropdownPopover(EnumSetting<E> setting, String[] labels, float width) {
		this.setting = setting;
		this.labels = labels;
		this.w = width;
		this.h = labels.length * ITEM + 12f;
	}

	@Override
	protected void drawContent(GuiGraphics g, float mx, float my, float a) {
		int selected = this.setting.get().ordinal();
		for (int i = 0; i < this.labels.length; i++) {
			float iy = this.y + 6f + i * ITEM;
			boolean hov = mx >= this.x + 6f && mx < this.x + this.w - 6f && my >= iy && my < iy + ITEM;
			int bg = EnumRow.itemBackground(i == selected, hov);
			if (bg != 0) {
				Draw.rect(g, this.x + 6f, iy, this.w - 12f, ITEM, 8f, dim(bg, a));
			}
			Text.drawMiddle(g, Fonts.MEDIUM, this.labels[i], this.x + 18f, iy + ITEM * 0.5f, 12.5f, dim(EnumRow.itemColor(i == selected, hov), a), Text.Align.LEFT);
			if (i == selected) {
				Text.icon(g, Icons.CHECK, this.x + this.w - 22f, iy + ITEM * 0.5f, 13f, dim(EnumRow.itemColor(true, false), a));
			}
		}
	}

	@Override
	public boolean mouseClicked(float mx, float my, int button) {
		if (!this.contains(mx, my)) {
			return false;
		}
		int i = (int) ((my - this.y - 6f) / ITEM);
		if (i >= 0 && i < this.labels.length) {
			this.setting.set(this.setting.options()[i]);
			UiSounds.soft();
			this.close();
		}
		return true;
	}
}
