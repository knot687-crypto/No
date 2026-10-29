package dev.xeys.visuals.modules.animations;

import dev.xeys.visuals.anim.HandFx;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.ui.Icons;

/** A different first-person swing: eased, a straight punch, a sideways slash, an overhead chop or a spin. */
public final class SwingStyleModule extends Module {
	public enum Style {
		SMOOTH(HandFx.SwingStyle.SMOOTH),
		PUNCH(HandFx.SwingStyle.PUNCH),
		SLASH(HandFx.SwingStyle.SLASH),
		CHOP(HandFx.SwingStyle.CHOP),
		SPIN(HandFx.SwingStyle.SPIN);

		final HandFx.SwingStyle style;

		Style(HandFx.SwingStyle style) {
			this.style = style;
		}
	}

	private final EnumSetting<Style> style = this.add(new EnumSetting<>("style", "Style", Style.SMOOTH).onChange(v -> this.apply()).group("Swing"));

	public SwingStyleModule() {
		super("swing_style", "Swing Style", Category.ANIMATIONS, Icons.SWORD, "Smooth, punch, slash, chop or spin swings");
	}

	private void apply() {
		HandFx.swingStyle = this.isEnabled() ? this.style.get().style : HandFx.SwingStyle.VANILLA;
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		HandFx.swingStyle = HandFx.SwingStyle.VANILLA;
	}
}
