package dev.xeys.visuals.modules.animations;

import dev.xeys.visuals.anim.Emotes;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.ui.Icons;

/** Hold the emote key (B by default, in Controls) to play an emote on your player; only you see it. */
public final class EmotesModule extends Module {
	private final EnumSetting<Emotes.Emote> emote = this.add(new EnumSetting<>("emote", "Emote", Emotes.Emote.WAVE).onChange(v -> this.apply())
		.group("Emote"));
	private final BoolSetting camera = this.add(new BoolSetting("camera", "Front Camera", true)
		.describe("Switch to the front view while emoting, then back").onChange(v -> this.apply()).group("Emote"));

	public EmotesModule() {
		super("emotes", "Emotes", Category.ANIMATIONS, Icons.SMILE, "Hold B to wave, cheer, clap, dab, bow and more");
	}

	private void apply() {
		Emotes.enabled = this.isEnabled();
		Emotes.emote = this.emote.get();
		Emotes.showCamera = this.camera.on();
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		Emotes.enabled = false;
		Emotes.reset();
	}
}
