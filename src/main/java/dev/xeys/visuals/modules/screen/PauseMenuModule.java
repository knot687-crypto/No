package dev.xeys.visuals.modules.screen;

import dev.xeys.visuals.core.Session;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import dev.xeys.visuals.util.Fmt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.multiplayer.ServerData;

/**
 * A composed pause menu: vanilla's buttons (every one of them, including server additions) gathered on a
 * frosted card under the XEYS wordmark, centered on screen, with the world or server and session time.
 */
public final class PauseMenuModule extends Module {
	private static final float PAD = 16f;
	private static final float HEADER = 44f;
	private static PauseMenuModule instance;

	private final BoolSetting info = this.add(new BoolSetting("info", "Session Info", true).describe("World or server and session time")
		.group("Pause Menu"));
	private final BoolSetting wordmark = this.add(new BoolSetting("wordmark", "Wordmark", true).group("Pause Menu"));

	private float cardX;
	private float cardY;
	private float cardW;
	private float cardH;
	private String subtitle = "";
	private final StringBuilder line = new StringBuilder(48);

	public PauseMenuModule() {
		super("pause_menu", "Pause Menu", Category.SCREEN, Icons.PAUSE, "A composed, frosted pause menu");
		instance = this;
		this.enabledByDefault();
	}

	private static boolean on(PauseScreen screen) {
		return instance != null && instance.isEnabled() && screen.showsPauseMenu();
	}

	/** After vanilla lays the menu out: center it and make room for the header. */
	public static void arrange(PauseScreen screen) {
		if (!on(screen)) {
			return;
		}
		PauseMenuModule m = instance;
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		for (GuiEventListener l : screen.children()) {
			if (l instanceof StringWidget title) {
				title.visible = false;
				m.subtitle = title.getMessage().getString();
			} else if (l instanceof AbstractWidget w && w.visible) {
				minX = Math.min(minX, w.getX());
				minY = Math.min(minY, w.getY());
				maxX = Math.max(maxX, w.getX() + w.getWidth());
				maxY = Math.max(maxY, w.getY() + w.getHeight());
			}
		}
		if (minX > maxX) {
			return;
		}
		float contentH = maxY - minY;
		float totalH = HEADER + contentH + PAD + (m.info.on() ? 30f : 0f);
		float top = Math.max(8f, (screen.height - totalH) * 0.5f);
		int dy = Math.round(top + HEADER - minY);
		for (GuiEventListener l : screen.children()) {
			if (l instanceof AbstractWidget w && !(l instanceof StringWidget)) {
				w.setY(w.getY() + dy);
			}
		}
		m.cardX = minX - PAD;
		m.cardY = top;
		m.cardW = maxX - minX + PAD * 2f;
		m.cardH = HEADER + contentH + PAD;
	}

	/** Drawn with the background, so the buttons sit on top of the card. */
	public static void decorate(GuiGraphics g, PauseScreen screen) {
		if (!on(screen) || instance.cardW <= 0f) {
			return;
		}
		PauseMenuModule m = instance;
		float x = m.cardX;
		float y = m.cardY;
		float w = m.cardW;
		float h = m.cardH;
		Draw.shadow(g, x, y, w, h, 16f, 0f, 10f, 32f, -4f, Colors.withAlpha(0x000000, 0.55f));
		Draw.glass(g, x, y, w, h, 0xC8080809).radius(16f).border(1f, Theme.GLASS_BORDER).borderFade(0.25f).submit();
		float cx = x + w * 0.5f;
		if (m.wordmark.on()) {
			float size = 15f;
			float tracking = 0.24f;
			float mw = Fonts.DISPLAY.width("XEYS", size, tracking);
			float base = y + 17f + Fonts.DISPLAY.capHeight(size) * 0.5f;
			Text.shadow(g, Fonts.DISPLAY, "XEYS", cx - mw * 0.5f, base, size, Colors.withAlpha(Theme.accent(), 0.35f), 0f, 0f, 4f, tracking);
			Text.drawBaseline(g, Fonts.DISPLAY, "XEYS", cx - mw * 0.5f, base, size, 0xFFDDE2EC, tracking, 0f);
			Text.drawMiddle(g, Fonts.MEDIUM, m.subtitle, cx, y + 33f, 8.5f, Theme.TEXT_MUTED, Text.Align.CENTER);
		} else {
			Text.drawMiddle(g, Fonts.SEMIBOLD, m.subtitle, cx, y + 22f, 11f, Theme.TEXT, Text.Align.CENTER);
		}
		Draw.rect(g, x + PAD, y + HEADER - 7f, w - PAD * 2f, 1f).gradientH(Colors.withAlpha(0xFFFFFF, 0f), Colors.withAlpha(0xFFFFFF, 0.1f))
			.submit();
		if (m.info.on()) {
			Minecraft mc = Minecraft.getInstance();
			m.line.setLength(0);
			ServerData server = mc.getCurrentServer();
			if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
				m.line.append(mc.getSingleplayerServer().getWorldData().getLevelName());
			} else if (server != null) {
				m.line.append(server.name);
			}
			if (!m.line.isEmpty()) {
				m.line.append("   ·   ");
			}
			Fmt.duration(m.line, Session.seconds()).append(" this session");
			Text.drawMiddle(g, Fonts.MEDIUM, m.line, cx, y + h + 16f, 9f, Theme.TEXT_SECONDARY, Text.Align.CENTER);
		}
	}
}
