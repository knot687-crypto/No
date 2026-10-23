package dev.xeys.visuals.ui.core;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.render.text.XeysFont;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.util.Colors;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Single-line text input: caret, selection, clipboard, word jumps, horizontal scrolling. */
public class TextField extends Widget {
	private final StringBuilder text = new StringBuilder();
	private String placeholder = "";
	private char icon;
	private int caret;
	private int anchor;
	private float scrollX;
	private int maxLength = 64;
	private float textSize = 13f;
	private XeysFont font = Fonts.MEDIUM;
	private IntPredicate filter = c -> c >= 32 && c != 127;
	private Consumer<String> onChange = s -> {
	};
	private Runnable onSubmit = () -> {
	};
	private double lastEdit;
	private String cachedString = "";
	private boolean stringDirty = true;

	public TextField placeholder(String placeholder) {
		this.placeholder = placeholder;
		return this;
	}

	public TextField icon(char icon) {
		this.icon = icon;
		return this;
	}

	public TextField maxLength(int maxLength) {
		this.maxLength = maxLength;
		return this;
	}

	public TextField textSize(float size) {
		this.textSize = size;
		return this;
	}

	public TextField filter(IntPredicate filter) {
		this.filter = filter;
		return this;
	}

	public TextField onChange(Consumer<String> onChange) {
		this.onChange = onChange;
		return this;
	}

	public TextField onSubmit(Runnable onSubmit) {
		this.onSubmit = onSubmit;
		return this;
	}

	public String value() {
		if (this.stringDirty) {
			this.cachedString = this.text.toString();
			this.stringDirty = false;
		}
		return this.cachedString;
	}

	public void setValue(String value) {
		this.text.setLength(0);
		this.text.append(value.length() > this.maxLength ? value.substring(0, this.maxLength) : value);
		this.caret = this.anchor = this.text.length();
		this.stringDirty = true;
	}

	@Override
	public boolean focusable() {
		return true;
	}

	@Override
	protected boolean clickable() {
		return false;
	}

	private float textLeft() {
		return this.x + (this.icon != 0 ? 36f : 14f);
	}

	private float textRight() {
		return this.x + this.w - 12f;
	}

	@Override
	protected void draw(GuiGraphics g, float mx, float my) {
		if (this.hovered) {
			g.requestCursor(CursorTypes.IBEAM);
		}
		float hv = this.hover.get();
		float fc = this.focus.get();
		float r = this.h * 0.5f;
		int bg = Colors.lerp(Colors.lerp(0x99000000, 0xB3050506, hv), 0xCC050506, fc);
		int border = Colors.lerpOklab(Colors.lerp(Theme.HAIRLINE_STRONG, 0x33FFFFFF, hv), Colors.withAlpha(Theme.accent(), 0.75f), fc);
		Draw.rect(g, this.x, this.y, this.w, this.h).radius(r).color(bg).border(1f, border).submit();
		if (fc > 0.01f) {
			Draw.glow(g, this.x, this.y, this.w, this.h, r, 14f, -2f, Colors.withAlpha(Theme.accent(), 0.22f * fc));
		}
		float cy = this.y + this.h * 0.5f;
		if (this.icon != 0) {
			Text.icon(g, this.icon, this.x + 20f, cy, 14f, Colors.lerpOklab(Theme.TEXT_MUTED, Theme.accent(), fc));
		}

		float left = this.textLeft();
		float right = this.textRight();
		float avail = right - left;
		String value = this.value();
		float caretX = this.font.width(value, 0, this.caret, this.textSize, 0f);
		if (caretX - this.scrollX > avail) {
			this.scrollX = caretX - avail;
		} else if (caretX - this.scrollX < 0f) {
			this.scrollX = caretX;
		}
		this.scrollX = Math.max(0f, this.scrollX);

		Draw.pushClip(g, left, this.y, right, this.y + this.h, 6f);
		if (value.isEmpty() && !this.focused) {
			Text.drawMiddle(g, this.font, this.placeholder, left, cy, this.textSize, Theme.TEXT_FAINT, Text.Align.LEFT);
		} else {
			if (this.caret != this.anchor) {
				int a = Math.min(this.caret, this.anchor);
				int b = Math.max(this.caret, this.anchor);
				float sx0 = left - this.scrollX + this.font.width(value, 0, a, this.textSize, 0f);
				float sx1 = left - this.scrollX + this.font.width(value, 0, b, this.textSize, 0f);
				Draw.rect(g, sx0, cy - this.textSize * 0.62f, sx1 - sx0, this.textSize * 1.24f, 3f, Theme.accent(0.32f));
			}
			if (value.isEmpty()) {
				Text.drawMiddle(g, this.font, this.placeholder, left, cy, this.textSize, Theme.TEXT_FAINT, Text.Align.LEFT);
			} else {
				Text.drawMiddle(g, this.font, value, left - this.scrollX, cy, this.textSize, Theme.TEXT, Text.Align.LEFT);
			}
		}
		if (this.focused) {
			// Caret: solid while typing, then a soft blink.
			double since = Clock.time() - this.lastEdit;
			float blink = since < 0.5 ? 1f : (float) (0.5 + 0.5 * Math.cos((since - 0.5) * Math.PI * 2.0 / 1.05));
			float cxp = left - this.scrollX + caretX;
			Draw.rect(g, cxp - 0.6f, cy - this.textSize * 0.6f, 1.3f, this.textSize * 1.2f).color(Theme.accent(Math.max(0f, blink))).submit();
		}
		Draw.popClip();
	}

	private int indexAt(float mx) {
		String value = this.value();
		float local = mx - this.textLeft() + this.scrollX;
		int best = 0;
		float bestDist = Float.MAX_VALUE;
		for (int i = 0; i <= value.length(); i++) {
			float d = Math.abs(this.font.width(value, 0, i, this.textSize, 0f) - local);
			if (d < bestDist) {
				bestDist = d;
				best = i;
			}
		}
		return best;
	}

	@Override
	public boolean mouseClicked(float mx, float my, int button) {
		if (this.visible && this.enabled && this.contains(mx, my)) {
			this.pressed = true;
			this.caret = this.indexAt(mx);
			if (!InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), InputConstants.KEY_LSHIFT)) {
				this.anchor = this.caret;
			}
			this.lastEdit = Clock.time();
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(float mx, float my, int button, float dx, float dy) {
		if (this.pressed) {
			this.caret = this.indexAt(mx);
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(float mx, float my, int button) {
		boolean was = this.pressed;
		this.pressed = false;
		return was;
	}

	@Override
	public boolean charTyped(int codepoint, int modifiers) {
		if (!this.focused || !this.filter.test(codepoint)) {
			return false;
		}
		this.insert(new String(Character.toChars(codepoint)));
		return true;
	}

	private void insert(String s) {
		int a = Math.min(this.caret, this.anchor);
		int b = Math.max(this.caret, this.anchor);
		this.text.delete(a, b);
		StringBuilder clean = new StringBuilder();
		s.codePoints().filter(this.filter).forEach(clean::appendCodePoint);
		String add = clean.toString();
		int room = this.maxLength - this.text.length();
		if (add.length() > room) {
			add = add.substring(0, Math.max(0, room));
		}
		this.text.insert(a, add);
		this.caret = this.anchor = a + add.length();
		this.changed();
	}

	private void changed() {
		this.stringDirty = true;
		this.lastEdit = Clock.time();
		this.onChange.accept(this.value());
	}

	@Override
	public boolean keyPressed(int key, int scancode, int modifiers) {
		if (!this.focused) {
			return false;
		}
		boolean ctrl = (modifiers & (InputConstants.MOD_CONTROL | InputConstants.MOD_SUPER)) != 0;
		boolean shift = (modifiers & InputConstants.MOD_SHIFT) != 0;
		Minecraft mc = Minecraft.getInstance();
		switch (key) {
			case InputConstants.KEY_BACKSPACE -> {
				if (this.caret != this.anchor) {
					this.insert("");
				} else if (this.caret > 0) {
					int from = ctrl ? this.wordLeft(this.caret) : this.caret - 1;
					this.text.delete(from, this.caret);
					this.caret = this.anchor = from;
					this.changed();
				}
				return true;
			}
			case InputConstants.KEY_DELETE -> {
				if (this.caret != this.anchor) {
					this.insert("");
				} else if (this.caret < this.text.length()) {
					int to = ctrl ? this.wordRight(this.caret) : this.caret + 1;
					this.text.delete(this.caret, to);
					this.changed();
				}
				return true;
			}
			case InputConstants.KEY_LEFT -> {
				this.caret = ctrl ? this.wordLeft(this.caret) : Math.max(0, this.caret - 1);
				if (!shift) {
					this.anchor = this.caret;
				}
				this.lastEdit = Clock.time();
				return true;
			}
			case InputConstants.KEY_RIGHT -> {
				this.caret = ctrl ? this.wordRight(this.caret) : Math.min(this.text.length(), this.caret + 1);
				if (!shift) {
					this.anchor = this.caret;
				}
				this.lastEdit = Clock.time();
				return true;
			}
			case InputConstants.KEY_HOME -> {
				this.caret = 0;
				if (!shift) {
					this.anchor = 0;
				}
				return true;
			}
			case InputConstants.KEY_END -> {
				this.caret = this.text.length();
				if (!shift) {
					this.anchor = this.caret;
				}
				return true;
			}
			case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
				this.onSubmit.run();
				return true;
			}
			default -> {
			}
		}
		if (ctrl) {
			switch (key) {
				case InputConstants.KEY_A -> {
					this.anchor = 0;
					this.caret = this.text.length();
					return true;
				}
				case InputConstants.KEY_C, InputConstants.KEY_X -> {
					if (this.caret != this.anchor) {
						int a = Math.min(this.caret, this.anchor);
						int b = Math.max(this.caret, this.anchor);
						mc.keyboardHandler.setClipboard(this.text.substring(a, b));
						if (key == InputConstants.KEY_X) {
							this.insert("");
						}
					}
					return true;
				}
				case InputConstants.KEY_V -> {
					this.insert(mc.keyboardHandler.getClipboard());
					return true;
				}
				default -> {
				}
			}
		}
		return false;
	}

	private int wordLeft(int from) {
		int i = from;
		while (i > 0 && this.text.charAt(i - 1) == ' ') {
			i--;
		}
		while (i > 0 && this.text.charAt(i - 1) != ' ') {
			i--;
		}
		return i;
	}

	private int wordRight(int from) {
		int i = from;
		int n = this.text.length();
		while (i < n && this.text.charAt(i) != ' ') {
			i++;
		}
		while (i < n && this.text.charAt(i) == ' ') {
			i++;
		}
		return i;
	}
}
