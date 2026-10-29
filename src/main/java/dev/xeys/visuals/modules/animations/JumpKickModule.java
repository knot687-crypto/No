package dev.xeys.visuals.modules.animations;

import dev.xeys.visuals.anim.CameraMotion;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;

/** A small nod of the camera as you push off for a jump. */
public final class JumpKickModule extends Module {
	private final NumberSetting kick = this.add(new NumberSetting("kick", "Kick", 1.5f, 0.5f, 5f, 0.25f).unit("°").decimals(2)
		.onChange(v -> this.apply()).group("Jump"));

	public JumpKickModule() {
		super("jump_kick", "Jump Kick", Category.ANIMATIONS, Icons.CHEVRON_UP, "The camera nods as you jump");
	}

	private void apply() {
		CameraMotion.jumpKick = this.isEnabled() ? this.kick.value() : 0f;
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		CameraMotion.jumpKick = 0f;
	}
}
