package dev.xeys.visuals.modules.hud;

import dev.xeys.visuals.anim.Anim;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.anim.Easing;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.hud.HudItems;
import dev.xeys.visuals.hud.HudModule;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.HudStyle;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The item in your hand: name in its rarity color, durability, and how many you carry in total. */
public final class HeldItemModule extends HudModule {
	public enum Mode {
		ALWAYS,
		ON_SWITCH
	}

	private final EnumSetting<Mode> mode = this.add(new EnumSetting<>("mode", "Show", Mode.ALWAYS)
		.describe("Always, or briefly after switching items").group("Display"));
	private final NumberSetting linger = this.add(new NumberSetting("linger", "Linger", 2.5f, 0.5f, 8f, 0.5f).unit("s").decimals(1)
		.visibleWhen(() -> this.mode.is(Mode.ON_SWITCH)).group("Display"));
	private final BoolSetting rarity = this.add(new BoolSetting("rarity_color", "Rarity Color", true).describe("Tint the name by item rarity")
		.group("Display"));
	private final BoolSetting total = this.add(new BoolSetting("total", "Total Count", true).describe("How many of this item you carry")
		.group("Display"));
	private final BoolSetting durability = this.add(new BoolSetting("durability", "Durability", true).group("Display"));

	private ItemStack stack = ItemStack.EMPTY;
	private ItemStack lastSeen = ItemStack.EMPTY;
	private String name = "";
	private ItemStack named = ItemStack.EMPTY;
	private int nameColor = 0xFFFFFFFF;
	private final StringBuilder detail = new StringBuilder(24);
	private float fraction = -1f;
	private double switchedAt = -100.0;
	private final Anim appear = new Anim(0f);
	private ItemStack sample;
	private long countedAt;
	private int totalCount;

	public HeldItemModule() {
		super("held_item", "Held Item", Icons.PACKAGE, "The item in your hand with durability and total count", HudPosition.Anchor.BOTTOM_CENTER, 0f,
			-62f);
	}

	private ItemStack current(boolean preview) {
		Player p = Minecraft.getInstance().player;
		ItemStack s = p != null ? p.getMainHandItem() : ItemStack.EMPTY;
		if (s.isEmpty() && preview) {
			if (this.sample == null) {
				this.sample = new ItemStack(Items.DIAMOND_PICKAXE);
				this.sample.setDamageValue(this.sample.getMaxDamage() / 4);
			}
			return this.sample;
		}
		return s;
	}

	/** Tracks item switches (for the On Switch mode); cheap enough to run every frame. */
	private boolean wanted(boolean preview) {
		ItemStack s = this.current(preview);
		if (s != this.lastSeen && !ItemStack.isSameItemSameComponents(s, this.lastSeen)) {
			this.switchedAt = Clock.time();
		}
		this.lastSeen = s;
		if (!s.isEmpty()) {
			this.stack = s;
		}
		return !s.isEmpty() && (preview || this.mode.is(Mode.ALWAYS) || Clock.time() - this.switchedAt < this.linger.value());
	}

	@Override
	public boolean hasContent(boolean preview) {
		return this.wanted(preview) || this.appear.get() > 0f;
	}

	@Override
	protected void layout(boolean preview) {
		boolean visible = this.wanted(preview);
		if (visible && this.appear.target() < 1f) {
			this.appear.to(1f, 0.25f, Easing.OUT_CUBIC);
		} else if (!visible && this.appear.target() > 0f) {
			this.appear.to(0f, 0.35f, Easing.IN_OUT_CUBIC);
		}
		ItemStack s = this.stack;
		if (s != this.named) {
			this.named = s;
			this.name = s.getHoverName().getString();
		}
		ChatFormatting rarityFormat = s.getRarity().color();
		Integer rarityColor = rarityFormat.getColor();
		this.nameColor = this.rarity.on() && rarityColor != null && rarityFormat != ChatFormatting.WHITE ? 0xFF000000 | rarityColor : HudStyle.text();
		this.detail.setLength(0);
		this.fraction = this.durability.on() ? HudItems.durability(s) : -1f;
		if (this.fraction >= 0f) {
			this.detail.append(s.getMaxDamage() - s.getDamageValue()).append(" / ").append(s.getMaxDamage());
		} else if (this.total.on() && s.isStackable()) {
			long now = System.nanoTime();
			if (now - this.countedAt > 250_000_000L) {
				this.countedAt = now;
				this.totalCount = this.count(s, preview);
			}
			this.detail.append(this.totalCount).append(" total");
		}
		float size = HudStyle.textSize();
		float textW = Math.max(HudStyle.fontStrong().width(this.name, size), HudStyle.font().width(this.detail, size * 0.86f));
		this.width = HudStyle.pad() * 2f + 18f + 7f + Math.min(textW, 220f);
		this.height = Math.max(26f, this.detail.isEmpty() ? size + HudStyle.pad() * 2f + 1f : size * 2f + HudStyle.pad() * 2f + 3f);
	}

	private int count(ItemStack s, boolean preview) {
		Player p = Minecraft.getInstance().player;
		if (p == null) {
			return preview ? 64 : s.getCount();
		}
		Inventory inv = p.getInventory();
		int n = 0;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack o = inv.getItem(i);
			if (!o.isEmpty() && ItemStack.isSameItemSameComponents(o, s)) {
				n += o.getCount();
			}
		}
		return n;
	}

	@Override
	protected void draw(GuiGraphics g, float partialTick, boolean preview) {
		float a = Easing.OUT_CUBIC.apply(this.appear.get());
		float rise = (1f - a) * 6f;
		g.pose().pushMatrix();
		g.pose().translate(0f, rise);
		HudStyle.panel(g, 0f, 0f, this.width, this.height);
		float pad = HudStyle.pad();
		float size = HudStyle.textSize();
		HudItems.render(g, this.stack, pad + 1f, (this.height - 16f) * 0.5f, 16f);
		float tx = pad + 18f + 7f;
		float maxW = this.width - tx - pad;
		if (this.detail.isEmpty()) {
			Text.drawTruncated(g, HudStyle.fontStrong(), this.name, tx, this.height * 0.5f, size, Colors.mulAlpha(this.nameColor, a), maxW);
		} else {
			float ny = pad + size * 0.5f + 0.5f;
			Text.drawTruncated(g, HudStyle.fontStrong(), this.name, tx, ny, size, Colors.mulAlpha(this.nameColor, a), maxW);
			float dy = ny + size + 2f;
			int muted = Colors.mulAlpha(HudStyle.textSecondary(), a);
			float dw = HudStyle.label(g, HudStyle.font(), this.detail, tx, dy, size * 0.86f, muted, Text.Align.LEFT);
			if (this.fraction >= 0f) {
				float bx = tx + dw + 6f;
				float bw = this.width - pad - bx;
				if (bw > 8f) {
					Draw.rect(g, bx, dy - 1f, bw, 2f, 1f, Colors.withAlpha(0xFFFFFF, 0.09f * a));
					Draw.rect(g, bx, dy - 1f, Math.max(2f, bw * this.fraction), 2f, 1f, Colors.mulAlpha(HudItems.durabilityColor(this.fraction), a));
				}
			}
		}
		g.pose().popMatrix();
	}
}
