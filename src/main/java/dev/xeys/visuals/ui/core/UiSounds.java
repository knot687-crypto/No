package dev.xeys.visuals.ui.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/** Quiet, slightly varied interface sounds (loud vanilla clicks feel cheap in a premium UI). */
public final class UiSounds {
	private UiSounds() {
	}

	public static void click() {
		if (!dev.xeys.visuals.modules.client.InterfaceModule.soundsEnabled()) {
			return;
		}
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.12f, 0.32f));
	}

	public static void toggle(boolean on) {
		if (!dev.xeys.visuals.modules.client.InterfaceModule.soundsEnabled()) {
			return;
		}
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), on ? 1.35f : 1.0f, 0.22f));
	}

	public static void soft() {
		if (!dev.xeys.visuals.modules.client.InterfaceModule.soundsEnabled()) {
			return;
		}
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.6f, 0.12f));
	}
}
