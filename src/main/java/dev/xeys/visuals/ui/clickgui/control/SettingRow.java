package dev.xeys.visuals.ui.clickgui.control;

import dev.xeys.visuals.core.setting.Setting;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.ui.clickgui.GuiContext;
import dev.xeys.visuals.ui.core.UiSounds;
import dev.xeys.visuals.ui.core.Widget;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.gui.GuiGraphics;

/** One setting in the settings panel: label + description on the left, control on the right. */
public abstract class SettingRow<S extends Setting<?>> extends Widget {
	public static final float PAD = 16f;
	protected final S setting;
	protected final GuiContext ctx;
	private boolean resetHovered;

	protected SettingRow(S setting, GuiContext ctx) {
		this.setting = setting;
		this.ctx = ctx;
	}

	public S setting() {
		return this.setting;
	}

	public abstract float preferredHeight();

	@Override
	protected boolean clickable() {
		return false;
	}

	@Override
	protected void draw(GuiGraphics g, float mx, float my) {
		float hv = this.hover.get();
		if (hv > 0.01f) {
			Draw.rect(g, this.x, this.y, this.w, this.h, 12f, Colors.withAlpha(0xFFFFFF, 0.025f * hv));
		}
		this.drawControl(g, mx, my);
	}

	/** Draws name and description in the label column, top-aligned for tall rows. */
	protected void drawLabel(GuiGraphics g, float centerY, float maxWidth) {
		String desc = this.setting.description();
		boolean showReset = !this.setting.isDefault() && this.setting.key() != null;
		float nameW = Fonts.MEDIUM.width(this.setting.name(), 13f);
		if (desc.isEmpty()) {
			Text.drawMiddle(g, Fonts.MEDIUM, this.setting.name(), this.x + PAD, centerY, 13f, Theme.TEXT, Text.Align.LEFT);
		} else {
			Text.drawMiddle(g, Fonts.MEDIUM, this.setting.name(), this.x + PAD, centerY - 8f, 13f, Theme.TEXT, Text.Align.LEFT);
			Text.drawTruncated(g, Fonts.REGULAR, desc, this.x + PAD, centerY + 9f, 11f, Theme.TEXT_MUTED, maxWidth);
		}
		if (showReset && this.hover.get() > 0.01f) {
			float rx = this.x + PAD + nameW + 12f;
			float ry = desc.isEmpty() ? centerY : centerY - 8f;
			this.resetX = rx;
			this.resetY = ry;
			int c = this.resetHovered ? Theme.accent() : Theme.TEXT_FAINT;
			Text.icon(g, Icons.ROTATE_CCW, rx, ry, 11f, Colors.mulAlpha(c, this.hover.get()));
		} else {
			this.resetX = Float.NaN;
		}
	}

	private float resetX = Float.NaN;
	private float resetY;

	protected boolean onResetIcon(float mx, float my) {
		return !Float.isNaN(this.resetX) && Math.abs(mx - this.resetX) < 9f && Math.abs(my - this.resetY) < 9f;
	}

	@Override
	public boolean mouseClicked(float mx, float my, int button) {
		this.resetHovered = false;
		if (this.visible && this.contains(mx, my) && button == 0 && this.onResetIcon(mx, my)) {
			this.setting.reset();
			UiSounds.soft();
			return true;
		}
		return this.clicked(mx, my, button);
	}

	protected abstract void drawControl(GuiGraphics g, float mx, float my);

	protected boolean clicked(float mx, float my, int button) {
		return false;
	}

	public void updateHover(float mx, float my) {
		this.resetHovered = this.onResetIcon(mx, my);
	}
}
