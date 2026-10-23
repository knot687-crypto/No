package dev.xeys.visuals.ui.clickgui.control;

import dev.xeys.visuals.anim.Spring;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.ui.clickgui.GuiContext;
import dev.xeys.visuals.ui.core.Switch;
import dev.xeys.visuals.ui.core.UiSounds;
import net.minecraft.client.gui.GuiGraphics;

public final class ToggleRow extends SettingRow<BoolSetting> {
	private final Spring knob;

	public ToggleRow(BoolSetting setting, GuiContext ctx) {
		super(setting, ctx);
		this.knob = new Spring(setting.on() ? 1f : 0f, 380f, 0.78f);
	}

	@Override
	public float preferredHeight() {
		return this.setting.description().isEmpty() ? 44f : 54f;
	}

	@Override
	protected boolean clickable() {
		return true;
	}

	@Override
	protected void drawControl(GuiGraphics g, float mx, float my) {
		this.knob.target(this.setting.on() ? 1f : 0f);
		float cy = this.y + this.h * 0.5f;
		this.drawLabel(g, cy, this.w - PAD * 3f - Switch.WIDTH);
		Switch.draw(g, this.x + this.w - PAD - Switch.WIDTH, cy - Switch.HEIGHT * 0.5f, this.knob.get(), this.hover.get(), 1f);
	}

	@Override
	protected boolean clicked(float mx, float my, int button) {
		if (this.contains(mx, my) && button == 0) {
			this.setting.toggle();
			UiSounds.toggle(this.setting.on());
			return true;
		}
		return false;
	}
}
