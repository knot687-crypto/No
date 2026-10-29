package dev.xeys.visuals.modules.cosmetics;

import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.cosmetic.CosmeticHooks;
import dev.xeys.visuals.ui.Icons;

/** Hide pieces of your own armor so your skin and cosmetics show (only on your screen). */
public final class HideArmorModule extends Module {
	private final BoolSetting helmet = this.add(new BoolSetting("helmet", "Helmet", true).onChange(v -> this.apply()).group("Hide"));
	private final BoolSetting chestplate = this.add(new BoolSetting("chestplate", "Chestplate", false).onChange(v -> this.apply()).group("Hide"));
	private final BoolSetting leggings = this.add(new BoolSetting("leggings", "Leggings", false).onChange(v -> this.apply()).group("Hide"));
	private final BoolSetting boots = this.add(new BoolSetting("boots", "Boots", false).onChange(v -> this.apply()).group("Hide"));

	public HideArmorModule() {
		super("hide_armor", "Hide Armor", Category.COSMETICS, Icons.SHIELD, "Hide your own armor pieces so your look shows");
	}

	private void apply() {
		boolean on = this.isEnabled();
		CosmeticHooks.HIDE_ARMOR[0] = on && this.helmet.on();
		CosmeticHooks.HIDE_ARMOR[1] = on && this.chestplate.on();
		CosmeticHooks.HIDE_ARMOR[2] = on && this.leggings.on();
		CosmeticHooks.HIDE_ARMOR[3] = on && this.boots.on();
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		this.apply();
	}
}
