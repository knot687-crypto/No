package dev.xeys.visuals.ui.clickgui;

import dev.xeys.visuals.anim.Anim;
import dev.xeys.visuals.anim.Easing;
import dev.xeys.visuals.anim.Spring;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.ActionSetting;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.core.setting.GradientSetting;
import dev.xeys.visuals.core.setting.KeybindSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.core.setting.Setting;
import dev.xeys.visuals.core.setting.TextSetting;
import dev.xeys.visuals.hud.HudModule;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.ui.clickgui.control.ActionRow;
import dev.xeys.visuals.ui.clickgui.control.ColorRow;
import dev.xeys.visuals.ui.clickgui.control.EnumRow;
import dev.xeys.visuals.ui.clickgui.control.GradientRow;
import dev.xeys.visuals.ui.clickgui.control.KeybindRow;
import dev.xeys.visuals.ui.clickgui.control.SettingRow;
import dev.xeys.visuals.ui.clickgui.control.SliderRow;
import dev.xeys.visuals.ui.clickgui.control.TextRow;
import dev.xeys.visuals.ui.clickgui.control.ToggleRow;
import dev.xeys.visuals.ui.core.ScrollState;
import dev.xeys.visuals.ui.core.Switch;
import dev.xeys.visuals.ui.core.UiSounds;
import dev.xeys.visuals.util.Colors;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import org.jspecify.annotations.Nullable;

/** A module's settings, grouped into sections, sliding in over the module grid. */
final class SettingsPanel {
	private static final float HEADER = 92f;
	private static final float SECTION = 34f;

	final Module module;
	private final List<SettingRow<?>> rows = new ArrayList<>();
	private final List<Spring> rowVisibility = new ArrayList<>();
	private final ScrollState scroll = new ScrollState();
	final Anim open = new Anim(0f);
	private final Spring enabledSwitch;
	private float x;
	private float y;
	private float w;
	private float h;
	private boolean closing;
	private boolean backHovered;
	private float switchX;
	private float switchY;

	SettingsPanel(Module module, GuiContext ctx) {
		this.module = module;
		this.enabledSwitch = new Spring(module.isEnabled() ? 1f : 0f, 380f, 0.78f);
		for (Setting<?> s : module.settings()) {
			SettingRow<?> row = createRow(s, ctx);
			if (row != null) {
				this.rows.add(row);
			}
		}
		if (module instanceof HudModule hud) {
			NumberSetting scale = new NumberSetting("hud_scale", "Scale", hud.position.scale(), 0.4f, 3f, 0.05f).unit("x")
				.describe("Size of this element (also adjustable in the HUD editor)").group("Layout");
			scale.onChange(hud.position::setScale);
			this.rows.add(new SliderRow(scale, ctx));
			this.rows.add(new ActionRow(new ActionSetting("hud_reset", "Position", "Reset", hud.position::reset).group("Layout")
				.describe("Move back to the default anchor"), ctx));
		}
		if (!module.isAlwaysOn()) {
			this.rows.add(new KeybindRow(module.keybind.group("Module"), ctx));
		}
		for (int i = 0; i < this.rows.size(); i++) {
			this.rowVisibility.add(new Spring(this.rows.get(i).setting().isVisible() ? 1f : 0f, 300f, 1f));
		}
		this.open.to(1f, 0.32f, Easing.OUT_CUBIC);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static @Nullable SettingRow<?> createRow(Setting<?> s, GuiContext ctx) {
		if (s instanceof BoolSetting b) {
			return new ToggleRow(b, ctx);
		} else if (s instanceof NumberSetting n) {
			return new SliderRow(n, ctx);
		} else if (s instanceof EnumSetting e) {
			return new EnumRow(e, ctx);
		} else if (s instanceof ColorSetting c) {
			return new ColorRow(c, ctx);
		} else if (s instanceof GradientSetting gr) {
			return new GradientRow(gr, ctx);
		} else if (s instanceof KeybindSetting k) {
			return new KeybindRow(k, ctx);
		} else if (s instanceof TextSetting t) {
			return new TextRow(t, ctx);
		} else if (s instanceof ActionSetting a) {
			return new ActionRow(a, ctx);
		}
		return null;
	}

	void close() {
		if (!this.closing) {
			this.closing = true;
			this.open.to(0f, 0.2f, Easing.OUT_CUBIC);
		}
	}

	boolean closed() {
		return this.closing && this.open.get() <= 0.001f;
	}

	boolean closing() {
		return this.closing;
	}

	void bounds(float x, float y, float w, float h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
	}

	void render(GuiGraphics g, float mx, float my) {
		float a = this.open.get();
		if (a <= 0.001f) {
			return;
		}
		float slide = (1f - a) * 36f;
		float x = this.x + slide;

		// Header.
		this.backHovered = mx >= x && mx <= x + 34f && my >= this.y + 16f && my <= this.y + 50f;
		Draw.rect(g, x, this.y + 16f, 34f, 34f).radius(10f).color(Colors.mulAlpha(this.backHovered ? 0xFF22222A : 0xFF18181D, a))
			.border(1f, Colors.mulAlpha(this.backHovered ? 0x26FFFFFF : 0x14FFFFFF, a)).submit();
		Text.icon(g, Icons.CHEVRON_LEFT, x + 17f, this.y + 33f, 16f, Colors.mulAlpha(this.backHovered ? Theme.TEXT : Theme.TEXT_SECONDARY, a));
		float en = this.enabledSwitch.get();
		float tileX = x + 48f;
		Draw.rect(g, tileX, this.y + 12f, 42f, 42f).radius(12f)
			.color(Colors.mulAlpha(Colors.lerpOklab(0xFF1D1D23, Colors.lerpOklab(0xFF16161B, Theme.accent(), 0.25f), en), a))
			.border(1f, Colors.mulAlpha(Colors.lerpOklab(0x14FFFFFF, Colors.withAlpha(Theme.accent(), 0.45f), en), a)).submit();
		Text.icon(g, this.module.icon(), tileX + 21f, this.y + 33f, 19f, Colors.mulAlpha(Colors.lerpOklab(Theme.TEXT_SECONDARY, Theme.accent(), en), a));
		Text.drawMiddle(g, Fonts.SEMIBOLD, this.module.name(), tileX + 56f, this.y + 25f, 18f, Colors.mulAlpha(Theme.TEXT, a), Text.Align.LEFT);
		String sub = this.module.category().title() + "  ·  " + this.module.description();
		Text.drawTruncated(g, Fonts.REGULAR, sub, tileX + 56f, this.y + 44f, 11.5f, Colors.mulAlpha(Theme.TEXT_MUTED, a), this.w - 260f);
		if (!this.module.isAlwaysOn()) {
			this.enabledSwitch.target(this.module.isEnabled() ? 1f : 0f);
			this.switchX = x + this.w - Switch.WIDTH;
			this.switchY = this.y + 33f - Switch.HEIGHT * 0.5f;
			String state = this.module.isEnabled() ? "Enabled" : "Disabled";
			Text.drawMiddle(g, Fonts.MEDIUM, state, this.switchX - 12f, this.y + 33f, 12f,
				Colors.mulAlpha(this.module.isEnabled() ? Theme.accentText() : Theme.TEXT_MUTED, a), Text.Align.RIGHT);
			Switch.draw(g, this.switchX, this.switchY, en, 0f, a);
		}
		Draw.rect(g, x, this.y + HEADER - 12f, this.w, 1f).color(Colors.mulAlpha(Theme.HAIRLINE, a)).submit();

		// Rows.
		float top = this.y + HEADER;
		float viewH = this.h - HEADER;
		float content = this.layoutRows(x, top);
		this.scroll.setExtent(viewH, content + 16f);
		Draw.pushClip(g, x - 8f, top - 4f, x + this.w + 8f, this.y + this.h, 22f);
		String group = null;
		float off = this.scroll.offset();
		float cy = top - off;
		for (int i = 0; i < this.rows.size(); i++) {
			SettingRow<?> row = this.rows.get(i);
			float vis = this.rowVisibility.get(i).get();
			if (vis <= 0.01f) {
				continue;
			}
			String rg = row.setting().group();
			if (rg != null && !rg.equals(group)) {
				group = rg;
				Text.drawMiddle(g, Fonts.SEMIBOLD, rg.toUpperCase(Locale.ROOT), x + 16f, cy + SECTION * 0.62f, 10f,
					Colors.mulAlpha(Theme.TEXT_FAINT, a * vis), Text.Align.LEFT, 0.12f);
				cy += SECTION;
			}
			float rh = row.preferredHeight() * vis;
			row.bounds(x, cy, this.w, row.preferredHeight());
			if (cy + rh > top - 40f && cy < this.y + this.h + 40f) {
				g.pose().pushMatrix();
				if (vis < 1f) {
					g.pose().translate(0f, -(1f - vis) * 6f);
				}
				row.visible = vis > 0.5f;
				row.render(g, mx, my);
				row.updateHover(mx, my);
				g.pose().popMatrix();
			}
			cy += rh;
		}
		Draw.popClip();
		this.scroll.drawBar(g, x + this.w + 6f, top, mx > x + this.w - 20f && mx < x + this.w + 12f && my > top);
	}

	private float layoutRows(float x, float top) {
		float total = 0f;
		String group = null;
		for (int i = 0; i < this.rows.size(); i++) {
			SettingRow<?> row = this.rows.get(i);
			Spring vis = this.rowVisibility.get(i);
			vis.target(row.setting().isVisible() ? 1f : 0f);
			if (vis.get() <= 0.01f && !row.setting().isVisible()) {
				continue;
			}
			String rg = row.setting().group();
			if (rg != null && !rg.equals(group)) {
				group = rg;
				total += SECTION;
			}
			total += row.preferredHeight() * vis.get();
		}
		return total;
	}

	private boolean inRows(float mx, float my) {
		return mx >= this.x && mx <= this.x + this.w && my >= this.y + HEADER - 4f && my <= this.y + this.h;
	}

	boolean mouseClicked(float mx, float my, int button) {
		if (this.backHovered && button == 0) {
			UiSounds.click();
			this.close();
			return true;
		}
		if (!this.module.isAlwaysOn() && mx >= this.switchX - 70f && mx <= this.switchX + Switch.WIDTH + 6f
			&& my >= this.switchY - 8f && my <= this.switchY + Switch.HEIGHT + 8f) {
			this.module.toggle();
			UiSounds.toggle(this.module.isEnabled());
			return true;
		}
		if (this.inRows(mx, my)) {
			for (SettingRow<?> row : this.rows) {
				if (row.visible && row.setting().isVisible() && row.mouseClicked(mx, my, button)) {
					this.focusOnly(row);
					return true;
				}
			}
			this.focusOnly(null);
			return true;
		}
		return false;
	}

	private void focusOnly(@Nullable SettingRow<?> focused) {
		for (SettingRow<?> row : this.rows) {
			if (row instanceof TextRow t) {
				t.field().setFocused(row == focused && t.field().isHovered());
			}
		}
	}

	boolean mouseReleased(float mx, float my, int button) {
		boolean handled = false;
		for (SettingRow<?> row : this.rows) {
			handled |= row.mouseReleased(mx, my, button);
		}
		return handled;
	}

	boolean mouseDragged(float mx, float my, int button, float dx, float dy) {
		for (SettingRow<?> row : this.rows) {
			if (row.mouseDragged(mx, my, button, dx, dy)) {
				return true;
			}
		}
		return false;
	}

	boolean mouseScrolled(float mx, float my, float amount) {
		if (!this.inRows(mx, my)) {
			return false;
		}
		for (SettingRow<?> row : this.rows) {
			if (row.visible && row.mouseScrolled(mx, my, amount)) {
				return true;
			}
		}
		this.scroll.scroll(amount);
		return true;
	}

	boolean keyPressed(int key, int scancode, int modifiers) {
		for (SettingRow<?> row : this.rows) {
			if (row.keyPressed(key, scancode, modifiers)) {
				return true;
			}
		}
		return false;
	}

	boolean charTyped(int codepoint, int modifiers) {
		for (SettingRow<?> row : this.rows) {
			if (row.charTyped(codepoint, modifiers)) {
				return true;
			}
		}
		return false;
	}
}
