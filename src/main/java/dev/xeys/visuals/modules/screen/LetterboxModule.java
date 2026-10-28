package dev.xeys.visuals.modules.screen;

import dev.xeys.visuals.anim.Spring;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.render.post.ScreenFx;
import dev.xeys.visuals.ui.Icons;
import net.minecraft.client.Minecraft;

/** Cinematic black bars that frame the world at a wide aspect ratio; they slide in and out smoothly. */
public final class LetterboxModule extends ScreenEffectModule {
	public enum Aspect {
		SCOPE("2.39:1", 2.39f),
		UNIVISIUM("2:1", 2f),
		WIDESCREEN("1.85:1", 1.85f);

		final String label;
		final float ratio;

		Aspect(String label, float ratio) {
			this.label = label;
			this.ratio = ratio;
		}

		@Override
		public String toString() {
			return this.label;
		}
	}

	public enum When {
		ALWAYS("Always"),
		HIDDEN_HUD("With HUD Hidden"),
		THIRD_PERSON("Third Person");

		final String label;

		When(String label) {
			this.label = label;
		}

		@Override
		public String toString() {
			return this.label;
		}
	}

	private final EnumSetting<Aspect> aspect = this.add(new EnumSetting<>("aspect", "Aspect Ratio", Aspect.SCOPE).group("Letterbox"));
	private final EnumSetting<When> when = this.add(new EnumSetting<>("when", "Show", When.HIDDEN_HUD)
		.describe("Always, only with the HUD hidden (F1), or only in third person").group("Letterbox"));
	private final Spring bars = new Spring(0f, 160f, 1f);

	public LetterboxModule() {
		super("letterbox", "Letterbox", Icons.CLAPPERBOARD, "Cinematic widescreen bars");
	}

	@Override
	public void contribute(ScreenFx fx, float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		boolean show = switch (this.when.get()) {
			case ALWAYS -> true;
			case HIDDEN_HUD -> mc.options.hideGui;
			case THIRD_PERSON -> !mc.options.getCameraType().isFirstPerson();
		};
		float screen = (float) mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
		float target = show ? Math.max(0f, (1f - screen / this.aspect.get().ratio) * 0.5f) : 0f;
		this.bars.target(target);
		fx.letterbox = Math.max(0f, this.bars.get());
	}
}
