package dev.xeys.visuals.modules.screen;

import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.post.ScreenFx;
import dev.xeys.visuals.ui.Icons;

/** Grayscale, sepia or high-contrast noir. */
public final class MonochromeModule extends ScreenEffectModule {
	public enum Mode {
		GRAYSCALE,
		SEPIA,
		NOIR
	}

	private final EnumSetting<Mode> mode = this.add(new EnumSetting<>("mode", "Style", Mode.GRAYSCALE).group("Monochrome"));
	private final NumberSetting amount = this.add(new NumberSetting("amount", "Amount", 100f, 0f, 100f, 1f).unit("%").group("Monochrome"));

	public MonochromeModule() {
		super("monochrome", "Monochrome", Icons.CONTRAST, "Grayscale, sepia or film-noir look");
	}

	@Override
	public void contribute(ScreenFx fx, float partialTick) {
		fx.mono = this.amount.value() / 100f;
		fx.monoMode = this.mode.get().ordinal();
	}
}
