package dev.xeys.visuals.hud;

import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.render.text.Text;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A single-line readout: optional icon, a label and a value. The value is written into a reused
 * StringBuilder at a chosen refresh rate, so steady-state rendering allocates nothing.
 */
public abstract class InfoHudModule extends HudModule {
	protected final BoolSetting showIcon = this.add(new BoolSetting("show_icon", "Icon", true).group("Display"));
	protected final BoolSetting showLabel = this.add(new BoolSetting("show_label", "Label", true).group("Display"));
	protected final StringBuilder value = new StringBuilder(32);
	private final String label;
	private long lastUpdate;
	private final long intervalNanos;

	protected InfoHudModule(String id, String name, char icon, String description, String label, float refreshHz,
							HudPosition.Anchor anchor, float x, float y) {
		super(id, name, icon, description, anchor, x, y);
		this.label = label;
		this.intervalNanos = refreshHz <= 0f ? 0L : (long) (1_000_000_000L / refreshHz);
	}

	/** Writes the current value into {@code out} (already cleared). */
	protected abstract void update(StringBuilder out, boolean preview);

	/** Color of the value text; override for status coloring. */
	protected int valueColor() {
		return HudStyle.text();
	}

	protected String label() {
		return this.label;
	}

	@Override
	protected void layout(boolean preview) {
		long now = System.nanoTime();
		if (this.intervalNanos == 0L || now - this.lastUpdate >= this.intervalNanos || this.value.isEmpty()) {
			this.lastUpdate = now;
			this.value.setLength(0);
			this.update(this.value, preview);
		}
		float size = HudStyle.textSize();
		float pad = HudStyle.pad();
		float w = pad * 2f;
		if (this.showIcon.on()) {
			w += size + 4f;
		}
		if (this.showLabel.on() && !this.label().isEmpty()) {
			w += HudStyle.font().width(this.label(), size) + 4f;
		}
		w += HudStyle.fontStrong().width(this.value, size);
		this.width = w;
		this.height = size + pad * 2f + 1f;
	}

	@Override
	protected void draw(GuiGraphics g, float partialTick, boolean preview) {
		HudStyle.panel(g, 0f, 0f, this.width, this.height);
		this.drawContentOnly(g);
	}

	/** Draws the leading icon in a size x size box whose left edge is {@code x}. */
	protected void drawIcon(GuiGraphics g, float x, float centerY, float size) {
		Text.icon(g, this.icon(), x + size * 0.5f, centerY, size, HudStyle.accent());
	}

	/** Icon, label and value laid out in the top {@code height} pixels, without the panel. */
	protected void drawContentOnly(GuiGraphics g) {
		float size = HudStyle.textSize();
		float x = HudStyle.pad();
		float cy = this.height * 0.5f;
		if (this.showIcon.on()) {
			this.drawIcon(g, x, cy, size);
			x += size + 4f;
		}
		if (this.showLabel.on() && !this.label().isEmpty()) {
			x += HudStyle.label(g, HudStyle.font(), this.label(), x, cy, size, HudStyle.textSecondary(), Text.Align.LEFT) + 4f;
		}
		HudStyle.label(g, HudStyle.fontStrong(), this.value, x, cy, size, this.valueColor(), Text.Align.LEFT);
	}
}
