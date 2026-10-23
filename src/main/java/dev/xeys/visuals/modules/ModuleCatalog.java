package dev.xeys.visuals.modules;

import dev.xeys.visuals.core.module.ModuleManager;
import dev.xeys.visuals.modules.hud.ArmorHudModule;
import dev.xeys.visuals.modules.hud.BiomeModule;
import dev.xeys.visuals.modules.hud.ClockModule;
import dev.xeys.visuals.modules.hud.CompassModule;
import dev.xeys.visuals.modules.hud.CoordinatesModule;
import dev.xeys.visuals.modules.hud.CpsModule;
import dev.xeys.visuals.modules.hud.DayCounterModule;
import dev.xeys.visuals.modules.hud.DirectionModule;
import dev.xeys.visuals.modules.hud.FpsModule;
import dev.xeys.visuals.modules.hud.HeadingBarModule;
import dev.xeys.visuals.modules.hud.HeldItemModule;
import dev.xeys.visuals.modules.hud.InventoryHudModule;
import dev.xeys.visuals.modules.hud.KeystrokesModule;
import dev.xeys.visuals.modules.hud.LightLevelModule;
import dev.xeys.visuals.modules.hud.MemoryModule;
import dev.xeys.visuals.modules.hud.ModuleListModule;
import dev.xeys.visuals.modules.hud.MoonPhaseModule;
import dev.xeys.visuals.modules.hud.PackDisplayModule;
import dev.xeys.visuals.modules.hud.PingModule;
import dev.xeys.visuals.modules.hud.PotionHudModule;
import dev.xeys.visuals.modules.hud.ServerInfoModule;
import dev.xeys.visuals.modules.hud.SessionTimeModule;
import dev.xeys.visuals.modules.hud.SpeedModule;
import dev.xeys.visuals.modules.hud.TargetHudModule;
import dev.xeys.visuals.modules.hud.WatermarkModule;
import dev.xeys.visuals.modules.hud.WeatherModule;

/** Registers the feature modules, batch by batch. Registration order is the order the ClickGUI lists them. */
final class ModuleCatalog {
	private ModuleCatalog() {
	}

	static void registerAll() {
		hudBatch1();
	}

	/** HUD batch 1: readouts, compass, armor, effects, keystrokes, target and info elements. */
	private static void hudBatch1() {
		ModuleManager.register(new WatermarkModule());
		ModuleManager.register(new ModuleListModule());
		ModuleManager.register(new FpsModule());
		ModuleManager.register(new CpsModule());
		ModuleManager.register(new PingModule());
		ModuleManager.register(new CoordinatesModule());
		ModuleManager.register(new DirectionModule());
		ModuleManager.register(new CompassModule());
		ModuleManager.register(new HeadingBarModule());
		ModuleManager.register(new SpeedModule());
		ModuleManager.register(new ClockModule());
		ModuleManager.register(new SessionTimeModule());
		ModuleManager.register(new MemoryModule());
		ModuleManager.register(new ServerInfoModule());
		ModuleManager.register(new BiomeModule());
		ModuleManager.register(new DayCounterModule());
		ModuleManager.register(new MoonPhaseModule());
		ModuleManager.register(new WeatherModule());
		ModuleManager.register(new LightLevelModule());
		ModuleManager.register(new ArmorHudModule());
		ModuleManager.register(new PotionHudModule());
		ModuleManager.register(new HeldItemModule());
		ModuleManager.register(new InventoryHudModule());
		ModuleManager.register(new KeystrokesModule());
		ModuleManager.register(new TargetHudModule());
		ModuleManager.register(new PackDisplayModule());
	}
}
