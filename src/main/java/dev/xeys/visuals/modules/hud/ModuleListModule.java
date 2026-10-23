package dev.xeys.visuals.modules.hud;

import dev.xeys.visuals.anim.Anim;
import dev.xeys.visuals.anim.Easing;
import dev.xeys.visuals.anim.Spring;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.module.ModuleManager;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.hud.HudModule;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.HudStyle;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.render.text.XeysFont;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;

/** Enabled modules as a list that slides entries in and out and keeps itself sorted. */
public final class ModuleListModule extends HudModule {
	public enum Sort {
		LENGTH,
		ALPHABETICAL
	}

	public enum Bar {
		NONE,
		OUTER,
		INNER
	}

	private final EnumSetting<Sort> sort = this.add(new EnumSetting<>("sort", "Sort By", Sort.LENGTH).group("Display"));
	private final EnumSetting<Bar> bar = this.add(new EnumSetting<>("bar", "Accent Bar", Bar.OUTER).describe("A colored edge on each entry")
		.group("Display"));
	private final BoolSetting background = this.add(new BoolSetting("background", "Backgrounds", true).group("Display"));
	private final BoolSetting lowercase = this.add(new BoolSetting("lowercase", "Lowercase", false).group("Display"));
	private final NumberSetting spacing = this.add(new NumberSetting("spacing", "Spacing", 1f, 0f, 6f, 0.5f).unit("px").decimals(1)
		.group("Display"));
	private final BoolSetting hudModules = this.add(new BoolSetting("hud_modules", "Include HUD Elements", false).group("Filter"));
	private final BoolSetting clientModules = this.add(new BoolSetting("client_modules", "Include Client Settings", false).group("Filter"));
	private final ColorSetting textColor = this.add(new ColorSetting("text_color", "Text", 0xFFFFFFFF, ColorSetting.Mode.STATIC).group("Colors"));
	private final ColorSetting barColor = this.add(new ColorSetting("bar_color", "Bar", 0xFFFFFFFF, ColorSetting.Mode.ACCENT).group("Colors"));
	private final NumberSetting wave = this.add(new NumberSetting("wave", "Color Wave", 6f, 0f, 20f, 1f).unit("%")
		.describe("Phase offset between rows for rainbow and pulse colors").group("Colors"));

	private final List<Entry> entries = new ArrayList<>();
	private final List<Entry> order = new ArrayList<>();
	private boolean rightAligned;

	public ModuleListModule() {
		super("module_list", "Module List", Icons.LIST, "Your enabled modules, sorted and animated", HudPosition.Anchor.TOP_RIGHT, -6f, 6f);
	}

	private static final class Entry {
		final Module module;
		String label;
		boolean lower;
		float textW;
		final Anim appear = new Anim(0f);
		final Spring y = new Spring(0f, 380f, 1f);
		boolean placed;

		Entry(Module module) {
			this.module = module;
			this.label = module.name();
		}
	}

	private void ensureEntries() {
		List<Module> all = ModuleManager.all();
		if (this.entries.size() == all.size()) {
			return;
		}
		this.entries.clear();
		this.order.clear();
		for (Module m : all) {
			if (m != this) {
				Entry e = new Entry(m);
				this.entries.add(e);
				this.order.add(e);
			}
		}
	}

	private boolean listed(Module m, boolean preview, boolean fallback) {
		if (!m.isEnabled() || m.isAlwaysOn() || m == this) {
			return false;
		}
		Category c = m.category();
		if (c == Category.HUD) {
			return this.hudModules.on() || fallback;
		}
		return c != Category.CLIENT || this.clientModules.on();
	}

	@Override
	public boolean hasContent(boolean preview) {
		this.ensureEntries();
		for (int i = 0; i < this.entries.size(); i++) {
			if (this.entries.get(i).appear.get() > 0f || this.listed(this.entries.get(i).module, preview, preview)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void layout(boolean preview) {
		this.ensureEntries();
		float size = HudStyle.textSize();
		XeysFont font = HudStyle.font();
		boolean anyListed = false;
		for (int i = 0; i < this.entries.size(); i++) {
			if (this.listed(this.entries.get(i).module, preview, false)) {
				anyListed = true;
				break;
			}
		}
		// In the editor with nothing to list, show HUD elements so the style can be previewed.
		boolean fallback = preview && !anyListed;
		for (int i = 0; i < this.entries.size(); i++) {
			Entry e = this.entries.get(i);
			if (e.lower != this.lowercase.on()) {
				e.lower = this.lowercase.on();
				e.label = e.lower ? e.module.name().toLowerCase(Locale.ROOT) : e.module.name();
			}
			e.textW = font.width(e.label, size);
			boolean show = this.listed(e.module, preview, fallback);
			if (show && e.appear.target() < 1f) {
				e.appear.to(1f, 0.32f, Easing.OUT_CUBIC);
			} else if (!show && e.appear.target() > 0f) {
				e.appear.to(0f, 0.26f, Easing.IN_OUT_CUBIC);
			}
		}
		this.sortOrder();
		float rowH = this.rowHeight();
		float y = 0f;
		float w = 0f;
		for (int i = 0; i < this.order.size(); i++) {
			Entry e = this.order.get(i);
			float a = e.appear.get();
			if (a <= 0f && e.appear.target() <= 0f) {
				e.placed = false;
				continue;
			}
			if (!e.placed) {
				e.y.snap(y);
				e.placed = true;
			}
			e.y.target(y);
			w = Math.max(w, this.rowWidth(e));
			y += (rowH + this.spacing.value()) * Easing.OUT_CUBIC.apply(a);
		}
		this.width = Math.max(w, 40f);
		this.height = Math.max(y - this.spacing.value(), rowH);
		this.rightAligned = this.position.anchor().fx > 0.5f;
	}

	private float rowHeight() {
		return HudStyle.textSize() + 7f;
	}

	private float rowWidth(Entry e) {
		return e.textW + 12f + (this.bar.is(Bar.NONE) ? 0f : 3f);
	}

	private void sortOrder() {
		boolean byName = this.sort.is(Sort.ALPHABETICAL);
		for (int i = 1; i < this.order.size(); i++) {
			Entry e = this.order.get(i);
			int j = i - 1;
			while (j >= 0 && (byName ? this.order.get(j).label.compareToIgnoreCase(e.label) > 0 : this.order.get(j).textW < e.textW)) {
				this.order.set(j + 1, this.order.get(j));
				j--;
			}
			this.order.set(j + 1, e);
		}
	}

	@Override
	protected void draw(GuiGraphics g, float partialTick, boolean preview) {
		float size = HudStyle.textSize();
		float rowH = this.rowHeight();
		float radius = Math.min(HudStyle.radius(), rowH * 0.3f);
		float waveStep = this.wave.value() / 100f;
		int index = 0;
		for (int i = 0; i < this.order.size(); i++) {
			Entry e = this.order.get(i);
			float a = e.appear.get();
			if (a <= 0.005f) {
				continue;
			}
			float ea = Easing.OUT_CUBIC.apply(a);
			float rw = this.rowWidth(e);
			float slide = (1f - ea) * (rw + 10f);
			float x = this.rightAligned ? this.width - rw + slide : -slide;
			float y = e.y.get();
			int tc = Colors.mulAlpha(this.textColor.color(index * waveStep), ea);
			int bc = Colors.mulAlpha(this.barColor.color(index * waveStep), ea);
			if (this.background.on()) {
				HudStyle.panel(g, x, y, rw, rowH, radius);
			}
			boolean outer = this.bar.is(Bar.OUTER);
			if (!this.bar.is(Bar.NONE)) {
				boolean onRight = this.rightAligned == outer;
				float bx = onRight ? x + rw - 3.5f : x + 1.5f;
				Draw.rect(g, bx, y + 3f, 2f, rowH - 6f, 1f, bc);
			}
			float tx = x + 6f + (!this.bar.is(Bar.NONE) && this.rightAligned != outer ? 3f : 0f);
			HudStyle.label(g, HudStyle.font(), e.label, tx, y + rowH * 0.5f, size, tc, Text.Align.LEFT);
			index++;
		}
	}
}
