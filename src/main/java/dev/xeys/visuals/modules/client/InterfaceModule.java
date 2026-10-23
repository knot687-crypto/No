package dev.xeys.visuals.modules.client;

import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.ui.core.UiScale;

/** Interface scale, sounds and motion preferences. */
public final class InterfaceModule extends Module {
	public final NumberSetting scale = this.add(new NumberSetting("scale", "Interface Scale", 100f, 80f, 140f, 5f).unit("%")
		.describe("Size of the menu, ClickGUI and editors").group("Layout"));
	public final BoolSetting sounds = this.add(new BoolSetting("sounds", "Interface Sounds", true)
		.describe("Soft clicks on buttons and toggles").group("Feedback"));
	public final BoolSetting reduceMotion = this.add(new BoolSetting("reduce_motion", "Reduce Motion", false)
		.describe("Shorter, calmer transitions").group("Feedback"));

	private static InterfaceModule instance;

	public InterfaceModule() {
		super("interface", "Interface", Category.CLIENT, Icons.LAYOUT_PANEL_LEFT, "Interface scale, sounds and motion");
		this.alwaysOn();
		instance = this;
		this.scale.onChange(v -> UiScale.setUserScale(v / 100f));
	}

	@Override
	protected void onEnable() {
		UiScale.setUserScale(this.scale.value() / 100f);
	}

	public static boolean soundsEnabled() {
		return instance == null || instance.sounds.on();
	}

	public static boolean reducedMotion() {
		return instance != null && instance.reduceMotion.on();
	}
}
