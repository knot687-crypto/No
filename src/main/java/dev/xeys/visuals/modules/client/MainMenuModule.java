package dev.xeys.visuals.modules.client;

import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.ui.menu.MenuHooks;

/** The moon main menu and the moon backdrop behind other menus. */
public final class MainMenuModule extends Module {
	public final BoolSetting shootingStars = this.add(new BoolSetting("shooting_stars", "Shooting Stars", true).group("Sky"));
	public final NumberSetting starBrightness = this.add(new NumberSetting("star_brightness", "Star Brightness", 100f, 0f, 200f, 5f).unit("%").group("Sky"));
	public final NumberSetting parallax = this.add(new NumberSetting("parallax", "Parallax", 100f, 0f, 200f, 5f).unit("%")
		.describe("How far the stars and moon drift with the cursor").group("Sky"));
	public final NumberSetting relief = this.add(new NumberSetting("relief", "Crater Relief", 1.7f, 0.5f, 3f, 0.1f).unit("x").group("Moon"));
	public final NumberSetting rotation = this.add(new NumberSetting("rotation", "Rotation Speed", 100f, 0f, 400f, 10f).unit("%").group("Moon"));
	public final BoolSetting backdrop = this.add(new BoolSetting("backdrop", "Moon Behind Menus", true)
		.describe("Use the moon sky behind world selection, options and other menus").group("Menus"));
	public final BoolSetting arrival = this.add(new BoolSetting("arrival", "World Arrival Transition", true)
		.describe("Fade and ease the view in when a world finishes loading").group("Menus"));

	private static MainMenuModule instance;

	public MainMenuModule() {
		super("main_menu", "Moon Menu", Category.CLIENT, Icons.MOON, "The XEYS main menu with the ray-traced moon");
		this.enabledByDefault();
		instance = this;
	}

	public static MainMenuModule get() {
		return instance;
	}

	@Override
	protected void onEnable() {
		MenuHooks.setEnabled(true);
	}

	@Override
	protected void onDisable() {
		MenuHooks.setEnabled(false);
	}
}
