package dev.xeys.visuals.modules.cosmetics;

import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.cosmetic.CosmeticHooks;
import dev.xeys.visuals.cosmetic.Cosmetics;
import dev.xeys.visuals.ui.Icons;

/** Settings shared by every cosmetic. Cosmetics are only ever drawn on your own screen. */
public final class CosmeticsModule extends Module {
	private final BoolSetting others = this.add(new BoolSetting("on_others", "Show on Other Players", false)
		.describe("Draw your cosmetics on everyone you see, just for fun").onChange(v -> Cosmetics.onOthers = v).group("Visibility"));
	private final BoolSetting ownName = this.add(new BoolSetting("own_name", "Show Own Name Tag", false)
		.describe("See your own name tag in third person").onChange(v -> CosmeticHooks.ownNameTag = v).group("Visibility"));

	public CosmeticsModule() {
		super("cosmetics", "Cosmetics", Category.COSMETICS, Icons.SPARKLES, "Who wears your cosmetics, and your own name tag");
		this.alwaysOn();
	}

	@Override
	protected void onEnable() {
		Cosmetics.onOthers = this.others.on();
		CosmeticHooks.ownNameTag = this.ownName.on();
	}
}
