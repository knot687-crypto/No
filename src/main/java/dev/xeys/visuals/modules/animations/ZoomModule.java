package dev.xeys.visuals.modules.animations;

import dev.xeys.visuals.anim.Zoom;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;

/** Hold the zoom key (C by default, in Controls) to zoom smoothly; the wheel adjusts how far. */
public final class ZoomModule extends Module {
	private final NumberSetting level = this.add(new NumberSetting("level", "Zoom", 4f, 1.5f, 30f, 0.5f).unit("x").decimals(1)
		.onChange(v -> this.apply()).group("Zoom"));
	private final NumberSetting speed = this.add(new NumberSetting("speed", "Speed", 100f, 25f, 300f, 5f).unit("%").onChange(v -> this.apply())
		.group("Zoom"));
	private final BoolSetting scroll = this.add(new BoolSetting("scroll", "Scroll to Adjust", true)
		.describe("The mouse wheel zooms further in or out while held").onChange(v -> this.apply()).group("Zoom"));
	private final BoolSetting resetOnRelease = this.add(new BoolSetting("reset", "Reset on Release", true)
		.describe("Each zoom starts at the level above").onChange(v -> this.apply()).group("Zoom"));
	private final BoolSetting steady = this.add(new BoolSetting("steady", "Steady Aim", true)
		.describe("Mouse sensitivity drops with the zoom").onChange(v -> this.apply()).group("Feel"));
	private final BoolSetting cinematic = this.add(new BoolSetting("cinematic", "Cinematic", false)
		.describe("Smooth camera motion while zoomed").onChange(v -> this.apply()).group("Feel"));
	private final BoolSetting hideHand = this.add(new BoolSetting("hide_hand", "Hide Hand", false).onChange(v -> this.apply()).group("Feel"));

	public ZoomModule() {
		super("zoom", "Smooth Zoom", Category.ANIMATIONS, Icons.ZOOM_IN, "Hold C to zoom smoothly; scroll to adjust");
	}

	private void apply() {
		Zoom.enabled = this.isEnabled();
		Zoom.configured = this.level.value();
		Zoom.level = this.level.value();
		Zoom.speed = this.speed.value() / 100f;
		Zoom.scrollable = this.scroll.on();
		Zoom.resetOnRelease = this.resetOnRelease.on();
		Zoom.steadyAim = this.steady.on();
		Zoom.cinematic = this.cinematic.on();
		Zoom.hideHand = this.hideHand.on();
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		Zoom.enabled = false;
		Zoom.reset();
	}
}
