package dev.xeys.visuals.modules.animations;

import dev.xeys.visuals.anim.HandFx;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.ui.Icons;

/** How a newly selected item comes into your hand, and whether the hand dips while attacks recharge. */
public final class ItemSwitchModule extends Module {
	public enum Style {
		INSTANT(HandFx.EquipStyle.INSTANT),
		SLIDE(HandFx.EquipStyle.SLIDE),
		SPIN(HandFx.EquipStyle.SPIN),
		POP(HandFx.EquipStyle.POP);

		final HandFx.EquipStyle style;

		Style(HandFx.EquipStyle style) {
			this.style = style;
		}
	}

	private final EnumSetting<Style> style = this.add(new EnumSetting<>("style", "Style", Style.SLIDE)
		.describe("Instant shows the new item at once").onChange(v -> this.apply()).group("Switch"));
	private final BoolSetting cooldownDip = this.add(new BoolSetting("cooldown_dip", "Cooldown Dip", false)
		.describe("The hand lowers while your attack recharges, as in vanilla").onChange(v -> this.apply()).group("Switch"));

	public ItemSwitchModule() {
		super("item_switch", "Item Switch", Category.ANIMATIONS, Icons.SHUFFLE, "Instant, sliding, spinning or popping item switches");
	}

	private void apply() {
		HandFx.equipStyle = this.isEnabled() ? this.style.get().style : HandFx.EquipStyle.VANILLA;
		HandFx.cooldownDip = !this.isEnabled() || this.cooldownDip.on();
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		HandFx.equipStyle = HandFx.EquipStyle.VANILLA;
		HandFx.cooldownDip = true;
	}
}
