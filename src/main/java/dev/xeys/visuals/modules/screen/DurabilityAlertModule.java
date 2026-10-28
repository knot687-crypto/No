package dev.xeys.visuals.modules.screen;

import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.anim.Easing;
import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.hud.HudItems;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** A card above the hotbar the moment a worn or held item drops below a durability threshold. */
public final class DurabilityAlertModule extends Module implements Events.Overlay, Events.ClientTick {
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
		EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
	private static final float HOLD = 3.2f;

	private final NumberSetting threshold = this.add(new NumberSetting("threshold", "Warn Below", 10f, 2f, 40f, 1f).unit("%").group("Alert"));
	private final float[] last = new float[SLOTS.length];
	private final ItemStack[] lastStack = new ItemStack[SLOTS.length];
	private ItemStack alertStack = ItemStack.EMPTY;
	private String alertName = "";
	private final StringBuilder alertText = new StringBuilder(24);
	private float alertFraction;
	private double alertAt = -100.0;

	public DurabilityAlertModule() {
		super("durability_alert", "Durability Alert", Category.SCREEN, Icons.CIRCLE_ALERT, "A warning when gear is about to break");
		for (int i = 0; i < SLOTS.length; i++) {
			this.last[i] = 1f;
			this.lastStack[i] = ItemStack.EMPTY;
		}
	}

	@Override
	public void onClientTick() {
		Player p = Minecraft.getInstance().player;
		if (p == null) {
			return;
		}
		float limit = this.threshold.value() / 100f;
		for (int i = 0; i < SLOTS.length; i++) {
			ItemStack s = p.getItemBySlot(SLOTS[i]);
			float f = HudItems.durability(s);
			boolean same = ItemStack.isSameItem(s, this.lastStack[i]);
			// Warn when this item crosses the threshold, and again at half of it.
			if (same && f >= 0f && ((this.last[i] >= limit && f < limit) || (this.last[i] >= limit * 0.5f && f < limit * 0.5f))) {
				this.alert(s, f);
			}
			this.last[i] = f < 0f ? 1f : f;
			this.lastStack[i] = s;
		}
	}

	private void alert(ItemStack s, float f) {
		this.alertStack = s.copy();
		this.alertName = s.getHoverName().getString();
		this.alertFraction = f;
		this.alertText.setLength(0);
		this.alertText.append(Math.max(1, Math.round(f * 100f))).append("% durability left");
		this.alertAt = Clock.time();
	}

	@Override
	public boolean aboveHud() {
		return true;
	}

	@Override
	public void renderOverlay(GuiGraphics g, float partialTick) {
		float age = (float) (Clock.time() - this.alertAt);
		if (age < 0f || age > HOLD + 0.4f) {
			return;
		}
		float in = Easing.OUT_BACK_SOFT.apply(Math.min(age / 0.35f, 1f));
		float out = age > HOLD ? 1f - Easing.IN_OUT_CUBIC.apply((age - HOLD) / 0.4f) : 1f;
		float a = Math.min(Math.min(age / 0.2f, 1f), out);
		float size = 9f;
		float nameW = Fonts.SEMIBOLD.width(this.alertName, size);
		float textW = Fonts.REGULAR.width(this.alertText, size * 0.86f);
		float w = 16f + 26f + Math.max(nameW, textW) + 12f;
		float h = 30f;
		float x = (g.guiWidth() - w) * 0.5f;
		float y = g.guiHeight() - 92f + (1f - in) * 10f;
		int warn = this.alertFraction < this.threshold.value() / 200f ? Theme.DANGER : Theme.WARNING;
		float pulse = 0.55f + 0.45f * (float) (0.5 + 0.5 * Math.cos(age * Math.PI * 2.4));
		Draw.shadow(g, x, y, w, h, 9f, 0f, 3f, 12f, 0f, Colors.withAlpha(0x000000, 0.45f * a));
		Draw.glass(g, x, y, w, h, Colors.withAlpha(0x0A0A0C, 0.82f)).radius(9f).border(1f, Colors.mulAlpha(warn, (0.35f + 0.5f * pulse) * a))
			.alpha(a).submit();
		Draw.circle(g, x + 20f, y + h * 0.5f, 11f, Colors.mulAlpha(Colors.withAlpha(warn, 0.18f), a));
		HudItems.render(g, this.alertStack, x + 12f, y + 7f, 16f);
		Text.drawMiddle(g, Fonts.SEMIBOLD, this.alertName, x + 38f, y + 10.5f, size, Colors.mulAlpha(Theme.TEXT, a), Text.Align.LEFT);
		Text.drawMiddle(g, Fonts.REGULAR, this.alertText, x + 38f, y + 20.5f, size * 0.86f, Colors.mulAlpha(warn, a), Text.Align.LEFT);
	}
}
