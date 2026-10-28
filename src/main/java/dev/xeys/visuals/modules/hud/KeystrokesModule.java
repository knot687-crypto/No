package dev.xeys.visuals.modules.hud;

import dev.xeys.visuals.anim.Anim;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.anim.Easing;
import dev.xeys.visuals.core.input.InputStats;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.hud.HudModule;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.HudStyle;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.render.text.XeysFont;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;

/** Movement keys, mouse buttons and the space bar, lighting up as you press them. */
public final class KeystrokesModule extends HudModule {
	private static final int W = 0;
	private static final int A = 1;
	private static final int S = 2;
	private static final int D = 3;
	private static final int LMB = 4;
	private static final int RMB = 5;
	private static final int SPACE = 6;
	private static final int KEYS = 7;

	private final NumberSetting keySize = this.add(new NumberSetting("key_size", "Key Size", 24f, 16f, 40f, 1f).unit("px").group("Layout"));
	private final NumberSetting gap = this.add(new NumberSetting("gap", "Gap", 2f, 0f, 6f, 0.5f).unit("px").decimals(1).group("Layout"));
	private final BoolSetting mouse = this.add(new BoolSetting("mouse", "Mouse Buttons", true).group("Layout"));
	private final BoolSetting cps = this.add(new BoolSetting("cps", "CPS", true).describe("Clicks per second on the mouse keys")
		.visibleWhen(this.mouse::on).group("Layout"));
	private final BoolSetting space = this.add(new BoolSetting("space", "Space Bar", true).group("Layout"));
	private final ColorSetting pressed = this.add(new ColorSetting("pressed", "Pressed", 0xFFFFFFFF, ColorSetting.Mode.ACCENT).group("Colors"));
	private final NumberSetting fade = this.add(new NumberSetting("fade", "Release Fade", 180f, 0f, 600f, 10f).unit("ms")
		.describe("How long a key takes to dim after release").group("Animation"));
	private final BoolSetting ripple = this.add(new BoolSetting("ripple", "Ripple", true).describe("A soft ring spreads from each press")
		.group("Animation"));
	private final BoolSetting depth = this.add(new BoolSetting("depth", "Press Depth", true).describe("Keys sink slightly when pressed")
		.group("Animation"));

	private final Anim[] press = new Anim[KEYS];
	private final Anim[] rippleAnim = new Anim[KEYS];
	private final boolean[] down = new boolean[KEYS];
	private final String[] labels = {"W", "A", "S", "D", "LMB", "RMB", ""};
	private final StringBuilder leftCps = new StringBuilder(8);
	private final StringBuilder rightCps = new StringBuilder(8);
	private boolean spaceIsSpace = true;
	private double labelsAt = -10.0;

	public KeystrokesModule() {
		super("keystrokes", "Keystrokes", Icons.KEYBOARD, "Your movement keys and clicks, animated", HudPosition.Anchor.TOP_LEFT, 6f, 260f);
		for (int i = 0; i < KEYS; i++) {
			this.press[i] = new Anim(0f);
			this.rippleAnim[i] = new Anim(1f);
		}
	}

	private void refreshLabels() {
		if (Clock.time() - this.labelsAt < 1.0) {
			return;
		}
		this.labelsAt = Clock.time();
		Options o = Minecraft.getInstance().options;
		KeyMapping[] keys = {o.keyUp, o.keyLeft, o.keyDown, o.keyRight};
		for (int i = 0; i < 4; i++) {
			String name = keys[i].getTranslatedKeyMessage().getString();
			if (!name.equals(this.labels[i])) {
				this.labels[i] = name;
			}
		}
		this.spaceIsSpace = o.keyJump.isDefault();
		if (!this.spaceIsSpace) {
			this.labels[SPACE] = o.keyJump.getTranslatedKeyMessage().getString();
		}
	}

	private boolean demo(int key) {
		double t = Clock.time();
		return switch (key) {
			case W -> t % 2.4 < 1.25;
			case A -> t % 3.1 > 0.6 && t % 3.1 < 0.95;
			case S -> t % 5.3 > 3.6 && t % 5.3 < 3.9;
			case D -> t % 2.7 > 1.45 && t % 2.7 < 1.8;
			case LMB -> t % 4.0 < 1.8 && (t * 7.0) % 1.0 < 0.38;
			case RMB -> t % 3.3 > 2.1 && t % 3.3 < 2.45;
			default -> t % 1.9 < 0.2;
		};
	}

	private void updateKeys(boolean preview) {
		Minecraft mc = Minecraft.getInstance();
		Options o = mc.options;
		boolean simulate = preview && (mc.screen != null || mc.level == null);
		for (int i = 0; i < KEYS; i++) {
			boolean d = simulate ? this.demo(i) : switch (i) {
				case W -> o.keyUp.isDown();
				case A -> o.keyLeft.isDown();
				case S -> o.keyDown.isDown();
				case D -> o.keyRight.isDown();
				case LMB -> o.keyAttack.isDown();
				case RMB -> o.keyUse.isDown();
				default -> o.keyJump.isDown();
			};
			if (d != this.down[i]) {
				this.down[i] = d;
				if (d) {
					this.press[i].to(1f, 0.06f, Easing.OUT_CUBIC);
					this.rippleAnim[i].restart(0f, 1f, 0.45f, Easing.OUT_CUBIC);
				} else {
					this.press[i].to(0f, this.fade.value() / 1000f, Easing.OUT_CUBIC);
				}
			}
		}
		this.leftCps.setLength(0);
		this.rightCps.setLength(0);
		if (simulate) {
			this.leftCps.append(Clock.time() % 4.0 < 1.8 ? 7 : 0).append(" CPS");
			this.rightCps.append(0).append(" CPS");
		} else {
			this.leftCps.append(InputStats.leftCps()).append(" CPS");
			this.rightCps.append(InputStats.rightCps()).append(" CPS");
		}
	}

	@Override
	protected void layout(boolean preview) {
		this.refreshLabels();
		this.updateKeys(preview);
		float s = this.keySize.value();
		float gp = this.gap.value();
		this.width = s * 3f + gp * 2f;
		float h = s * 2f + gp;
		if (this.mouse.on()) {
			h += gp + s;
		}
		if (this.space.on()) {
			h += gp + Math.round(s * 0.55f);
		}
		this.height = h;
	}

	@Override
	protected void draw(GuiGraphics g, float partialTick, boolean preview) {
		float s = this.keySize.value();
		float gp = this.gap.value();
		float step = s + gp;
		this.key(g, W, step, 0f, s, s);
		this.key(g, A, 0f, step, s, s);
		this.key(g, S, step, step, s, s);
		this.key(g, D, step * 2f, step, s, s);
		float y = step * 2f;
		if (this.mouse.on()) {
			float mw = (this.width - gp) * 0.5f;
			this.key(g, LMB, 0f, y, mw, s);
			this.key(g, RMB, mw + gp, y, mw, s);
			y += step;
		}
		if (this.space.on()) {
			this.key(g, SPACE, 0f, y, this.width, Math.round(s * 0.55f));
		}
	}

	private void key(GuiGraphics g, int index, float x, float y, float w, float h) {
		float p = this.press[index].get();
		int accent = this.pressed.color();
		float radius = Math.min(HudStyle.radius(), Math.min(w, h) * 0.32f);
		float cx = x + w * 0.5f;
		float cy = y + h * 0.5f;
		boolean scaled = this.depth.on() && p > 0.001f;
		if (scaled) {
			float k = 1f - 0.06f * p;
			g.pose().pushMatrix();
			g.pose().translate(cx, cy);
			g.pose().scale(k, k);
			g.pose().translate(-cx, -cy);
		}
		if (p > 0.001f) {
			Draw.glow(g, x, y, w, h, radius, 6f, 0f, Colors.mulAlpha(accent, 0.28f * p));
		}
		HudStyle.panel(g, x, y, w, h, radius);
		if (p > 0.001f) {
			Draw.rect(g, x, y, w, h).radius(radius).color(Colors.mulAlpha(accent, 0.92f * p)).submit();
		}
		float rp = this.rippleAnim[index].get();
		if (this.ripple.on() && rp < 1f) {
			// A soft flash that spreads from the center and fades, clipped to the key.
			Draw.pushClip(g, x, y, x + w, y + h);
			float rr = Math.max(w * 0.5f, h * 0.5f) * (0.35f + 0.95f * rp);
			Draw.circleShape(g, cx, cy, rr).color(Colors.withAlpha(0xFFFFFF, 0.3f * (1f - rp) * (1f - rp))).soft(rr * 0.35f).submit();
			Draw.popClip();
		}

		int onAccent = Colors.luminance(accent) > 0.45f ? 0xFF0A0A0B : 0xFFFFFFFF;
		int textColor = Colors.lerp(HudStyle.text(), onAccent, p);
		XeysFont font = HudStyle.fontStrong();
		float size = Math.min(HudStyle.textSize() * (h >= 20f ? 1.05f : 0.95f), h * 0.46f);
		if (index == SPACE && this.spaceIsSpace) {
			float bw = Math.min(w * 0.3f, 28f);
			Draw.rect(g, cx - bw * 0.5f, cy - 1f, bw, 2f, 1f, textColor);
		} else if ((index == LMB || index == RMB) && this.cps.on()) {
			float ls = size * 0.92f;
			HudStyle.label(g, font, this.labels[index], cx, cy - h * 0.15f, ls, textColor, Text.Align.CENTER);
			HudStyle.label(g, HudStyle.font(), index == LMB ? this.leftCps : this.rightCps, cx, cy + h * 0.2f, ls * 0.72f,
				Colors.mulAlpha(textColor, 0.72f), Text.Align.CENTER);
		} else {
			String label = this.labels[index];
			float lw = font.width(label, size);
			float fit = lw > w - 6f ? size * (w - 6f) / lw : size;
			HudStyle.label(g, font, label, cx, cy, fit, textColor, Text.Align.CENTER);
		}
		if (scaled) {
			g.pose().popMatrix();
		}
	}
}
