package dev.xeys.visuals.modules.animations;

import dev.xeys.visuals.anim.AnimHooks;
import dev.xeys.visuals.anim.HandFx;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.ui.Icons;

/** The 1.7 look: block-hitting, armor that flashes red when hurt, and no hand dip after attacks. */
public final class OldAnimationsModule extends Module {
	private final BoolSetting blockHit = this.add(new BoolSetting("block_hit", "Block-Hit", true)
		.describe("Hold use with a sword for the old blocking pose; swings show while blocking").onChange(v -> this.apply()).group("Animations"));
	private final BoolSetting redArmor = this.add(new BoolSetting("red_armor", "Red Armor", true)
		.describe("Armor flashes red with its wearer when hurt").onChange(v -> this.apply()).group("Animations"));
	private final BoolSetting noDip = this.add(new BoolSetting("no_cooldown_dip", "No Cooldown Dip", true)
		.describe("The hand stays up after attacks, as before attack cooldowns").onChange(v -> this.apply()).group("Animations"));

	public OldAnimationsModule() {
		super("old_animations", "1.7 Animations", Category.ANIMATIONS, Icons.SWORDS, "Old block-hitting, red armor flashes and no cooldown dip");
	}

	private void apply() {
		HandFx.blockHit = this.isEnabled() && this.blockHit.on();
		AnimHooks.redArmor = this.isEnabled() && this.redArmor.on();
		HandFx.oldNoCooldownDip = this.isEnabled() && this.noDip.on();
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		HandFx.blockHit = false;
		AnimHooks.redArmor = false;
		HandFx.oldNoCooldownDip = false;
	}
}
