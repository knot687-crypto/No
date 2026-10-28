package dev.xeys.visuals.modules.hud;

import dev.xeys.visuals.anim.Anim;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.anim.Easing;
import dev.xeys.visuals.anim.Spring;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.hud.HudModule;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.HudStyle;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import dev.xeys.visuals.util.Fmt;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** Active status effects with icons, levels, timers and remaining-time bars; rows animate in and out. */
public final class PotionHudModule extends HudModule {
	public enum Layout {
		DETAILED,
		COMPACT
	}

	public enum Sort {
		DURATION,
		NAME,
		BENEFICIAL_FIRST
	}

	private static PotionHudModule instance;

	private final EnumSetting<Layout> layout = this.add(new EnumSetting<>("layout", "Layout", Layout.DETAILED).group("Display"));
	private final EnumSetting<Sort> sort = this.add(new EnumSetting<>("sort", "Sort By", Sort.DURATION).group("Display"));
	private final BoolSetting bars = this.add(new BoolSetting("bars", "Time Bars", true).describe("Remaining time as a bar")
		.visibleWhen(() -> this.layout.is(Layout.DETAILED)).group("Display"));
	private final BoolSetting effectColors = this.add(new BoolSetting("effect_colors", "Effect Colors", true)
		.describe("Tint bars with each effect's color; accent when off").group("Colors"));
	private final BoolSetting endingPulse = this.add(new BoolSetting("ending_pulse", "Ending Pulse", true)
		.describe("Pulse effects in their last 10 seconds").group("Colors"));
	private final BoolSetting hideVanilla = this.add(new BoolSetting("hide_vanilla", "Hide Vanilla Icons", true)
		.describe("Hide the default effect icons in the top-right corner").group("Display"));

	private final List<Row> rows = new ArrayList<>();
	private final ArrayDeque<Row> free = new ArrayDeque<>();
	private final MobEffectInstance[] demo = new MobEffectInstance[3];

	public PotionHudModule() {
		super("potion_hud", "Potion Effects", Icons.SPARKLES, "Active effects with timers and progress bars", HudPosition.Anchor.TOP_RIGHT,
			-6f, 6f);
		this.enabledByDefault();
		instance = this;
	}

	/** Whether vanilla's top-right effect icons should be skipped this frame. */
	public static boolean hidesVanillaIcons() {
		return instance != null && instance.isEnabled() && instance.hideVanilla.on();
	}

	private static final class Row {
		Holder<MobEffect> effect;
		Identifier sprite;
		String name;
		int amplifier;
		int color;
		boolean beneficial;
		boolean infinite;
		int duration;
		int maxDuration;
		boolean present;
		final Anim appear = new Anim(0f);
		final Spring y = new Spring(0f, 420f, 1f);
		final StringBuilder time = new StringBuilder(10);
		float nameW;
		float timeW;
		boolean placed;
	}

	@Override
	public boolean hasContent(boolean preview) {
		if (preview || !this.rows.isEmpty()) {
			return true;
		}
		Player p = Minecraft.getInstance().player;
		if (p != null) {
			for (MobEffectInstance inst : p.getActiveEffects()) {
				if (inst.showIcon()) {
					return true;
				}
			}
		}
		return false;
	}

	private Iterable<MobEffectInstance> effects(boolean preview) {
		Player p = Minecraft.getInstance().player;
		if (p != null && !p.getActiveEffects().isEmpty()) {
			return p.getActiveEffects();
		}
		if (!preview) {
			return List.of();
		}
		// Preview: a believable set whose timers count down.
		int t = (int) ((Clock.time() * 20.0) % 2400.0);
		this.demo[0] = new MobEffectInstance(MobEffects.SPEED, 3600 - t, 1);
		this.demo[1] = new MobEffectInstance(MobEffects.STRENGTH, 2600 - t, 0);
		this.demo[2] = new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0);
		return List.of(this.demo);
	}

	private void sync(boolean preview) {
		for (int i = 0; i < this.rows.size(); i++) {
			this.rows.get(i).present = false;
		}
		for (MobEffectInstance inst : this.effects(preview)) {
			if (!inst.showIcon()) {
				continue;
			}
			Row row = this.find(inst.getEffect());
			if (row == null) {
				row = this.free.isEmpty() ? new Row() : this.free.pop();
				row.effect = inst.getEffect();
				row.sprite = Gui.getMobEffectSprite(inst.getEffect());
				row.amplifier = -1;
				row.maxDuration = 0;
				row.placed = false;
				MobEffect effect = inst.getEffect().value();
				row.beneficial = effect.isBeneficial();
				int c = 0xFF000000 | effect.getColor();
				// Keep very dark effect colors (Blindness, Darkness) visible on a dark panel.
				row.color = Colors.luminance(c) < 0.22f ? Colors.lerpOklab(c, 0xFFE6E6EA, 0.45f) : c;
				row.appear.restart(0f, 1f, 0.35f, Easing.OUT_CUBIC);
				this.rows.add(row);
			}
			row.present = true;
			if (row.amplifier != inst.getAmplifier()) {
				row.amplifier = inst.getAmplifier();
				String base = inst.getEffect().value().getDisplayName().getString();
				row.name = row.amplifier > 0 ? base + " " + Fmt.roman(row.amplifier + 1) : base;
				row.maxDuration = 0;
			}
			row.infinite = inst.isInfiniteDuration();
			row.duration = inst.getDuration();
			if (row.duration > row.maxDuration) {
				row.maxDuration = row.duration;
			}
			if (row.appear.target() < 1f) {
				row.appear.to(1f, 0.3f, Easing.OUT_CUBIC);
			}
		}
		for (int i = this.rows.size() - 1; i >= 0; i--) {
			Row row = this.rows.get(i);
			if (!row.present) {
				if (row.appear.target() > 0f) {
					row.appear.to(0f, 0.28f, Easing.IN_OUT_CUBIC);
				} else if (row.appear.get() <= 0f) {
					this.rows.remove(i);
					this.free.push(row);
				}
			}
		}
		this.sortRows();
	}

	private Row find(Holder<MobEffect> effect) {
		for (int i = 0; i < this.rows.size(); i++) {
			Row r = this.rows.get(i);
			if (r.effect.equals(effect)) {
				return r;
			}
		}
		return null;
	}

	private int compare(Row a, Row b) {
		return switch (this.sort.get()) {
			case DURATION -> Integer.compare(a.infinite ? Integer.MAX_VALUE : a.duration, b.infinite ? Integer.MAX_VALUE : b.duration);
			case NAME -> a.name.compareToIgnoreCase(b.name);
			case BENEFICIAL_FIRST -> a.beneficial == b.beneficial ? a.name.compareToIgnoreCase(b.name) : (a.beneficial ? -1 : 1);
		};
	}

	/** Stable insertion sort: tiny lists, no allocation, rows keep their order on ties. */
	private void sortRows() {
		for (int i = 1; i < this.rows.size(); i++) {
			Row r = this.rows.get(i);
			int j = i - 1;
			while (j >= 0 && this.compare(this.rows.get(j), r) > 0) {
				this.rows.set(j + 1, this.rows.get(j));
				j--;
			}
			this.rows.set(j + 1, r);
		}
	}

	private float rowHeight() {
		return this.layout.is(Layout.DETAILED) ? HudStyle.textSize() * 2f + 11f : HudStyle.textSize() + 9f;
	}

	@Override
	protected void layout(boolean preview) {
		this.sync(preview);
		float size = HudStyle.textSize();
		float pad = HudStyle.pad();
		boolean detailed = this.layout.is(Layout.DETAILED);
		float icon = detailed ? 18f : 14f;
		float w = 0f;
		float y = pad;
		float rh = this.rowHeight();
		for (int i = 0; i < this.rows.size(); i++) {
			Row r = this.rows.get(i);
			r.time.setLength(0);
			if (!r.infinite) {
				Fmt.duration(r.time, Math.max(0, r.duration) / 20L);
			}
			r.nameW = HudStyle.font().width(r.name, size);
			r.timeW = r.infinite ? size : HudStyle.fontStrong().width(r.time, detailed ? size * 0.86f : size);
			float rowW = detailed ? icon + 8f + Math.max(r.nameW, r.timeW + (this.bars.on() ? 50f : 0f)) : icon + 7f + r.nameW + 12f + r.timeW;
			w = Math.max(w, rowW);
			if (!r.placed) {
				r.y.snap(y);
				r.placed = true;
			}
			r.y.target(y);
			y += rh * Easing.OUT_CUBIC.apply(r.appear.get());
		}
		this.width = Math.max(w, detailed ? 96f : 70f) + pad * 2f;
		this.height = Math.max(y - pad, 0f) + pad * 2f;
	}

	private float pulse(Row r) {
		if (!this.endingPulse.on() || r.infinite || r.duration > 200) {
			return 1f;
		}
		return 0.45f + 0.55f * (float) (0.5 + 0.5 * Math.cos(Clock.time() * Math.PI * 2.0));
	}

	@Override
	protected void draw(GuiGraphics g, float partialTick, boolean preview) {
		HudStyle.panel(g, 0f, 0f, this.width, this.height);
		float size = HudStyle.textSize();
		float pad = HudStyle.pad();
		boolean detailed = this.layout.is(Layout.DETAILED);
		float icon = detailed ? 18f : 14f;
		float rh = this.rowHeight();
		Draw.pushClip(g, 0f, 0f, this.width, this.height);
		for (int i = 0; i < this.rows.size(); i++) {
			Row r = this.rows.get(i);
			float a = r.appear.get();
			if (a <= 0.01f) {
				continue;
			}
			float ry = r.y.get();
			float slide = (1f - Easing.OUT_CUBIC.apply(a)) * 10f;
			float x = pad + slide;
			float pulse = this.pulse(r);
			int barColor = this.effectColors.on() ? r.color : HudStyle.accent();
			float iconY = ry + (rh - icon) * 0.5f;

			g.pose().pushMatrix();
			g.pose().translate(x, iconY);
			g.pose().scale(icon / 18f, icon / 18f);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, r.sprite, 0, 0, 18, 18, a * pulse);
			g.pose().popMatrix();

			float tx = x + icon + (detailed ? 8f : 7f);
			int timeColor = r.duration <= 200 && !r.infinite && this.endingPulse.on() ? Colors.mulAlpha(Theme.WARNING, a)
				: Colors.mulAlpha(HudStyle.textSecondary(), a);
			if (detailed) {
				float nameY = ry + 4.5f + size * 0.5f;
				float timeY = nameY + size + 2f;
				float ts = size * 0.86f;
				HudStyle.label(g, HudStyle.font(), r.name, tx, nameY, size, Colors.mulAlpha(HudStyle.text(), a), Text.Align.LEFT);
				if (r.infinite) {
					Text.icon(g, Icons.INFINITY, tx + ts * 0.5f, timeY, ts, timeColor);
				} else {
					HudStyle.label(g, HudStyle.fontStrong(), r.time, tx, timeY, ts, timeColor, Text.Align.LEFT);
				}
				if (this.bars.on()) {
					float bx = tx + r.timeW + 7f;
					float bw = this.width - pad - bx;
					if (bw > 6f) {
						float f = r.infinite || r.maxDuration <= 0 ? 1f : Math.max(0f, Math.min(r.duration / (float) r.maxDuration, 1f));
						Draw.rect(g, bx, timeY - 1f, bw, 2f, 1f, Colors.withAlpha(0xFFFFFF, 0.09f * a));
						Draw.rect(g, bx, timeY - 1f, Math.max(2f, bw * f), 2f, 1f, Colors.mulAlpha(barColor, a * (0.55f + 0.45f * pulse)));
					}
				}
			} else {
				float cy = ry + rh * 0.5f;
				HudStyle.label(g, HudStyle.font(), r.name, tx, cy, size, Colors.mulAlpha(HudStyle.text(), a), Text.Align.LEFT);
				if (r.infinite) {
					Text.icon(g, Icons.INFINITY, this.width - pad - size * 0.5f, cy, size, timeColor);
				} else {
					HudStyle.label(g, HudStyle.fontStrong(), r.time, this.width - pad, cy, size, timeColor, Text.Align.RIGHT);
				}
			}
		}
		Draw.popClip();
	}
}
