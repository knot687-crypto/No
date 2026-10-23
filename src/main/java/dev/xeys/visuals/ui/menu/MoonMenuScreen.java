package dev.xeys.visuals.ui.menu;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.xeys.visuals.Xeys;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.anim.Easing;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.ScreenFade;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.ui.clickgui.ClickGuiScreen;
import dev.xeys.visuals.ui.core.UiScale;
import dev.xeys.visuals.ui.core.Widget;
import dev.xeys.visuals.util.Colors;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import com.mojang.realmsclient.RealmsMainScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.Nullable;

/**
 * The XEYS main menu: a ray-traced moon over a parallax starfield, a glass dock and the wordmark.
 *
 * <p>Extends {@link TitleScreen} so every vanilla system that checks for the title screen (menu music,
 * Realms, telemetry, test harnesses) keeps working, but replaces all of its UI.
 */
public final class MoonMenuScreen extends TitleScreen {
	private static boolean firstShow = true;
	private static final float EXIT_SECONDS = 0.6f;

	private final MoonScene scene = new MoonScene();
	private final List<MenuButton> dock = new ArrayList<>();
	private final List<IconButton> corner = new ArrayList<>();
	private final List<Widget> focusOrder = new ArrayList<>();
	private double openedAt;
	private boolean cinematic;
	private @Nullable Runnable pendingAction;
	private int focusIndex = -1;
	private boolean copyrightHovered;
	private final String versionLine;

	public MoonMenuScreen() {
		super(false);
		String mc = "Minecraft " + SharedConstants.getCurrentVersion().name();
		String version = FabricLoader.getInstance().getModContainer(Xeys.MOD_ID)
			.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("1.0.0");
		this.versionLine = Xeys.NAME + " " + version + "  ·  " + mc;
	}

	// ------------------------------------------------------------------ lifecycle

	@Override
	protected void init() {
		this.dock.clear();
		this.corner.clear();
		this.focusOrder.clear();
		this.pendingAction = null;
		this.focusIndex = -1;

		this.dock.add(new MenuButton(Icons.USER, "Singleplayer", false, () -> this.go(() -> new SelectWorldScreen(this))));
		MenuButton multiplayer = new MenuButton(Icons.USERS, "Multiplayer", false, () -> this.go(() ->
			this.minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this)));
		multiplayer.enabled = this.minecraft.allowsMultiplayer();
		this.dock.add(multiplayer);
		this.dock.add(new MenuButton(Icons.SETTINGS, "Settings", false, () -> this.go(() -> new OptionsScreen(this, this.minecraft.options))));
		this.dock.add(new MenuButton(Icons.SPARKLES, "Visuals", true, () -> this.go(() -> new ClickGuiScreen(this))));
		this.dock.add(new MenuButton(Icons.POWER, "Quit", false, this::quit));

		this.corner.add(new IconButton(Icons.CLOUD, "Minecraft Realms", () -> this.go(() -> new RealmsMainScreen(this))));
		this.corner.add(new IconButton(Icons.GLOBE, "Language", () ->
			this.go(() -> new LanguageSelectScreen(this, this.minecraft.options, this.minecraft.getLanguageManager()))));
		this.corner.add(new IconButton(Icons.ACCESSIBILITY, "Accessibility", () ->
			this.go(() -> new AccessibilityOptionsScreen(this, this.minecraft.options))));

		this.focusOrder.addAll(this.dock);
		this.focusOrder.addAll(this.corner);
		this.layout();

		this.cinematic = firstShow;
		firstShow = false;
		this.openedAt = Clock.time();
		this.scene.playIntro(this.cinematic);
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
	}

	private void layout() {
		float W = UiScale.width();
		float H = UiScale.height();
		float gap = 12f;
		float total = -gap;
		for (MenuButton b : this.dock) {
			total += b.preferredWidth() + gap;
		}
		float x = (W - total) * 0.5f;
		float y = H * 0.895f - MenuButton.HEIGHT * 0.5f;
		for (MenuButton b : this.dock) {
			float bw = b.preferredWidth();
			b.bounds(x, y, bw, MenuButton.HEIGHT);
			x += bw + gap;
		}
		float ix = W - 28f - IconButton.SIZE;
		for (int i = this.corner.size() - 1; i >= 0; i--) {
			this.corner.get(i).bounds(ix, 26f, IconButton.SIZE, IconButton.SIZE);
			ix -= IconButton.SIZE + 10f;
		}
	}

	@Override
	public void tick() {
		// No vanilla widgets or Realms notifications to tick.
	}

	@Override
	public void removed() {
		// Nothing to release: the moon textures stay resident for the next visit.
	}

	@Override
	public void added() {
		// Skip TitleScreen.added (Realms notification screen).
	}

	// ------------------------------------------------------------------ navigation

	private void go(Supplier<Screen> next) {
		if (this.pendingAction != null) {
			return;
		}
		this.scene.playExit(EXIT_SECONDS);
		this.pendingAction = () -> {
			ScreenFade.fadeFrom(0xFF020203, 0.4f, Easing.OUT_CUBIC);
			this.minecraft.setScreen(next.get());
		};
	}

	private void quit() {
		if (this.pendingAction != null) {
			return;
		}
		this.scene.playExit(EXIT_SECONDS);
		this.pendingAction = this.minecraft::stop;
	}

	private float appear(float delay, float duration) {
		float scale = this.cinematic ? 1f : 0.22f;
		float t = (float) ((Clock.time() - this.openedAt - delay * scale) / (duration * (this.cinematic ? 1f : 0.7f)));
		float in = Easing.OUT_CUBIC.apply(Math.max(0f, Math.min(1f, t)));
		return in * (1f - this.scene.exitProgress());
	}

	// ------------------------------------------------------------------ rendering

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		var window = this.minecraft.getWindow();
		float cx = (float) (this.minecraft.mouseHandler.xpos() / Math.max(1, window.getScreenWidth())) * 2f - 1f;
		float cy = (float) (this.minecraft.mouseHandler.ypos() / Math.max(1, window.getScreenHeight())) * 2f - 1f;
		this.scene.update(window.getWidth(), window.getHeight(), cx, cy);
		if (!MoonSky.draw(this.scene)) {
			// Pipeline unavailable: a calm gradient keeps the menu usable.
			g.fillGradient(0, 0, this.width, this.height, 0xFF07080D, 0xFF0B0C14);
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		if (this.pendingAction != null && this.scene.exitProgress() >= 1f) {
			Runnable action = this.pendingAction;
			this.pendingAction = null;
			ScreenFade.cover(0xFF020203);
			action.run();
			return;
		}

		float mx = UiScale.toDesign(mouseX);
		float my = UiScale.toDesign(mouseY);
		float W = UiScale.width();
		float H = UiScale.height();
		UiScale.push(g);

		this.renderLogo(g, W, H);

		for (int i = 0; i < this.dock.size(); i++) {
			MenuButton b = this.dock.get(i);
			b.appear = this.appear(0.95f + i * 0.075f, 0.7f);
			b.render(g, mx, my);
		}
		for (int i = 0; i < this.corner.size(); i++) {
			IconButton b = this.corner.get(i);
			b.appear = this.appear(1.35f + i * 0.06f, 0.6f);
			b.render(g, mx, my);
		}
		this.renderFooter(g, W, H, mx, my);
		UiScale.pop(g);
	}

	private void renderLogo(GuiGraphics g, float W, float H) {
		float a = this.appear(0.5f, 1.1f);
		if (a <= 0.001f) {
			return;
		}
		float size = Math.max(H * 0.064f, 46f);
		float tracking = 0.26f;
		float cy = H * 0.748f + (1f - a) * 14f;
		String mark = "XEYS";
		float w = Fonts.DISPLAY.width(mark, size, tracking);
		float left = (W - w) * 0.5f + size * tracking * 0.5f;
		float baseline = cy + Fonts.DISPLAY.capHeight(size) * 0.5f;

		// Accent bloom behind the wordmark, then the silver letters.
		Text.shadow(g, Fonts.DISPLAY, mark, left, baseline, size, Colors.withAlpha(Theme.accent(), 0.30f * a), 0f, 0f, 9f, tracking);
		Text.drawBaseline(g, Fonts.DISPLAY, mark, left, baseline, size, Colors.mulAlpha(0xFFD9DEE8, a), tracking, 0f);

		// Periodic light sheen sweeping across the letters.
		double period = 7.0;
		double t = (Clock.time() - this.openedAt - (this.cinematic ? 1.4 : 0.6)) % period;
		if (t >= 0 && t < 1.3) {
			float p = Easing.IN_OUT_SINE.apply((float) (t / 1.3));
			float band = size * 0.9f;
			float bx = left - band * 2f + (w + band * 4f) * p;
			Draw.pushClip(g, bx, cy - size, bx + band, cy + size, band * UiScale.factor() * 0.45f);
			Text.drawBaseline(g, Fonts.DISPLAY, mark, left, baseline, size, Colors.mulAlpha(0xFFFFFFFF, a), tracking, 0.6f);
			Text.shadow(g, Fonts.DISPLAY, mark, left, baseline, size, Colors.withAlpha(0xFFFFFF, 0.45f * a), 0f, 0f, 6f, tracking);
			Draw.popClip();
		}

		// Subtitle with hairlines.
		float sa = this.appear(0.75f, 1.0f);
		if (sa > 0.001f) {
			String sub = "VISUALS";
			float subSize = 13f;
			float subTracking = 0.62f;
			float sy = cy + size * 0.62f + 8f + (1f - sa) * 8f;
			float sw = Fonts.SEMIBOLD.width(sub, subSize, subTracking);
			float sx = (W - sw) * 0.5f + subSize * subTracking * 0.5f;
			Text.drawMiddle(g, Fonts.SEMIBOLD, sub, sx, sy, subSize, Colors.mulAlpha(Theme.TEXT_SECONDARY, sa), Text.Align.LEFT, subTracking);
			float line = 54f * sa;
			float lx0 = (W - sw) * 0.5f - 18f;
			float lx1 = (W + sw) * 0.5f + 18f;
			Draw.rect(g, lx0 - line, sy - 0.5f, line, 1f).gradientH(Colors.withAlpha(Theme.accent(), 0f), Colors.withAlpha(Theme.accent(), 0.75f * sa)).submit();
			Draw.rect(g, lx1, sy - 0.5f, line, 1f).gradientH(Colors.withAlpha(Theme.accent(), 0.75f * sa), Colors.withAlpha(Theme.accent(), 0f)).submit();
		}
	}

	private void renderFooter(GuiGraphics g, float W, float H, float mx, float my) {
		float a = this.appear(1.5f, 0.8f);
		if (a <= 0.001f) {
			return;
		}
		float y = H - 24f;
		Text.drawMiddle(g, Fonts.MEDIUM, this.versionLine, 28f, y, 11.5f, Colors.mulAlpha(Theme.TEXT_FAINT, a), Text.Align.LEFT);
		String copyright = "Copyright Mojang AB. Do not distribute!";
		float cw = Fonts.MEDIUM.width(copyright, 11.5f);
		float cx = W - 28f - cw;
		this.copyrightHovered = mx >= cx && mx <= cx + cw && my >= y - 9f && my <= y + 9f && this.pendingAction == null;
		if (this.copyrightHovered) {
			g.requestCursor(CursorTypes.POINTING_HAND);
		}
		Text.drawMiddle(g, Fonts.MEDIUM, copyright, cx, y, 11.5f,
			Colors.mulAlpha(this.copyrightHovered ? Theme.TEXT_SECONDARY : Theme.TEXT_FAINT, a), Text.Align.LEFT);
	}

	// ------------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.pendingAction != null) {
			return true;
		}
		float mx = UiScale.toDesign(event.x());
		float my = UiScale.toDesign(event.y());
		for (Widget w : this.focusOrder) {
			if (w.mouseClicked(mx, my, event.button())) {
				this.setFocusIndex(-1);
				return true;
			}
		}
		if (this.copyrightHovered && event.button() == 0) {
			this.go(() -> new CreditsAndAttributionScreen(this));
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		float mx = UiScale.toDesign(event.x());
		float my = UiScale.toDesign(event.y());
		boolean handled = false;
		for (Widget w : this.focusOrder) {
			handled |= w.mouseReleased(mx, my, event.button());
		}
		return handled;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.pendingAction != null) {
			return true;
		}
		int key = event.key();
		boolean shift = (event.modifiers() & InputConstants.MOD_SHIFT) != 0;
		if (key == InputConstants.KEY_TAB) {
			this.moveFocus(shift ? -1 : 1);
			return true;
		}
		if (key == InputConstants.KEY_RIGHT || key == InputConstants.KEY_LEFT) {
			this.moveFocus(key == InputConstants.KEY_RIGHT ? 1 : -1);
			return true;
		}
		if ((key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER || key == InputConstants.KEY_SPACE) && this.focusIndex >= 0) {
			Widget w = this.focusOrder.get(this.focusIndex);
			if (w instanceof MenuButton b && b.enabled) {
				b.activate();
			} else if (w instanceof IconButton b) {
				b.activate();
			}
			return true;
		}
		return false;
	}

	private void moveFocus(int dir) {
		int n = this.focusOrder.size();
		int i = this.focusIndex;
		for (int step = 0; step < n; step++) {
			i = i < 0 ? (dir > 0 ? 0 : n - 1) : Math.floorMod(i + dir, n);
			if (this.focusOrder.get(i).enabled) {
				break;
			}
		}
		this.setFocusIndex(i);
	}

	private void setFocusIndex(int index) {
		this.focusIndex = index;
		for (int i = 0; i < this.focusOrder.size(); i++) {
			this.focusOrder.get(i).setFocused(i == index);
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
