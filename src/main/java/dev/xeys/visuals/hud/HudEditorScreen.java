package dev.xeys.visuals.hud;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.xeys.visuals.anim.Anim;
import dev.xeys.visuals.anim.Easing;
import dev.xeys.visuals.anim.Spring;
import dev.xeys.visuals.core.config.ConfigManager;
import dev.xeys.visuals.core.module.ModuleManager;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.ui.clickgui.ClickGuiScreen;
import dev.xeys.visuals.ui.core.Switch;
import dev.xeys.visuals.ui.core.UiScale;
import dev.xeys.visuals.ui.core.UiSounds;
import dev.xeys.visuals.util.Colors;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Drag-and-drop HUD layout editor with snapping guides, scaling and live preview. Elements render with
 * real data in a world and sample data elsewhere.
 */
public final class HudEditorScreen extends Screen {
	private static final float SNAP = 4f;
	private static final float EDGE_MARGIN = 4f;
	private static boolean snapping = true;
	private static boolean grid = false;

	private final @Nullable Screen parent;
	private final Anim open = new Anim(0f);
	private final Map<HudModule, Spring> hoverFx = new IdentityHashMap<>();
	private @Nullable HudModule hovered;
	private @Nullable HudModule dragging;
	private @Nullable HudModule resizing;
	private float grabX;
	private float grabY;
	private float resizeStartScale;
	private float resizeStartDist;
	private float guideX = Float.NaN;
	private float guideY = Float.NaN;
	private boolean trayOpen;
	private final Anim tray = new Anim(0f);
	private int toolbarHover = -1;
	private int trayHover = -1;

	public HudEditorScreen(@Nullable Screen parent) {
		super(Component.literal("HUD Editor"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		this.open.restart(0f, 1f, 0.3f, Easing.OUT_CUBIC);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		if (this.minecraft.level == null) {
			this.renderPanorama(g, delta);
		}
	}

	private static boolean preview() {
		return net.minecraft.client.Minecraft.getInstance().level == null;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		float a = this.open.get();
		float sw = this.width;
		float sh = this.height;
		float mx = mouseX;
		float my = mouseY;

		// Subtle darkening, optional grid and the center cross.
		Draw.rect(g, 0, 0, sw, sh).color(Colors.withAlpha(0x000000, 0.28f * a)).submit();
		if (grid) {
			for (float gx = 0; gx < sw; gx += 16f) {
				Draw.rect(g, gx, 0, 0.5f, sh).color(Colors.withAlpha(0xFFFFFF, 0.04f * a)).submit();
			}
			for (float gy = 0; gy < sh; gy += 16f) {
				Draw.rect(g, 0, gy, sw, 0.5f).color(Colors.withAlpha(0xFFFFFF, 0.04f * a)).submit();
			}
		}
		Draw.rect(g, sw * 0.5f - 0.25f, 0, 0.5f, sh).color(Colors.withAlpha(0xFFFFFF, 0.07f * a)).submit();
		Draw.rect(g, 0, sh * 0.5f - 0.25f, sw, 0.5f).color(Colors.withAlpha(0xFFFFFF, 0.07f * a)).submit();

		// Elements.
		List<HudModule> mods = ModuleManager.hudModules();
		this.hovered = null;
		boolean overChrome = this.overChrome(mouseX, mouseY);
		for (int i = mods.size() - 1; i >= 0 && !overChrome; i--) {
			HudModule m = mods.get(i);
			if (m.isEnabled() && this.contains(m, mx, my, sw, sh)) {
				this.hovered = m;
				break;
			}
		}
		if (this.dragging != null) {
			this.hovered = this.dragging;
		}
		float partial = delta;
		for (HudModule m : mods) {
			if (!m.isEnabled()) {
				continue;
			}
			HudRenderer.renderModule(g, m, sw, sh, partial, preview() || !m.hasContent(false));
			this.drawFrame(g, m, sw, sh, a);
		}

		// Snap guides.
		if (!Float.isNaN(this.guideX)) {
			Draw.rect(g, this.guideX - 0.5f, 0, 1f, sh).color(Colors.withAlpha(Theme.accent(), 0.85f * a)).submit();
		}
		if (!Float.isNaN(this.guideY)) {
			Draw.rect(g, 0, this.guideY - 0.5f, sw, 1f).color(Colors.withAlpha(Theme.accent(), 0.85f * a)).submit();
		}

		if (this.hovered != null) {
			g.requestCursor(this.overHandle(this.hovered, mx, my, sw, sh) ? CursorTypes.RESIZE_ALL : CursorTypes.POINTING_HAND);
		}
		this.renderChrome(g, mouseX, mouseY, a);
	}

	private boolean contains(HudModule m, float mx, float my, float sw, float sh) {
		m.layoutFrame(preview() || !m.hasContent(false));
		float w = m.scaledWidth();
		float h = m.scaledHeight();
		float x = m.position.left(sw, w);
		float y = m.position.top(sh, h);
		return mx >= x - 2f && mx <= x + w + 2f && my >= y - 2f && my <= y + h + 2f;
	}

	private boolean overHandle(HudModule m, float mx, float my, float sw, float sh) {
		float w = m.scaledWidth();
		float h = m.scaledHeight();
		float x = m.position.left(sw, w) + w;
		float y = m.position.top(sh, h) + h;
		return Math.abs(mx - x) <= 4f && Math.abs(my - y) <= 4f;
	}

	private void drawFrame(GuiGraphics g, HudModule m, float sw, float sh, float a) {
		Spring fx = this.hoverFx.computeIfAbsent(m, k -> new Spring(0f, 300f, 1f));
		fx.target(m == this.hovered ? 1f : 0f);
		float h = fx.get();
		float w = m.scaledWidth();
		float ht = m.scaledHeight();
		float x = m.position.left(sw, w);
		float y = m.position.top(sh, ht);
		int line = Colors.lerpOklab(Colors.withAlpha(0xFFFFFF, 0.22f), Colors.withAlpha(Theme.accent(), 0.95f), h);
		Draw.outline(g, x - 2f, y - 2f, w + 4f, ht + 4f, 4f, 0.75f, Colors.mulAlpha(line, a));
		if (h > 0.01f) {
			Draw.glow(g, x - 2f, y - 2f, w + 4f, ht + 4f, 4f, 8f, -1f, Colors.withAlpha(Theme.accent(), 0.18f * h * a));
			// Name chip above the element (below it when there is no room).
			String label = m.name() + "  " + Math.round(m.position.scale() * 100f) + "%";
			float lw = Fonts.SEMIBOLD.width(label, 6.5f) + 10f;
			float ly = y - 13f < 2f ? y + ht + 4f : y - 13f;
			Draw.rect(g, x - 2f, ly, lw, 10f).radius(3f).color(Colors.withAlpha(Theme.accent(), 0.92f * h * a)).submit();
			Text.drawMiddle(g, Fonts.SEMIBOLD, label, x + 3f, ly + 5f, 6.5f, Colors.mulAlpha(Theme.onAccent(), h * a), Text.Align.LEFT);
			Draw.circleShape(g, x + w + 2f, y + ht + 2f, 2.6f).color(Colors.mulAlpha(0xFFFFFFFF, h * a))
				.border(1f, Colors.withAlpha(Theme.accent(), h * a)).submit();
		}
	}

	// ------------------------------------------------------------------ chrome (design units)

	private static final String[] TOOLS = {"Snap", "Grid", "Elements", "Reset", "Done"};
	private static final char[] TOOL_ICONS = {Icons.MAGNET, Icons.GRID_3X3, Icons.LAYERS, Icons.ROTATE_CCW, Icons.CHECK};
	private float toolbarX;
	private float toolbarY;
	private final float[] toolX = new float[TOOLS.length];
	private final float[] toolW = new float[TOOLS.length];
	private float trayX;
	private float trayY;
	private float trayW;
	private float trayH;

	private boolean overChrome(double guiX, double guiY) {
		float mx = UiScale.toDesign(guiX);
		float my = UiScale.toDesign(guiY);
		boolean bar = my >= this.toolbarY && my <= this.toolbarY + 44f && mx >= this.toolbarX && mx <= this.toolbarX + this.toolbarWidth();
		boolean trayHit = this.trayOpen && mx >= this.trayX && mx <= this.trayX + this.trayW && my >= this.trayY && my <= this.trayY + this.trayH;
		return bar || trayHit;
	}

	private float toolbarWidth() {
		float w = 8f;
		for (String t : TOOLS) {
			w += Fonts.MEDIUM.width(t, 12f) + 46f;
		}
		return w;
	}

	private void renderChrome(GuiGraphics g, int mouseX, int mouseY, float a) {
		float mx = UiScale.toDesign(mouseX);
		float my = UiScale.toDesign(mouseY);
		UiScale.push(g);
		float W = UiScale.width();
		float H = UiScale.height();
		float tw = this.toolbarWidth();
		this.toolbarX = (W - tw) * 0.5f;
		this.toolbarY = 18f - (1f - a) * 12f;
		Draw.shadow(g, this.toolbarX, this.toolbarY, tw, 44f, 14f, 0f, 8f, 26f, -2f, Colors.withAlpha(0x000000, 0.5f * a));
		Draw.glass(g, this.toolbarX, this.toolbarY, tw, 44f, 0xD8070708).radius(14f).border(1f, Theme.GLASS_BORDER).borderFade(0.3f).alpha(a).submit();
		float x = this.toolbarX + 6f;
		this.toolbarHover = -1;
		for (int i = 0; i < TOOLS.length; i++) {
			float bw = Fonts.MEDIUM.width(TOOLS[i], 12f) + 40f;
			this.toolX[i] = x;
			this.toolW[i] = bw;
			boolean hov = mx >= x && mx < x + bw && my >= this.toolbarY + 6f && my < this.toolbarY + 38f;
			boolean active = (i == 0 && snapping) || (i == 1 && grid) || (i == 2 && this.trayOpen);
			if (hov) {
				this.toolbarHover = i;
				g.requestCursor(CursorTypes.POINTING_HAND);
			}
			if (active || hov || i == 4) {
				int bg = i == 4 ? (hov ? Theme.accentHover() : Theme.accent()) : (active ? Theme.accentSoft() : 0x12FFFFFF);
				Draw.rect(g, x, this.toolbarY + 6f, bw, 32f).radius(10f).color(Colors.mulAlpha(bg, a)).submit();
			}
			int fg = i == 4 ? Theme.onAccent() : (active ? Theme.accentText() : (hov ? Theme.TEXT : Theme.TEXT_SECONDARY));
			Text.icon(g, TOOL_ICONS[i], x + 18f, this.toolbarY + 22f, 14f, Colors.mulAlpha(fg, a));
			Text.drawMiddle(g, Fonts.MEDIUM, TOOLS[i], x + 32f, this.toolbarY + 22f, 12f, Colors.mulAlpha(fg, a), Text.Align.LEFT);
			x += bw + 2f;
		}

		// Elements tray.
		this.tray.to(this.trayOpen ? 1f : 0f, this.trayOpen ? 0.25f : 0.15f, Easing.OUT_CUBIC);
		float ta = this.tray.get() * a;
		List<HudModule> mods = ModuleManager.hudModules();
		this.trayW = 300f;
		this.trayH = Math.min(H - 120f, 20f + mods.size() * 40f);
		this.trayX = this.toolX[2] + this.toolW[2] * 0.5f - this.trayW * 0.5f;
		this.trayY = this.toolbarY + 54f - (1f - this.tray.get()) * 8f;
		this.trayHover = -1;
		if (ta > 0.01f) {
			Draw.shadow(g, this.trayX, this.trayY, this.trayW, this.trayH, 14f, 0f, 10f, 30f, -2f, Colors.withAlpha(0x000000, 0.55f * ta));
			Draw.rect(g, this.trayX, this.trayY, this.trayW, this.trayH).radius(14f).color(Colors.withAlpha(0xFF0E0E11, 0.985f * ta))
				.border(1f, Colors.withAlpha(0xFFFFFF, 0.12f * ta)).submit();
			Draw.pushClip(g, this.trayX, this.trayY + 6f, this.trayX + this.trayW, this.trayY + this.trayH - 6f, 16f);
			for (int i = 0; i < mods.size(); i++) {
				HudModule m = mods.get(i);
				float ry = this.trayY + 10f + i * 40f;
				boolean hov = this.trayOpen && mx >= this.trayX + 6f && mx < this.trayX + this.trayW - 6f && my >= ry && my < ry + 38f;
				if (hov) {
					this.trayHover = i;
					Draw.rect(g, this.trayX + 6f, ry, this.trayW - 12f, 38f, 9f, Colors.withAlpha(0xFFFFFF, 0.04f * ta));
				}
				Text.icon(g, m.icon(), this.trayX + 28f, ry + 19f, 15f, Colors.mulAlpha(m.isEnabled() ? Theme.accent() : Theme.TEXT_MUTED, ta));
				Text.drawMiddle(g, Fonts.MEDIUM, m.name(), this.trayX + 48f, ry + 19f, 12.5f, Colors.mulAlpha(Theme.TEXT, ta), Text.Align.LEFT);
				Switch.draw(g, this.trayX + this.trayW - 18f - Switch.WIDTH, ry + 19f - Switch.HEIGHT * 0.5f, m.isEnabled() ? 1f : 0f, hov ? 0.6f : 0f, ta);
			}
			Draw.popClip();
		}

		// Hint line.
		String hint = "Drag to move  ·  Scroll or drag the corner to resize  ·  Right-click for settings  ·  Hold Alt to place freely";
		Text.drawMiddle(g, Fonts.MEDIUM, hint, W * 0.5f, H - 26f, 11.5f, Colors.mulAlpha(Theme.TEXT_MUTED, a), Text.Align.CENTER);
		UiScale.pop(g);
	}

	// ------------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		float mx = (float) event.x();
		float my = (float) event.y();
		int button = event.button();
		if (this.toolbarHover >= 0) {
			UiSounds.soft();
			switch (this.toolbarHover) {
				case 0 -> snapping = !snapping;
				case 1 -> grid = !grid;
				case 2 -> this.trayOpen = !this.trayOpen;
				case 3 -> {
					for (HudModule m : ModuleManager.hudModules()) {
						m.position.reset();
					}
				}
				default -> this.onClose();
			}
			return true;
		}
		if (this.trayOpen && this.overChrome(mx, my)) {
			if (this.trayHover >= 0) {
				HudModule m = ModuleManager.hudModules().get(this.trayHover);
				m.toggle();
				UiSounds.toggle(m.isEnabled());
			}
			return true;
		}
		if (this.trayOpen) {
			this.trayOpen = false;
		}
		HudModule target = this.hovered;
		if (target == null) {
			return false;
		}
		if (button == 1) {
			UiSounds.click();
			this.minecraft.setScreen(ClickGuiScreen.forModule(this, target));
			return true;
		}
		if (button == 0) {
			float sw = this.width;
			float sh = this.height;
			if (this.overHandle(target, mx, my, sw, sh)) {
				this.resizing = target;
				this.resizeStartScale = target.position.scale();
				float ox = target.position.left(sw, target.scaledWidth());
				float oy = target.position.top(sh, target.scaledHeight());
				this.resizeStartDist = Math.max(4f, (float) Math.hypot(mx - ox, my - oy));
			} else {
				this.dragging = target;
				this.grabX = mx - target.position.left(sw, target.scaledWidth());
				this.grabY = my - target.position.top(sh, target.scaledHeight());
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		float mx = (float) event.x();
		float my = (float) event.y();
		float sw = this.width;
		float sh = this.height;
		if (this.resizing != null) {
			HudModule m = this.resizing;
			float ox = m.position.left(sw, m.scaledWidth());
			float oy = m.position.top(sh, m.scaledHeight());
			float dist = (float) Math.hypot(mx - ox, my - oy);
			float scale = Math.round(this.resizeStartScale * dist / this.resizeStartDist * 20f) / 20f;
			float w0 = m.scaledWidth();
			float h0 = m.scaledHeight();
			m.position.setScale(scale);
			// Keep the top-left fixed while scaling from the corner.
			m.position.setFromTopLeft(ox, oy, m.scaledWidth(), m.scaledHeight(), sw, sh);
			if (w0 != m.scaledWidth() || h0 != m.scaledHeight()) {
				this.guideX = Float.NaN;
				this.guideY = Float.NaN;
			}
			return true;
		}
		if (this.dragging == null) {
			return false;
		}
		HudModule m = this.dragging;
		float w = m.scaledWidth();
		float h = m.scaledHeight();
		float left = mx - this.grabX;
		float top = my - this.grabY;
		this.guideX = Float.NaN;
		this.guideY = Float.NaN;
		boolean free = InputConstants.isKeyDown(this.minecraft.getWindow(), InputConstants.KEY_LALT)
			|| InputConstants.isKeyDown(this.minecraft.getWindow(), InputConstants.KEY_RALT);
		if (snapping && !free) {
			float[] sx = this.snap(left, w, sw, true, m);
			left = sx[0];
			this.guideX = sx[1];
			float[] sy = this.snap(top, h, sh, false, m);
			top = sy[0];
			this.guideY = sy[1];
		}
		left = Math.max(0f, Math.min(left, sw - w));
		top = Math.max(0f, Math.min(top, sh - h));
		m.position.setFromTopLeft(left, top, w, h, sw, sh);
		return true;
	}

	/**
	 * Snaps one axis to screen edges (with a margin), the screen center and other elements' edges and
	 * centers. Returns {new position, guide coordinate or NaN}.
	 */
	private float[] snap(float pos, float size, float screen, boolean xAxis, HudModule self) {
		float best = SNAP + 1f;
		float result = pos;
		float guide = Float.NaN;
		float[] candidates = {EDGE_MARGIN, screen - EDGE_MARGIN - size, screen * 0.5f - size * 0.5f};
		float[] guides = {EDGE_MARGIN, screen - EDGE_MARGIN, screen * 0.5f};
		for (int i = 0; i < candidates.length; i++) {
			float d = Math.abs(pos - candidates[i]);
			if (d < best) {
				best = d;
				result = candidates[i];
				guide = guides[i];
			}
		}
		for (HudModule o : ModuleManager.hudModules()) {
			if (o == self || !o.isEnabled()) {
				continue;
			}
			float ow = xAxis ? o.scaledWidth() : o.scaledHeight();
			float op = xAxis ? o.position.left(this.width, o.scaledWidth()) : o.position.top(this.height, o.scaledHeight());
			float[] edges = {op, op + ow, op + ow * 0.5f};
			for (float e : edges) {
				float[][] options = {{e, e}, {e - size, e}, {e - size * 0.5f, e}};
				for (float[] opt : options) {
					float d = Math.abs(pos - opt[0]);
					if (d < best) {
						best = d;
						result = opt[0];
						guide = opt[1];
					}
				}
			}
		}
		return best <= SNAP ? new float[] {result, guide} : new float[] {pos, Float.NaN};
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		boolean was = this.dragging != null || this.resizing != null;
		this.dragging = null;
		this.resizing = null;
		this.guideX = Float.NaN;
		this.guideY = Float.NaN;
		return was;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
		if (this.hovered != null && !this.overChrome(x, y)) {
			HudPosition p = this.hovered.position;
			float sw = this.width;
			float sh = this.height;
			float left = p.left(sw, this.hovered.scaledWidth());
			float top = p.top(sh, this.hovered.scaledHeight());
			p.setScale(Math.round((p.scale() + (vertical > 0 ? 0.05f : -0.05f)) * 20f) / 20f);
			p.setFromTopLeft(left, top, this.hovered.scaledWidth(), this.hovered.scaledHeight(), sw, sh);
			return true;
		}
		return false;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == InputConstants.KEY_ESCAPE) {
			if (this.trayOpen) {
				this.trayOpen = false;
				return true;
			}
			this.onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		ConfigManager.saveIfDirty();
		this.minecraft.setScreen(this.parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
