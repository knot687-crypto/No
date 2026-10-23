package dev.xeys.visuals.modules.hud;

import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.hud.HudItems;
import dev.xeys.visuals.hud.HudModule;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.HudStyle;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Worn armor (and optionally held items) with durability readouts and bars. */
public final class ArmorHudModule extends HudModule {
	public enum Layout {
		VERTICAL,
		HORIZONTAL
	}

	public enum Readout {
		PERCENT,
		REMAINING,
		NONE
	}

	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
		EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
	private static final float ICON = 16f;
	private static ItemStack[] sample;

	private final EnumSetting<Layout> layout = this.add(new EnumSetting<>("layout", "Layout", Layout.VERTICAL).group("Display"));
	private final EnumSetting<Readout> readout = this.add(new EnumSetting<>("readout", "Durability Text", Readout.PERCENT).group("Display"));
	private final BoolSetting bars = this.add(new BoolSetting("bars", "Durability Bars", true).group("Display"));
	private final BoolSetting hands = this.add(new BoolSetting("hands", "Held Items", false).describe("Also show your main and off hand")
		.group("Display"));
	private final BoolSetting healthColors = this.add(new BoolSetting("health_colors", "Wear Colors", true)
		.describe("Bars shift from green to red as items wear; accent when off").group("Colors"));
	private final BoolSetting lowWarning = this.add(new BoolSetting("low_warning", "Low Warning", true)
		.describe("Pulse items below 10% durability").group("Colors"));

	private final ItemStack[] stacks = new ItemStack[SLOTS.length];
	private final float[] fractions = new float[SLOTS.length];
	private final StringBuilder[] texts = new StringBuilder[SLOTS.length];
	private int count;
	private float columnW;

	public ArmorHudModule() {
		super("armor_hud", "Armor HUD", Icons.SHIELD, "Your armor with durability bars", HudPosition.Anchor.BOTTOM_RIGHT, -6f, -6f);
		this.enabledByDefault();
		for (int i = 0; i < this.texts.length; i++) {
			this.texts[i] = new StringBuilder(8);
		}
	}

	private static ItemStack[] sample() {
		if (sample == null) {
			ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
			chest.setDamageValue(chest.getMaxDamage() * 2 / 5);
			ItemStack boots = new ItemStack(Items.NETHERITE_BOOTS);
			boots.setDamageValue(boots.getMaxDamage() * 19 / 20);
			ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
			sword.setDamageValue(sword.getMaxDamage() / 8);
			sample = new ItemStack[] {new ItemStack(Items.DIAMOND_HELMET), chest, new ItemStack(Items.IRON_LEGGINGS), boots, sword,
				new ItemStack(Items.TORCH, 48)};
		}
		return sample;
	}

	@Override
	public boolean hasContent(boolean preview) {
		this.collect(preview);
		return this.count > 0;
	}

	private void collect(boolean preview) {
		Player p = Minecraft.getInstance().player;
		int slots = this.hands.on() ? SLOTS.length : 4;
		this.count = 0;
		boolean any = false;
		if (p != null) {
			for (int i = 0; i < slots; i++) {
				ItemStack s = p.getItemBySlot(SLOTS[i]);
				if (!s.isEmpty()) {
					this.stacks[this.count++] = s;
					any = true;
				}
			}
		}
		if (!any && preview) {
			ItemStack[] demo = sample();
			for (int i = 0; i < slots; i++) {
				this.stacks[this.count++] = demo[i];
			}
		}
	}

	@Override
	protected void layout(boolean preview) {
		this.collect(preview);
		float size = HudStyle.textSize();
		this.columnW = 0f;
		for (int i = 0; i < this.count; i++) {
			ItemStack s = this.stacks[i];
			StringBuilder t = this.texts[i];
			t.setLength(0);
			float f = HudItems.durability(s);
			this.fractions[i] = f;
			if (f >= 0f) {
				switch (this.readout.get()) {
					case PERCENT -> t.append(Math.round(f * 100f)).append('%');
					case REMAINING -> t.append(s.getMaxDamage() - s.getDamageValue());
					case NONE -> {
					}
				}
			} else if (s.getCount() > 1) {
				t.append(s.getCount());
			}
			this.columnW = Math.max(this.columnW, HudStyle.fontStrong().width(t, size));
		}
		float pad = HudStyle.pad();
		if (this.layout.is(Layout.VERTICAL)) {
			this.columnW = Math.max(this.columnW, this.bars.on() ? 24f : 0f);
			this.width = pad * 2f + ICON + (this.columnW > 0f ? 6f + this.columnW : 0f);
			this.height = pad * 2f + this.count * (ICON + 3f) - 3f;
		} else {
			float cell = Math.max(ICON, this.columnW) + 6f;
			this.width = pad * 2f + this.count * cell - 6f;
			boolean anyText = this.columnW > 0f;
			this.height = pad * 2f + ICON + (this.bars.on() ? 4f : 0f) + (anyText ? size + 3f : 0f);
		}
		if (this.count == 0) {
			this.width = 40f;
			this.height = 20f;
		}
	}

	private int barColor(float f) {
		return this.healthColors.on() ? HudItems.durabilityColor(f) : HudStyle.accent();
	}

	private float warnPulse(float f) {
		if (!this.lowWarning.on() || f < 0f || f >= 0.1f) {
			return 1f;
		}
		return 0.55f + 0.45f * (float) (0.5 + 0.5 * Math.cos(Clock.time() * Math.PI * 2.0 * 1.1));
	}

	@Override
	protected void draw(GuiGraphics g, float partialTick, boolean preview) {
		HudStyle.panel(g, 0f, 0f, this.width, this.height);
		float pad = HudStyle.pad();
		float size = HudStyle.textSize();
		int track = Colors.withAlpha(0xFFFFFF, 0.1f);
		boolean vertical = this.layout.is(Layout.VERTICAL);
		float cell = Math.max(ICON, this.columnW) + 6f;
		for (int i = 0; i < this.count; i++) {
			ItemStack s = this.stacks[i];
			float f = this.fractions[i];
			float pulse = this.warnPulse(f);
			StringBuilder t = this.texts[i];
			int textColor = f >= 0f && f < 0.1f && this.lowWarning.on() ? Colors.mulAlpha(Theme.DANGER, pulse) : HudStyle.text();
			if (vertical) {
				float y = pad + i * (ICON + 3f);
				HudItems.render(g, s, pad, y, ICON);
				float tx = pad + ICON + 6f;
				boolean bar = this.bars.on() && f >= 0f;
				float cy = y + ICON * 0.5f - (bar && !t.isEmpty() ? 2f : 0f);
				if (!t.isEmpty()) {
					HudStyle.label(g, HudStyle.fontStrong(), t, tx, cy, size, textColor, Text.Align.LEFT);
				}
				if (bar) {
					float by = t.isEmpty() ? y + ICON * 0.5f - 1f : y + ICON - 2f;
					Draw.rect(g, tx, by, this.columnW, 2f, 1f, track);
					Draw.rect(g, tx, by, Math.max(2f, this.columnW * f), 2f, 1f, Colors.mulAlpha(this.barColor(f), pulse));
				}
			} else {
				float x = pad + i * cell;
				float cw = cell - 6f;
				HudItems.render(g, s, x + (cw - ICON) * 0.5f, pad, ICON);
				float y = pad + ICON;
				if (this.bars.on()) {
					if (f >= 0f) {
						float bw = Math.min(cw, 18f);
						float bx = x + (cw - bw) * 0.5f;
						Draw.rect(g, bx, y + 1.5f, bw, 2f, 1f, track);
						Draw.rect(g, bx, y + 1.5f, Math.max(2f, bw * f), 2f, 1f, Colors.mulAlpha(this.barColor(f), pulse));
					}
					y += 4f;
				}
				if (!t.isEmpty()) {
					HudStyle.label(g, HudStyle.fontStrong(), t, x + cw * 0.5f, y + 3f + size * 0.5f, size, textColor, Text.Align.CENTER);
				}
			}
		}
	}
}
