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
import dev.xeys.visuals.modules.screen.AmbientGlowModule;
import dev.xeys.visuals.modules.screen.ChromaticAberrationModule;
import dev.xeys.visuals.modules.screen.ColorFilterModule;
import dev.xeys.visuals.modules.screen.DamageDirectionModule;
import dev.xeys.visuals.modules.screen.DamageFlashModule;
import dev.xeys.visuals.modules.screen.DurabilityAlertModule;
import dev.xeys.visuals.modules.screen.HitMarkerModule;
import dev.xeys.visuals.modules.screen.InventoryBackgroundModule;
import dev.xeys.visuals.modules.screen.FilmGrainModule;
import dev.xeys.visuals.modules.screen.LetterboxModule;
import dev.xeys.visuals.modules.screen.LowHealthModule;
import dev.xeys.visuals.modules.screen.MonochromeModule;
import dev.xeys.visuals.modules.screen.PixelateModule;
import dev.xeys.visuals.modules.screen.ScanlinesModule;
import dev.xeys.visuals.modules.screen.ScreenshotFlashModule;
import dev.xeys.visuals.modules.screen.SharpenModule;
import dev.xeys.visuals.modules.screen.SpeedLinesModule;
import dev.xeys.visuals.modules.screen.TooltipStyleModule;
import dev.xeys.visuals.modules.screen.UnderwaterRippleModule;
import dev.xeys.visuals.modules.screen.VignetteModule;

/** Registers the feature modules, batch by batch. Registration order is the order the ClickGUI lists them. */
final class ModuleCatalog {
	private ModuleCatalog() {
	}

	static void registerAll() {
		hudBatch1();
		screenBatch1();
	}

	/** Screen batch 1: post effects in the combined screen pass, then 2D overlays and screen restyles. */
	private static void screenBatch1() {
		ModuleManager.register(new VignetteModule());
		ModuleManager.register(new FilmGrainModule());
		ModuleManager.register(new ChromaticAberrationModule());
		ModuleManager.register(new SharpenModule());
		ModuleManager.register(new ColorFilterModule());
		ModuleManager.register(new MonochromeModule());
		ModuleManager.register(new AmbientGlowModule());
		ModuleManager.register(new LowHealthModule());
		ModuleManager.register(new DamageFlashModule());
		ModuleManager.register(new UnderwaterRippleModule());
		ModuleManager.register(new LetterboxModule());
		ModuleManager.register(new ScanlinesModule());
		ModuleManager.register(new PixelateModule());
		ModuleManager.register(new DamageDirectionModule());
		ModuleManager.register(new HitMarkerModule());
		ModuleManager.register(new SpeedLinesModule());
		ModuleManager.register(new DurabilityAlertModule());
		ModuleManager.register(new ScreenshotFlashModule());
		ModuleManager.register(new InventoryBackgroundModule());
		ModuleManager.register(new TooltipStyleModule());
	}

	/**
	 * HUD batch 1. Within each screen corner, untouched elements stack in this order (see HudLayout), so
	 * the order here is also the default layout: left edge, right edge, bottom right, then the center.
	 */
	private static void hudBatch1() {
		// Left edge, top down.
		ModuleManager.register(new WatermarkModule());
		ModuleManager.register(new FpsModule());
		ModuleManager.register(new CpsModule());
		ModuleManager.register(new PingModule());
		ModuleManager.register(new CoordinatesModule());
		ModuleManager.register(new SpeedModule());
		ModuleManager.register(new DirectionModule());
		ModuleManager.register(new BiomeModule());
		ModuleManager.register(new LightLevelModule());
		ModuleManager.register(new KeystrokesModule());
		// Right edge, top down.
		ModuleManager.register(new PotionHudModule());
		ModuleManager.register(new ClockModule());
		ModuleManager.register(new MemoryModule());
		ModuleManager.register(new SessionTimeModule());
		ModuleManager.register(new ServerInfoModule());
		ModuleManager.register(new DayCounterModule());
		ModuleManager.register(new WeatherModule());
		ModuleManager.register(new MoonPhaseModule());
		ModuleManager.register(new CompassModule());
		ModuleManager.register(new ModuleListModule());
		// Bottom right, bottom up.
		ModuleManager.register(new ArmorHudModule());
		ModuleManager.register(new InventoryHudModule());
		ModuleManager.register(new PackDisplayModule());
		// Center column.
		ModuleManager.register(new HeadingBarModule());
		ModuleManager.register(new TargetHudModule());
		ModuleManager.register(new HeldItemModule());
	}
}
