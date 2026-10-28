package dev.xeys.visuals.modules.screen;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.anim.Easing;
import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.render.texture.CpuTexture;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;

/** A camera-style flash when you take a screenshot, and a thumbnail card that slides in to confirm it. */
public final class ScreenshotFlashModule extends Module implements Events.Overlay {
	private static final float FLASH = 0.6f;
	private static ScreenshotFlashModule instance;

	private final BoolSetting flash = this.add(new BoolSetting("flash", "Flash", true).group("Screenshot"));
	private final NumberSetting strength = this.add(new NumberSetting("strength", "Flash Strength", 55f, 10f, 100f, 1f).unit("%")
		.visibleWhen(this.flash::on).group("Screenshot"));
	private final BoolSetting preview = this.add(new BoolSetting("preview", "Preview Card", true)
		.describe("A thumbnail of the shot slides in at the corner").group("Screenshot"));
	private final NumberSetting hold = this.add(new NumberSetting("hold", "Preview Time", 3f, 1f, 8f, 0.5f).unit("s").decimals(1)
		.visibleWhen(this.preview::on).group("Screenshot"));

	private final CpuTexture thumbnail = new CpuTexture("XEYS screenshot preview");
	private double flashAt = -100.0;
	private double previewAt = -100.0;
	private float aspect = 16f / 9f;

	public ScreenshotFlashModule() {
		super("screenshot_flash", "Screenshot Flash", Category.SCREEN, Icons.CAMERA, "A flash and a preview card for screenshots");
		instance = this;
	}

	public static void onScreenshot(RenderTarget target) {
		ScreenshotFlashModule m = instance;
		if (m == null || !m.isEnabled()) {
			return;
		}
		m.flashAt = Clock.time();
		if (m.preview.on()) {
			// Vanilla requires a downscale factor that divides both dimensions exactly.
			int downscale = 1;
			for (int d = Math.max(1, target.width / 360); d > 1; d--) {
				if (target.width % d == 0 && target.height % d == 0) {
					downscale = d;
					break;
				}
			}
			try {
				Screenshot.takeScreenshot(target, downscale, image -> m.acceptThumbnail(image));
			} catch (RuntimeException e) {
				// The preview is a nicety; never let it interfere with the screenshot itself.
				dev.xeys.visuals.Xeys.LOG.warn("Could not capture the screenshot preview", e);
			}
		}
	}

	private void acceptThumbnail(NativeImage image) {
		try (image) {
			this.thumbnail.upload(image);
			this.aspect = (float) image.getWidth() / Math.max(1, image.getHeight());
			this.previewAt = Clock.time();
		}
	}

	@Override
	protected void onDisable() {
		this.thumbnail.close();
	}

	@Override
	public boolean aboveHud() {
		return true;
	}

	@Override
	public void renderOverlay(GuiGraphics g, float partialTick) {
		float w = g.guiWidth();
		float h = g.guiHeight();
		float fa = (float) (Clock.time() - this.flashAt);
		if (this.flash.on() && fa >= 0f && fa < FLASH) {
			// A short full-strength hold, then a soft decay, like a camera flash.
			float t = Math.max(0f, (fa - 0.05f) / (FLASH - 0.05f));
			float a = this.strength.value() / 100f * (1f - t) * (1f - t);
			Draw.rect(g, 0f, 0f, w, h, 0f, Colors.withAlpha(0xFFFFFF, a));
		}
		float age = (float) (Clock.time() - this.previewAt);
		float total = this.hold.value() + 0.45f;
		if (!this.preview.on() || age < 0f || age > total || this.thumbnail.view() == null) {
			return;
		}
		float in = Easing.OUT_CUBIC.apply(Math.min(age / 0.4f, 1f));
		float out = age > this.hold.value() ? Easing.IN_CUBIC.apply((age - this.hold.value()) / 0.45f) : 0f;
		float tw = 132f;
		float th = tw / this.aspect;
		float cardW = tw + 12f;
		float cardH = th + 30f;
		float x = w - cardW - 10f + (1f - in + out) * (cardW + 16f);
		float y = h - cardH - 10f;
		float a = Math.min(in * 1.4f, 1f) * (1f - out);
		Draw.shadow(g, x, y, cardW, cardH, 10f, 0f, 4f, 16f, 0f, Colors.withAlpha(0x000000, 0.5f * a));
		Draw.rect(g, x, y, cardW, cardH).radius(10f).color(Colors.withAlpha(0xFF0E0E11, 0.96f * a)).border(1f, Colors.withAlpha(0xFFFFFF, 0.1f * a))
			.submit();
		Draw.image(g, this.thumbnail.view(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR), x + 6f, y + 6f, tw, th, 0f, 0f, 1f, 1f)
			.radius(6f).color(Colors.withAlpha(0xFFFFFF, a)).submit();
		Text.icon(g, Icons.CIRCLE_CHECK, x + 13f, y + th + 18f, 9f, Colors.mulAlpha(Theme.SUCCESS, a));
		Text.drawMiddle(g, Fonts.SEMIBOLD, "Screenshot saved", x + 21f, y + th + 18f, 8.5f, Colors.mulAlpha(Theme.TEXT, a), Text.Align.LEFT);
	}
}
