package dev.xeys.visuals.modules.animations;

import dev.xeys.visuals.anim.AnimHooks;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

/** The third-person camera glides out from your head when you switch views, and can sit nearer or farther. */
public final class PerspectiveGlideModule extends Module implements Events.FrameStart {
	private final NumberSetting speed = this.add(new NumberSetting("speed", "Glide Speed", 100f, 25f, 300f, 5f).unit("%").group("Camera"));
	private final NumberSetting distance = this.add(new NumberSetting("distance", "Distance", 100f, 50f, 300f, 5f).unit("%")
		.describe("Third-person camera distance (blocks behind still stop it)").group("Camera"));
	private @Nullable CameraType last;
	private float glide = 1f;

	public PerspectiveGlideModule() {
		super("perspective_glide", "Perspective Glide", Category.ANIMATIONS, Icons.CAMERA, "Glide into third person; set its distance");
	}

	@Override
	public void onFrameStart() {
		CameraType type = Minecraft.getInstance().options.getCameraType();
		if (type != this.last) {
			if (!type.isFirstPerson()) {
				this.glide = 0.08f;
			}
			this.last = type;
		}
		this.glide += (1f - this.glide) * Clock.smoothing(7f * this.speed.value() / 100f);
		AnimHooks.cameraDistance = this.glide * this.distance.value() / 100f;
	}

	@Override
	protected void onEnable() {
		this.last = Minecraft.getInstance().options.getCameraType();
		this.glide = 1f;
	}

	@Override
	protected void onDisable() {
		AnimHooks.cameraDistance = 1f;
	}
}
