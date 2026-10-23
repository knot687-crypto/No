package dev.xeys.visuals.ui.clickgui.control;

import com.mojang.blaze3d.platform.InputConstants;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.setting.KeybindSetting;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.clickgui.GuiContext;
import dev.xeys.visuals.ui.core.UiSounds;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.gui.GuiGraphics;

/** Click, then press a key or mouse button. Esc cancels, Backspace/Delete unbinds. */
public final class KeybindRow extends SettingRow<KeybindSetting> {
	private boolean listening;
	private float bx;
	private float bw;

	public KeybindRow(KeybindSetting setting, GuiContext ctx) {
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
		String label = this.listening ? "Press a key…" : this.setting.displayName();
		this.bw = Math.max(96f, Fonts.SEMIBOLD.width(label, 12f) + 28f);
		this.drawLabel(g, cy, this.w - this.bw - PAD * 3f);
		this.bx = this.x + this.w - PAD - this.bw;
		float pulse = this.listening ? (float) (0.5 + 0.5 * Math.sin(Clock.time() * 6.0)) : 0f;
		int border = this.listening ? Colors.withAlpha(Theme.accent(), 0.5f + 0.4f * pulse) : 0x1AFFFFFF;
		if (this.listening) {
			Draw.glow(g, this.bx, cy - 15f, this.bw, 30f, 9f, 14f, -2f, Colors.withAlpha(Theme.accent(), 0.25f * pulse));
		}
		Draw.rect(g, this.bx, cy - 15f, this.bw, 30f).radius(9f).color(0xFF141418).border(1f, border).submit();
		int c = this.listening ? Theme.accentText() : (this.setting.isBound() ? Theme.TEXT : Theme.TEXT_MUTED);
		Text.drawMiddle(g, Fonts.SEMIBOLD, label, this.bx + this.bw * 0.5f, cy, 12f, c, Text.Align.CENTER);
	}

	@Override
	protected boolean clicked(float mx, float my, int button) {
		if (!this.contains(mx, my)) {
			return false;
		}
		if (!this.listening && button == 0 && mx >= this.bx) {
			this.listening = true;
			UiSounds.soft();
			this.ctx.captureInput(new GuiContext.InputCapture() {
				@Override
				public boolean onKey(int key) {
					if (key == InputConstants.KEY_ESCAPE) {
						KeybindRow.this.stop();
					} else if (key == InputConstants.KEY_BACKSPACE || key == InputConstants.KEY_DELETE) {
						KeybindRow.this.setting.set(KeybindSetting.NONE);
						KeybindRow.this.stop();
					} else {
						KeybindRow.this.setting.set(key);
						KeybindRow.this.stop();
					}
					return true;
				}

				@Override
				public boolean onMouse(int b) {
					if (b == 0) {
						KeybindRow.this.stop();
						return true;
					}
					KeybindRow.this.setting.set(KeybindSetting.MOUSE_BASE - b);
					KeybindRow.this.stop();
					return true;
				}
			});
			return true;
		}
		return true;
	}

	private void stop() {
		this.listening = false;
		this.ctx.captureInput(null);
		UiSounds.soft();
	}
}
