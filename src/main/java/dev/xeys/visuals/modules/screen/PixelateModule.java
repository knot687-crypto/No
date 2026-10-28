package dev.xeys.visuals.modules.screen;

import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.post.ScreenFx;
import dev.xeys.visuals.ui.Icons;
import net.minecraft.client.Minecraft;

/** Chunky retro pixels over the world (the HUD stays sharp). */
public final class PixelateModule extends ScreenEffectModule {
	private final NumberSetting size = this.add(new NumberSetting("size", "Pixel Size", 4f, 2f, 16f, 1f).unit("px")
		.describe("At 1080p; scales with resolution").group("Pixelate"));

	public PixelateModule() {
		super("pixelate", "Pixelate", Icons.GRID_3X3, "Retro chunky pixels");
	}

	@Override
	public void contribute(ScreenFx fx, float partialTick) {
		float k = Minecraft.getInstance().getWindow().getHeight() / 1080f;
		fx.pixelSize = Math.max(2f, Math.round(this.size.value() * k));
	}
}
