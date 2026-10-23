package dev.xeys.visuals.ui.clickgui.control;

import dev.xeys.visuals.core.setting.TextSetting;
import dev.xeys.visuals.ui.clickgui.GuiContext;
import dev.xeys.visuals.ui.core.TextField;
import net.minecraft.client.gui.GuiGraphics;

public final class TextRow extends SettingRow<TextSetting> {
	private final TextField field;

	public TextRow(TextSetting setting, GuiContext ctx) {
		super(setting, ctx);
		this.field = new TextField().maxLength(setting.maxLength()).textSize(12.5f).onChange(setting::set);
		this.field.setValue(setting.get());
	}

	public TextField field() {
		return this.field;
	}

	@Override
	public float preferredHeight() {
		return this.setting.description().isEmpty() ? 48f : 58f;
	}

	@Override
	protected void drawControl(GuiGraphics g, float mx, float my) {
		float cy = this.y + this.h * 0.5f;
		float fw = Math.min(220f, this.w * 0.48f);
		this.drawLabel(g, cy, this.w - fw - PAD * 3f);
		if (!this.field.isFocused() && !this.field.value().equals(this.setting.get())) {
			this.field.setValue(this.setting.get());
		}
		this.field.bounds(this.x + this.w - PAD - fw, cy - 16f, fw, 32f);
		this.field.render(g, mx, my);
	}

	@Override
	protected boolean clicked(float mx, float my, int button) {
		return this.field.mouseClicked(mx, my, button);
	}

	@Override
	public boolean mouseReleased(float mx, float my, int button) {
		return this.field.mouseReleased(mx, my, button);
	}

	@Override
	public boolean mouseDragged(float mx, float my, int button, float dx, float dy) {
		return this.field.mouseDragged(mx, my, button, dx, dy);
	}

	@Override
	public boolean keyPressed(int key, int scancode, int modifiers) {
		return this.field.keyPressed(key, scancode, modifiers);
	}

	@Override
	public boolean charTyped(int codepoint, int modifiers) {
		return this.field.charTyped(codepoint, modifiers);
	}
}
