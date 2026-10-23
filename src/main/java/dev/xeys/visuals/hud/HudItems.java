package dev.xeys.visuals.hud;

import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/** Item helpers shared by item-showing HUD elements. */
public final class HudItems {
	private HudItems() {
	}

	/** Renders an item at a fractional position and any size (vanilla items are 16x16). */
	public static void render(GuiGraphics g, ItemStack stack, float x, float y, float size) {
		if (stack.isEmpty()) {
			return;
		}
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		if (size != 16f) {
			g.pose().scale(size / 16f, size / 16f);
		}
		g.renderItem(stack, 0, 0);
		g.pose().popMatrix();
	}

	/** Remaining durability in 0..1, or -1 for items without durability. */
	public static float durability(ItemStack stack) {
		if (stack.isEmpty() || !stack.isDamageableItem() || stack.getMaxDamage() <= 0) {
			return -1f;
		}
		return 1f - (float) stack.getDamageValue() / stack.getMaxDamage();
	}

	/** Green when healthy, through amber, to red when nearly broken (interpolated in OKLab). */
	public static int durabilityColor(float fraction) {
		float f = Math.max(0f, Math.min(fraction, 1f));
		return f > 0.5f ? Colors.lerpOklab(Theme.WARNING, Theme.SUCCESS, (f - 0.5f) * 2f) : Colors.lerpOklab(Theme.DANGER, Theme.WARNING, f * 2f);
	}
}
