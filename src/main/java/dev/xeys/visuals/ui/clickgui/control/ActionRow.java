package dev.xeys.visuals.ui.clickgui.control;

import dev.xeys.visuals.core.setting.ActionSetting;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.clickgui.GuiContext;
import dev.xeys.visuals.ui.core.UiSounds;
import net.minecraft.client.gui.GuiGraphics;

public final class ActionRow extends SettingRow<ActionSetting> {
	private float bx;
	private float bw;

	public ActionRow(ActionSetting setting, GuiContext ctx) {
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
		this.bw = Fonts.SEMIBOLD.width(this.setting.buttonLabel(), 12f) + 30f;
		this.drawLabel(g, cy, this.w - this.bw - PAD * 3f);
		this.bx = this.x + this.w - PAD - this.bw;
		boolean hov = mx >= this.bx && this.hovered;
		Draw.rect(g, this.bx, cy - 15f, this.bw, 30f).radius(9f).color(hov ? 0xFF24242B : 0xFF1A1A1F).border(1f, hov ? Theme.accent(0.5f) : 0x1AFFFFFF).submit();
		Text.drawMiddle(g, Fonts.SEMIBOLD, this.setting.buttonLabel(), this.bx + this.bw * 0.5f, cy, 12f, hov ? Theme.accentText() : Theme.TEXT, Text.Align.CENTER);
	}

	@Override
	protected boolean clicked(float mx, float my, int button) {
		if (button == 0 && this.contains(mx, my) && mx >= this.bx) {
			UiSounds.click();
			this.setting.run();
			return true;
		}
		return this.contains(mx, my);
	}
}
