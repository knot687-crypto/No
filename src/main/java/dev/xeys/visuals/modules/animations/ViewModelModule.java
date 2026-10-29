package dev.xeys.visuals.modules.animations;

import dev.xeys.visuals.anim.HandFx;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;

/** Move, turn and resize your hands and held items; the left hand mirrors the right. */
public final class ViewModelModule extends Module {
	private final NumberSetting x = this.position("x", "Left / Right", 0f);
	private final NumberSetting y = this.position("y", "Down / Up", 0f);
	private final NumberSetting z = this.position("z", "Near / Far", 0f);
	private final NumberSetting rotX = this.rotation("rot_x", "Pitch");
	private final NumberSetting rotY = this.rotation("rot_y", "Yaw");
	private final NumberSetting rotZ = this.rotation("rot_z", "Roll");
	private final NumberSetting scale = this.add(new NumberSetting("scale", "Size", 85f, 25f, 200f, 5f).unit("%").onChange(v -> this.apply())
		.group("Size"));
	private final BoolSetting bothHands = this.add(new BoolSetting("both_hands", "Both Hands", true)
		.describe("Apply to the off hand too (mirrored)").onChange(v -> this.apply()).group("Size"));

	public ViewModelModule() {
		super("view_model", "View Model", Category.ANIMATIONS, Icons.MOVE_3D, "Position, rotate and resize your hands");
	}

	private NumberSetting position(String key, String name, float value) {
		return this.add(new NumberSetting(key, name, value, -0.6f, 0.6f, 0.01f).onChange(v -> this.apply()).group("Position"));
	}

	private NumberSetting rotation(String key, String name) {
		return this.add(new NumberSetting(key, name, 0f, -180f, 180f, 1f).unit("°").onChange(v -> this.apply()).group("Rotation"));
	}

	private void apply() {
		if (!this.isEnabled()) {
			return;
		}
		HandFx.offX = this.x.value();
		HandFx.offY = this.y.value();
		// Positive "far" pushes the hand away from the camera, which looks down -z.
		HandFx.offZ = -this.z.value();
		HandFx.rotX = this.rotX.value();
		HandFx.rotY = this.rotY.value();
		HandFx.rotZ = this.rotZ.value();
		HandFx.scale = this.scale.value() / 100f;
		HandFx.bothHands = this.bothHands.on();
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		HandFx.offX = 0f;
		HandFx.offY = 0f;
		HandFx.offZ = 0f;
		HandFx.rotX = 0f;
		HandFx.rotY = 0f;
		HandFx.rotZ = 0f;
		HandFx.scale = 1f;
		HandFx.bothHands = true;
	}
}
