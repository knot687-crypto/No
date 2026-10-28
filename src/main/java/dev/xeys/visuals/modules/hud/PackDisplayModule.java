package dev.xeys.visuals.modules.hud;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.xeys.visuals.Xeys;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.hud.HudModule;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.HudStyle;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.render.texture.CpuTexture;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;

/** The resource packs you have enabled, top pack first, with their icons. */
public final class PackDisplayModule extends HudModule {
	private static final float ICON = 22f;

	private final NumberSetting maxPacks = this.add(new NumberSetting("max_packs", "Max Packs", 3f, 1f, 8f, 1f).group("Display"));
	private final BoolSetting icons = this.add(new BoolSetting("icons", "Pack Icons", true).group("Display"));
	private final BoolSetting descriptions = this.add(new BoolSetting("descriptions", "Descriptions", false).group("Display"));

	private final List<Entry> entries = new ArrayList<>();
	private Collection<Pack> lastSelection;
	private boolean lastPreview;
	private int lastMax;

	public PackDisplayModule() {
		super("pack_display", "Pack Display", Icons.LAYERS, "Your active resource packs with their icons", HudPosition.Anchor.BOTTOM_RIGHT, -6f,
			-96f);
	}

	private static final class Entry {
		String title;
		String description;
		CpuTexture icon;
		boolean iconLoaded;
		float titleW;
		float descW;
	}

	@Override
	public boolean hasContent(boolean preview) {
		if (preview) {
			return true;
		}
		for (Pack p : Minecraft.getInstance().getResourcePackRepository().getSelectedPacks()) {
			if (!p.isRequired()) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void onDisable() {
		this.clear();
		this.lastSelection = null;
	}

	private void clear() {
		for (Entry e : this.entries) {
			if (e.icon != null) {
				e.icon.close();
			}
		}
		this.entries.clear();
	}

	/**
	 * Rebuilds the list when the selection changes. Vanilla replaces its selected-pack list whenever the
	 * selection changes, so comparing the list reference is enough and costs nothing per frame.
	 */
	private void refresh(boolean preview) {
		Collection<Pack> selected = Minecraft.getInstance().getResourcePackRepository().getSelectedPacks();
		int max = this.maxPacks.intValue();
		if (selected == this.lastSelection && preview == this.lastPreview && max == this.lastMax) {
			return;
		}
		this.lastSelection = selected;
		this.lastPreview = preview;
		this.lastMax = max;
		List<Pack> ordered = new ArrayList<>(selected);
		List<Pack> picked = new ArrayList<>();
		for (int i = ordered.size() - 1; i >= 0 && picked.size() < max; i--) {
			Pack p = ordered.get(i);
			if (!p.isRequired()) {
				picked.add(p);
			}
		}
		if (picked.isEmpty() && preview) {
			for (Pack p : ordered) {
				if ("vanilla".equals(p.getId())) {
					picked.add(p);
				}
			}
		}
		this.clear();
		for (Pack p : picked) {
			Entry e = new Entry();
			e.title = ChatFormatting.stripFormatting(p.getTitle().getString());
			e.description = ChatFormatting.stripFormatting(p.getDescription().getString()).replace('\n', ' ');
			e.icon = loadIcon(p);
			this.entries.add(e);
		}
	}

	private static CpuTexture loadIcon(Pack pack) {
		try (PackResources resources = pack.open()) {
			IoSupplier<InputStream> supplier = resources.getRootResource("pack.png");
			if (supplier == null) {
				return null;
			}
			try (InputStream in = supplier.get(); NativeImage image = NativeImage.read(in)) {
				CpuTexture tex = new CpuTexture("XEYS pack icon");
				tex.upload(image);
				return tex;
			}
		} catch (Exception e) {
			Xeys.LOG.debug("No icon for pack {}", pack.getId(), e);
			return null;
		}
	}

	@Override
	protected void layout(boolean preview) {
		this.refresh(preview);
		float size = HudStyle.textSize();
		float w = 0f;
		for (Entry e : this.entries) {
			e.titleW = HudStyle.fontStrong().width(e.title, size);
			e.descW = this.descriptions.on() ? Math.min(HudStyle.font().width(e.description, size * 0.84f), 180f) : 0f;
			w = Math.max(w, Math.max(e.titleW, e.descW));
		}
		float pad = HudStyle.pad();
		this.width = pad * 2f + (this.icons.on() ? ICON + 7f : 0f) + Math.min(w, 200f);
		float row = this.rowHeight();
		this.height = pad * 2f + this.entries.size() * row + Math.max(0, this.entries.size() - 1) * 4f;
	}

	private float rowHeight() {
		float size = HudStyle.textSize();
		float text = this.descriptions.on() ? size * 2f + 2f : size;
		return Math.max(this.icons.on() ? ICON : 0f, text);
	}

	@Override
	protected void draw(GuiGraphics g, float partialTick, boolean preview) {
		HudStyle.panel(g, 0f, 0f, this.width, this.height);
		float pad = HudStyle.pad();
		float size = HudStyle.textSize();
		float row = this.rowHeight();
		float y = pad;
		for (Entry e : this.entries) {
			float x = pad;
			if (this.icons.on()) {
				float iy = y + (row - ICON) * 0.5f;
				if (e.icon != null && e.icon.view() != null) {
					Draw.image(g, e.icon.view(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST), x, iy, ICON, ICON, 0f, 0f, 1f, 1f)
						.radius(4f).submit();
				} else {
					Draw.rect(g, x, iy, ICON, ICON, 4f, Colors.withAlpha(0xFFFFFF, 0.06f));
					Text.icon(g, Icons.LAYERS, x + ICON * 0.5f, iy + ICON * 0.5f, ICON * 0.5f, HudStyle.textSecondary());
				}
				Draw.outline(g, x, iy, ICON, ICON, 4f, 0.75f, Colors.withAlpha(0xFFFFFF, 0.1f));
				x += ICON + 7f;
			}
			float maxW = this.width - pad - x;
			float cy = y + row * 0.5f;
			if (this.descriptions.on()) {
				Text.drawTruncated(g, HudStyle.fontStrong(), e.title, x, cy - size * 0.55f - 0.5f, size, HudStyle.text(), maxW);
				Text.drawTruncated(g, HudStyle.font(), e.description, x, cy + size * 0.55f + 0.5f, size * 0.84f, HudStyle.textSecondary(), maxW);
			} else {
				Text.drawTruncated(g, HudStyle.fontStrong(), e.title, x, cy, size, HudStyle.text(), maxW);
			}
			y += row + 4f;
		}
	}
}
