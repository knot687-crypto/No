package dev.xeys.visuals.modules.hud;

import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.hud.HudItems;
import dev.xeys.visuals.hud.HudModule;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.HudStyle;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Your inventory on screen in a glass grid, with stack counts and durability. */
public final class InventoryHudModule extends HudModule {
	private static final String[] COUNTS = new String[100];
	private static ItemStack[] sample;

	static {
		for (int i = 0; i < COUNTS.length; i++) {
			COUNTS[i] = Integer.toString(i);
		}
	}

	private final NumberSetting slotSize = this.add(new NumberSetting("slot_size", "Slot Size", 18f, 16f, 26f, 1f).unit("px").group("Display"));
	private final BoolSetting hotbar = this.add(new BoolSetting("hotbar", "Include Hotbar", false).group("Display"));
	private final BoolSetting slots = this.add(new BoolSetting("slots", "Slot Backgrounds", true).group("Display"));
	private final BoolSetting hideEmpty = this.add(new BoolSetting("hide_empty", "Hide When Empty", true).group("Display"));
	private final BoolSetting bars = this.add(new BoolSetting("bars", "Durability Bars", true).group("Display"));

	public InventoryHudModule() {
		super("inventory_hud", "Inventory HUD", Icons.BACKPACK, "Your inventory in a glass grid", HudPosition.Anchor.BOTTOM_RIGHT, -6f, -102f);
	}

	private static ItemStack[] sample() {
		if (sample == null) {
			sample = new ItemStack[36];
			for (int i = 0; i < 36; i++) {
				sample[i] = ItemStack.EMPTY;
			}
			sample[9] = new ItemStack(Items.OAK_LOG, 48);
			sample[10] = new ItemStack(Items.COBBLESTONE, 64);
			sample[11] = new ItemStack(Items.TORCH, 23);
			sample[13] = new ItemStack(Items.ENDER_PEARL, 6);
			sample[15] = new ItemStack(Items.GOLDEN_APPLE, 3);
			sample[18] = new ItemStack(Items.IRON_INGOT, 31);
			sample[19] = new ItemStack(Items.DIAMOND, 9);
			ItemStack bow = new ItemStack(Items.BOW);
			bow.setDamageValue(bow.getMaxDamage() / 3);
			sample[22] = bow;
			sample[23] = new ItemStack(Items.ARROW, 64);
			sample[27] = new ItemStack(Items.BREAD, 17);
			sample[30] = new ItemStack(Items.WATER_BUCKET);
			sample[34] = new ItemStack(Items.REDSTONE, 40);
		}
		return sample;
	}

	private ItemStack slot(Inventory inv, int index, boolean demo) {
		return demo ? sample()[index] : inv.getItem(index);
	}

	private boolean useDemo(boolean preview) {
		Player p = Minecraft.getInstance().player;
		return preview && (p == null || this.isEmpty(p.getInventory()));
	}

	private boolean isEmpty(Inventory inv) {
		int from = this.hotbar.on() ? 0 : 9;
		for (int i = from; i < 36; i++) {
			if (!inv.getItem(i).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean hasContent(boolean preview) {
		if (preview || !this.hideEmpty.on()) {
			return true;
		}
		Player p = Minecraft.getInstance().player;
		return p != null && !this.isEmpty(p.getInventory());
	}

	@Override
	protected void layout(boolean preview) {
		float s = this.slotSize.value();
		int rows = this.hotbar.on() ? 4 : 3;
		float pad = HudStyle.pad();
		this.width = pad * 2f + 9f * s + 8f;
		this.height = pad * 2f + rows * s + (rows - 1) + (this.hotbar.on() ? 3f : 0f);
	}

	@Override
	protected void draw(GuiGraphics g, float partialTick, boolean preview) {
		HudStyle.panel(g, 0f, 0f, this.width, this.height);
		float s = this.slotSize.value();
		float pad = HudStyle.pad();
		boolean demo = this.useDemo(preview);
		Player p = Minecraft.getInstance().player;
		Inventory inv = p != null ? p.getInventory() : null;
		if (inv == null && !demo) {
			return;
		}
		int rows = this.hotbar.on() ? 4 : 3;
		float itemSize = Math.min(16f, s - 2f);
		float cs = Math.max(6.5f, s * 0.36f);
		for (int r = 0; r < rows; r++) {
			// Main inventory rows first (slots 9-35), then the hotbar (0-8) below a small gap.
			int first = r < 3 ? 9 + r * 9 : 0;
			float y = pad + r * (s + 1f) + (r == 3 ? 3f : 0f);
			for (int c = 0; c < 9; c++) {
				float x = pad + c * (s + 1f);
				if (this.slots.on()) {
					Draw.rect(g, x, y, s, s, Math.min(3.5f, s * 0.2f), Colors.withAlpha(0xFFFFFF, r == 3 ? 0.07f : 0.045f));
				}
				ItemStack stack = this.slot(inv, first + c, demo);
				if (stack.isEmpty()) {
					continue;
				}
				float ix = x + (s - itemSize) * 0.5f;
				float iy = y + (s - itemSize) * 0.5f;
				HudItems.render(g, stack, ix, iy, itemSize);
				if (stack.getCount() > 1) {
					String n = stack.getCount() < COUNTS.length ? COUNTS[stack.getCount()] : Integer.toString(stack.getCount());
					float tw = Fonts.BOLD.width(n, cs);
					float bx = x + s - 1.2f - tw;
					float by = y + s - 1.4f;
					Text.shadow(g, Fonts.BOLD, n, bx, by, cs, 0xB0000000, 0f, 0.4f, 1.1f, 0f);
					Text.drawBaseline(g, Fonts.BOLD, n, bx, by, cs, 0xFFFFFFFF, 0f, 0f);
				}
				float f = HudItems.durability(stack);
				if (this.bars.on() && f >= 0f && f < 1f) {
					float bw = s - 6f;
					Draw.rect(g, x + 3f, y + s - 2.6f, bw, 1.4f, 0.7f, 0x99000000);
					Draw.rect(g, x + 3f, y + s - 2.6f, Math.max(1.4f, bw * f), 1.4f, 0.7f, HudItems.durabilityColor(f));
				}
			}
		}
	}
}
