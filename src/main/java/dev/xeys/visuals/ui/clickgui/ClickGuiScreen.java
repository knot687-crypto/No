package dev.xeys.visuals.ui.clickgui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.xeys.visuals.anim.Anim;
import dev.xeys.visuals.anim.Easing;
import dev.xeys.visuals.anim.Spring;
import dev.xeys.visuals.core.config.ConfigManager;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.module.ModuleManager;
import dev.xeys.visuals.hud.HudEditorScreen;
import dev.xeys.visuals.modules.client.InterfaceModule;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.ui.clickgui.control.Popover;
import dev.xeys.visuals.ui.core.ScrollState;
import dev.xeys.visuals.ui.core.TextField;
import dev.xeys.visuals.ui.core.UiScale;
import dev.xeys.visuals.ui.core.UiSounds;
import dev.xeys.visuals.util.Colors;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * The XEYS visuals menu: a frosted glass window with a category sidebar, a searchable module grid and
 * a settings panel per module, plus profiles and the HUD editor entry point.
 */
public final class ClickGuiScreen extends Screen implements GuiContext {
	private static final float SIDEBAR = 224f;
	private static final float TOPBAR = 78f;
	private static final float GAP = 12f;
	private static final float CATEGORY_ROW = 40f;
	private static Category lastCategory = Category.HUD;

	private final @Nullable Screen parent;
	private final Anim open = new Anim(0f);
	private boolean closing;
	private Category category = lastCategory;
	private final Spring pillY = new Spring(0f, 300f, 0.86f);
	private boolean pillPlaced;
	private final Anim gridAppear = new Anim(0f);
	private final ScrollState gridScroll = new ScrollState();
	private final TextField search = new TextField().placeholder("Search modules").icon(Icons.SEARCH).maxLength(40);
	private final Map<Module, ModuleCard> cards = new IdentityHashMap<>();
	private final List<ModuleCard> visible = new ArrayList<>();
	private @Nullable SettingsPanel settings;
	private @Nullable Popover popover;
	private @Nullable InputCapture capture;
	private String lastQuery = "";
	private boolean draggingScrollbar;

	// Layout (design units), refreshed every frame.
	private float wx;
	private float wy;
	private float ww;
	private float wh;
	private int hoveredCategory = -1;
	private boolean hudHovered;
	private boolean profileHovered;
	private float profileY;

	private @Nullable Module pendingModule;

	public ClickGuiScreen(@Nullable Screen parent) {
		super(Component.literal("XEYS Visuals"));
		this.parent = parent;
		this.search.onChange(q -> this.rebuild());
	}

	/** Opens directly on a module's settings (e.g. from the HUD editor). */
	public static ClickGuiScreen forModule(@Nullable Screen parent, Module module) {
		ClickGuiScreen screen = new ClickGuiScreen(parent);
		screen.category = module.category();
		screen.pendingModule = module;
		return screen;
	}

	@Override
	protected void init() {
		boolean calm = InterfaceModule.reducedMotion();
		this.open.restart(0f, 1f, calm ? 0.2f : 0.42f, calm ? Easing.OUT_CUBIC : Easing.OUT_BACK_SOFT);
		this.closing = false;
		this.rebuild();
		if (this.pendingModule != null) {
			this.openSettings(this.pendingModule);
			this.pendingModule = null;
		}
	}

	// ------------------------------------------------------------------ model

	private void rebuild() {
		String query = this.search.value().trim().toLowerCase(Locale.ROOT);
		boolean changed = !query.equals(this.lastQuery);
		this.lastQuery = query;
		this.visible.clear();
		List<Module> source = query.isEmpty() ? ModuleManager.byCategory(this.category) : ModuleManager.all();
		for (Module m : source) {
			if (!query.isEmpty() && !matches(m, query)) {
				continue;
			}
			ModuleCard card = this.cards.computeIfAbsent(m, mod -> new ModuleCard(mod, this::openSettings));
			card.showCategory = !query.isEmpty();
			this.visible.add(card);
		}
		if (changed || query.isEmpty()) {
			this.gridScroll.reset();
			this.gridAppear.restart(0f, 1f, InterfaceModule.reducedMotion() ? 0.15f : 0.5f, Easing.LINEAR);
		}
	}

	/** Every query word must appear in the module's name, description or category. */
	private static boolean matches(Module m, String query) {
		String hay = m.searchText();
		for (String word : query.split("\\s+")) {
			if (!word.isEmpty() && !hay.contains(word)) {
				return false;
			}
		}
		return true;
	}

	private void selectCategory(Category c) {
		if (c == this.category && this.search.value().isEmpty() && this.settings == null) {
			return;
		}
		this.category = c;
		lastCategory = c;
		this.search.setValue("");
		this.search.setFocused(false);
		if (this.settings != null) {
			this.settings.close();
		}
		this.lastQuery = "\u0000";
		this.rebuild();
		UiSounds.soft();
	}

	private void openSettings(Module m) {
		this.settings = new SettingsPanel(m, this);
		this.search.setFocused(false);
	}

	// ------------------------------------------------------------------ GuiContext

	@Override
	public void openPopover(Popover popover) {
		this.popover = popover;
	}

	@Override
	public void closePopover() {
		if (this.popover != null) {
			this.popover.close();
		}
	}

	@Override
	public @Nullable Popover popover() {
		return this.popover;
	}

	@Override
	public void captureInput(@Nullable InputCapture capture) {
		this.capture = capture;
	}

	// ------------------------------------------------------------------ rendering

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		if (this.minecraft.level == null) {
			this.renderPanorama(g, delta);
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		float a = this.open.get();
		if (this.closing && a <= 0.001f) {
			this.minecraft.setScreen(this.parent);
			return;
		}
		float mx = UiScale.toDesign(mouseX);
		float my = UiScale.toDesign(mouseY);
		float W = UiScale.width();
		float H = UiScale.height();
		this.ww = Math.max(720f, Math.min(1040f, W - 96f));
		this.wh = Math.max(500f, Math.min(680f, H - 96f));
		this.wx = (W - this.ww) * 0.5f;
		this.wy = (H - this.wh) * 0.5f;

		UiScale.push(g);
		// Dim and vignette the world behind for focus.
		float fade = Math.min(1f, a);
		Draw.rect(g, 0f, 0f, W, H).color(Colors.withAlpha(0x000000, 0.38f * fade)).submit();

		float s = 0.955f + 0.045f * a;
		g.pose().pushMatrix();
		g.pose().translate(this.wx + this.ww * 0.5f, this.wy + this.wh * 0.5f);
		g.pose().scale(s, s);
		g.pose().translate(-(this.wx + this.ww * 0.5f), -(this.wy + this.wh * 0.5f));

		Draw.shadow(g, this.wx, this.wy, this.ww, this.wh, Theme.R_XL, 0f, 24f, 70f, -6f, Colors.withAlpha(0x000000, 0.7f * fade));
		Draw.glass(g, this.wx, this.wy, this.ww, this.wh, 0xD2060607).radius(Theme.R_XL).border(1f, Theme.GLASS_BORDER).borderFade(0.3f)
			.innerGlow(26f, 0.35f).alpha(fade).submit();

		boolean modal = this.popover != null;
		float imx = modal ? -1e4f : mx;
		float imy = modal ? -1e4f : my;
		this.renderSidebar(g, imx, imy, fade);
		this.renderContent(g, imx, imy, fade);

		if (this.popover != null) {
			if (this.popover.closed()) {
				this.popover = null;
			} else {
				this.popover.render(g, mx, my);
			}
		}
		g.pose().popMatrix();
		UiScale.pop(g);
	}

	private void renderSidebar(GuiGraphics g, float mx, float my, float a) {
		float x = this.wx;
		float y = this.wy;
		// Sidebar surface with a hairline divider.
		Draw.rect(g, x, y, SIDEBAR, this.wh).radii(Theme.R_XL, 0f, 0f, Theme.R_XL).color(Colors.withAlpha(0x000000, 0.22f * a)).submit();
		Draw.rect(g, x + SIDEBAR - 1f, y + 18f, 1f, this.wh - 36f).color(Colors.mulAlpha(Theme.HAIRLINE, a)).submit();

		// Wordmark.
		float lx = x + 26f;
		float ly = y + 40f;
		Text.drawBaseline(g, Fonts.DISPLAY, "XEYS", lx, ly + Fonts.DISPLAY.capHeight(21f) * 0.5f, 21f, Colors.mulAlpha(0xFFE3E7EF, a), 0.2f, 0f);
		float mw = Fonts.DISPLAY.width("XEYS", 21f, 0.2f);
		Draw.circle(g, lx + mw + 9f, ly + 6f, 2.6f, Colors.mulAlpha(Theme.accent(), a));
		Text.drawMiddle(g, Fonts.SEMIBOLD, "VISUALS", lx + 1f, ly + 19f, 8.5f, Colors.mulAlpha(Theme.TEXT_MUTED, a), Text.Align.LEFT, 0.42f);

		// Categories.
		float listY = y + 92f;
		Category[] cats = Category.values();
		this.hoveredCategory = -1;
		int selected = this.search.value().isEmpty() ? this.category.ordinal() : -1;
		float targetPill = listY + Math.max(selected, 0) * CATEGORY_ROW;
		if (!this.pillPlaced) {
			this.pillY.snap(targetPill);
			this.pillPlaced = true;
		}
		this.pillY.target(targetPill);
		if (selected >= 0) {
			float py = this.pillY.get();
			Draw.rect(g, x + 14f, py, SIDEBAR - 28f, CATEGORY_ROW - 4f).radius(11f)
				.gradientH(Colors.withAlpha(Theme.accent(), 0.20f * a), Colors.withAlpha(Theme.accent(), 0.06f * a))
				.border(1f, Colors.withAlpha(Theme.accent(), 0.28f * a)).submit();
			Draw.rect(g, x + 14f, py + 10f, 3f, CATEGORY_ROW - 24f).pill().color(Colors.mulAlpha(Theme.accent(), a)).submit();
		}
		for (int i = 0; i < cats.length; i++) {
			Category c = cats[i];
			float ry = listY + i * CATEGORY_ROW;
			boolean hov = mx >= x + 14f && mx < x + SIDEBAR - 14f && my >= ry && my < ry + CATEGORY_ROW - 4f;
			if (hov) {
				this.hoveredCategory = i;
				g.requestCursor(CursorTypes.POINTING_HAND);
				if (i != selected) {
					Draw.rect(g, x + 14f, ry, SIDEBAR - 28f, CATEGORY_ROW - 4f, 11f, Colors.withAlpha(0xFFFFFF, 0.035f * a));
				}
			}
			boolean sel = i == selected;
			float cy = ry + (CATEGORY_ROW - 4f) * 0.5f;
			Text.icon(g, c.icon(), x + 38f, cy, 16f, Colors.mulAlpha(sel ? Theme.accent() : (hov ? Theme.TEXT : Theme.TEXT_MUTED), a));
			Text.drawMiddle(g, Fonts.MEDIUM, c.title(), x + 58f, cy, 13.5f, Colors.mulAlpha(sel ? Theme.TEXT : (hov ? Theme.TEXT : Theme.TEXT_SECONDARY), a), Text.Align.LEFT);
			int on = ModuleManager.enabledCount(c);
			if (on > 0 && c != Category.CLIENT) {
				String n = Integer.toString(on);
				float bw = Math.max(20f, Fonts.SEMIBOLD.width(n, 10.5f) + 12f);
				float bx = x + SIDEBAR - 28f - bw;
				Draw.rect(g, bx, cy - 9f, bw, 18f).pill().color(Colors.mulAlpha(sel ? Theme.accentSoft() : 0x12FFFFFF, a)).submit();
				Text.drawMiddle(g, Fonts.SEMIBOLD, n, bx + bw * 0.5f, cy, 10.5f, Colors.mulAlpha(sel ? Theme.accentText() : Theme.TEXT_MUTED, a), Text.Align.CENTER);
			}
		}

		// Bottom: HUD editor and profile.
		float by = y + this.wh - 108f;
		Draw.rect(g, x + 20f, by - 12f, SIDEBAR - 40f, 1f).color(Colors.mulAlpha(Theme.HAIRLINE, a)).submit();
		this.hudHovered = mx >= x + 14f && mx < x + SIDEBAR - 14f && my >= by && my < by + 40f;
		this.sidebarButton(g, x + 14f, by, SIDEBAR - 28f, Icons.MOVE, "Edit HUD Layout", null, this.hudHovered, a);
		this.profileY = by + 46f;
		this.profileHovered = mx >= x + 14f && mx < x + SIDEBAR - 14f && my >= this.profileY && my < this.profileY + 40f;
		this.sidebarButton(g, x + 14f, this.profileY, SIDEBAR - 28f, Icons.FOLDER, ConfigManager.activeProfile(), "Profile", this.profileHovered, a);
		if (this.hudHovered || this.profileHovered) {
			g.requestCursor(CursorTypes.POINTING_HAND);
		}
	}

	private void sidebarButton(GuiGraphics g, float x, float y, float w, char icon, String label, @Nullable String caption, boolean hov, float a) {
		Draw.rect(g, x, y, w, 40f).radius(11f).color(Colors.mulAlpha(hov ? 0xFF1C1C22 : 0xFF141418, a))
			.border(1f, Colors.mulAlpha(hov ? Theme.accent(0.4f) : 0x12FFFFFF, a)).submit();
		Text.icon(g, icon, x + 22f, y + 20f, 15f, Colors.mulAlpha(hov ? Theme.accent() : Theme.TEXT_SECONDARY, a));
		if (caption == null) {
			Text.drawMiddle(g, Fonts.MEDIUM, label, x + 42f, y + 20f, 12.5f, Colors.mulAlpha(Theme.TEXT, a), Text.Align.LEFT);
		} else {
			Text.drawMiddle(g, Fonts.MEDIUM, caption, x + 42f, y + 13.5f, 9.5f, Colors.mulAlpha(Theme.TEXT_MUTED, a), Text.Align.LEFT);
			Text.drawTruncated(g, Fonts.SEMIBOLD, label, x + 42f, y + 27f, 12f, Colors.mulAlpha(Theme.TEXT, a), w - 70f);
			Text.icon(g, Icons.CHEVRON_UP, x + w - 18f, y + 20f, 13f, Colors.mulAlpha(Theme.TEXT_MUTED, a));
		}
	}

	private void renderContent(GuiGraphics g, float mx, float my, float a) {
		float cx = this.wx + SIDEBAR + 26f;
		float cw = this.ww - SIDEBAR - 52f;
		float top = this.wy;

		SettingsPanel panel = this.settings;
		if (panel != null && panel.closed()) {
			this.settings = null;
			panel = null;
		}
		float panelOpen = panel == null ? 0f : panel.open.get();
		float gridAlpha = a * (1f - panelOpen);

		if (gridAlpha > 0.001f) {
			boolean searching = !this.search.value().isEmpty();
			String title = searching ? "Search" : this.category.title();
			int total = this.visible.size();
			int enabled = 0;
			for (ModuleCard c : this.visible) {
				if (c.module.isEnabled() && !c.module.isAlwaysOn()) {
					enabled++;
				}
			}
			String sub = searching ? total + (total == 1 ? " result" : " results")
				: this.category.subtitle() + "  ·  " + total + " modules" + (enabled > 0 ? ", " + enabled + " on" : "");
			Text.drawMiddle(g, Fonts.SEMIBOLD, title, cx, top + 36f, 21f, Colors.mulAlpha(Theme.TEXT, gridAlpha), Text.Align.LEFT);
			Text.drawTruncated(g, Fonts.REGULAR, sub, cx, top + 58f, 11.5f, Colors.mulAlpha(Theme.TEXT_MUTED, gridAlpha), cw - 290f);
			float sw = 264f;
			this.search.bounds(cx + cw - sw, top + 24f, sw, 36f);
			g.pose().pushMatrix();
			this.search.render(g, mx, my);
			g.pose().popMatrix();

			this.renderGrid(g, mx, my, cx, top + TOPBAR + 10f, cw, this.wh - TOPBAR - 10f - 18f, gridAlpha);
		}
		if (panel != null) {
			panel.bounds(cx, top + 8f, cw, this.wh - 26f);
			panel.render(g, panel.closing() ? -1e4f : mx, my);
		}
	}

	private void renderGrid(GuiGraphics g, float mx, float my, float x, float y, float w, float h, float a) {
		int cols = w >= 600f ? 2 : 1;
		float cardW = (w - GAP * (cols - 1)) / cols;
		int rows = (this.visible.size() + cols - 1) / cols;
		float contentH = rows * (ModuleCard.HEIGHT + GAP);
		this.gridScroll.setExtent(h, contentH);
		float off = this.gridScroll.offset();
		boolean inside = mx >= x && mx <= x + w && my >= y && my <= y + h;
		Draw.pushClip(g, x - 20f, y - 2f, x + w + 20f, y + h, 26f);
		float appear = this.gridAppear.get();
		for (int i = 0; i < this.visible.size(); i++) {
			ModuleCard card = this.visible.get(i);
			int col = i % cols;
			int row = i / cols;
			card.bounds(x + col * (cardW + GAP), y + 2f + row * (ModuleCard.HEIGHT + GAP) - off, cardW, ModuleCard.HEIGHT);
			if (card.y + card.h < y - 10f || card.y > y + h + 10f) {
				continue;
			}
			float t = Math.max(0f, Math.min(1f, appear * 2.2f - Math.min(i, 12) * 0.07f));
			card.appear = Easing.OUT_CUBIC.apply(t) * a;
			card.render(g, inside ? mx : -1e4f, inside ? my : -1e4f);
		}
		Draw.popClip();
		if (this.visible.isEmpty()) {
			Text.icon(g, Icons.SEARCH, x + w * 0.5f, y + h * 0.4f - 18f, 26f, Colors.mulAlpha(Theme.TEXT_FAINT, a));
			Text.drawMiddle(g, Fonts.MEDIUM, "No modules match “" + this.search.value() + "”", x + w * 0.5f, y + h * 0.4f + 16f, 13f,
				Colors.mulAlpha(Theme.TEXT_MUTED, a), Text.Align.CENTER);
		}
		this.gridScroll.drawBar(g, x + w + 14f, y, this.gridScroll.barContains(x + w + 14f, y, mx, my));
		this.gridX = x;
		this.gridY = y;
		this.gridW = w;
		this.gridH = h;
	}

	private float gridX;
	private float gridY;
	private float gridW;
	private float gridH;

	// ------------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.closing) {
			return true;
		}
		float mx = UiScale.toDesign(event.x());
		float my = UiScale.toDesign(event.y());
		int button = event.button();
		if (this.capture != null) {
			this.capture.onMouse(button);
			return true;
		}
		if (this.popover != null && !this.popover.closing()) {
			if (!this.popover.mouseClicked(mx, my, button)) {
				this.popover.close();
			}
			return true;
		}
		if (mx < this.wx || mx > this.wx + this.ww || my < this.wy || my > this.wy + this.wh) {
			return true;
		}
		if (this.hoveredCategory >= 0) {
			this.selectCategory(Category.values()[this.hoveredCategory]);
			return true;
		}
		if (this.hudHovered) {
			UiSounds.click();
			this.minecraft.setScreen(new HudEditorScreen(this));
			return true;
		}
		if (this.profileHovered) {
			UiSounds.click();
			ProfilesPopover pop = new ProfilesPopover();
			pop.x = this.wx + 14f;
			pop.y = this.profileY - pop.h - 8f;
			this.openPopover(pop);
			return true;
		}
		if (this.settings != null && !this.settings.closing()) {
			return this.settings.mouseClicked(mx, my, button);
		}
		boolean onSearch = this.search.mouseClicked(mx, my, button);
		this.search.setFocused(onSearch);
		if (onSearch) {
			return true;
		}
		if (this.gridScroll.barContains(this.gridX + this.gridW + 14f, this.gridY, mx, my)) {
			this.draggingScrollbar = true;
			this.gridScroll.beginDrag(my);
			return true;
		}
		if (mx >= this.gridX && mx <= this.gridX + this.gridW && my >= this.gridY && my <= this.gridY + this.gridH) {
			for (ModuleCard card : this.visible) {
				if (card.mouseClicked(mx, my, button)) {
					return true;
				}
			}
		}
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		float mx = UiScale.toDesign(event.x());
		float my = UiScale.toDesign(event.y());
		int button = event.button();
		if (this.draggingScrollbar) {
			this.draggingScrollbar = false;
			this.gridScroll.endDrag();
			return true;
		}
		if (this.popover != null) {
			this.popover.mouseReleased(mx, my, button);
		}
		this.search.mouseReleased(mx, my, button);
		if (this.settings != null) {
			this.settings.mouseReleased(mx, my, button);
		}
		for (ModuleCard card : this.visible) {
			card.mouseReleased(mx, my, button);
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		float mx = UiScale.toDesign(event.x());
		float my = UiScale.toDesign(event.y());
		if (this.draggingScrollbar) {
			this.gridScroll.drag(my);
			return true;
		}
		if (this.popover != null && this.popover.mouseDragged(mx, my, event.button(), (float) dx, (float) dy)) {
			return true;
		}
		if (this.search.mouseDragged(mx, my, event.button(), (float) dx, (float) dy)) {
			return true;
		}
		return this.settings != null && this.settings.mouseDragged(mx, my, event.button(), (float) dx, (float) dy);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
		float mx = UiScale.toDesign(x);
		float my = UiScale.toDesign(y);
		if (this.popover != null) {
			return true;
		}
		if (this.settings != null && !this.settings.closing()) {
			return this.settings.mouseScrolled(mx, my, (float) vertical);
		}
		this.gridScroll.scroll((float) vertical);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (this.capture != null) {
			this.capture.onKey(key);
			return true;
		}
		if (this.popover != null && !this.popover.closing()) {
			if (key == InputConstants.KEY_ESCAPE) {
				this.popover.close();
				return true;
			}
			return this.popover.keyPressed(key, event.scancode(), event.modifiers());
		}
		boolean ctrl = (event.modifiers() & (InputConstants.MOD_CONTROL | InputConstants.MOD_SUPER)) != 0;
		if (key == InputConstants.KEY_F && ctrl) {
			if (this.settings != null) {
				this.settings.close();
			}
			this.search.setFocused(true);
			return true;
		}
		if (this.settings != null && !this.settings.closing()) {
			if (this.settings.keyPressed(key, event.scancode(), event.modifiers())) {
				return true;
			}
			if (key == InputConstants.KEY_ESCAPE) {
				this.settings.close();
				return true;
			}
			return false;
		}
		if (this.search.isFocused()) {
			if (key == InputConstants.KEY_ESCAPE) {
				if (!this.search.value().isEmpty()) {
					this.search.setValue("");
					this.rebuild();
				} else {
					this.search.setFocused(false);
				}
				return true;
			}
			if (this.search.keyPressed(key, event.scancode(), event.modifiers())) {
				return true;
			}
		}
		if (key == InputConstants.KEY_ESCAPE) {
			this.onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (this.capture != null) {
			return true;
		}
		if (this.popover != null) {
			return this.popover.charTyped(event.codepoint(), event.modifiers());
		}
		if (this.settings != null && !this.settings.closing()) {
			return this.settings.charTyped(event.codepoint(), event.modifiers());
		}
		if (!this.search.isFocused() && event.codepoint() > 32) {
			// Typing anywhere starts a search.
			this.search.setFocused(true);
		}
		return this.search.charTyped(event.codepoint(), event.modifiers());
	}

	@Override
	public void onClose() {
		if (!this.closing) {
			this.closing = true;
			this.open.to(0f, 0.16f, Easing.OUT_CUBIC);
			ConfigManager.saveIfDirty();
		}
	}

	@Override
	public void removed() {
		ConfigManager.saveIfDirty();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
