package dev.xeys.visuals.render.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Central camera adjustments. Features register FOV multipliers here instead of each mixing into the
 * camera, so effects compose predictably (e.g. smooth zoom during the world-entry ease-in).
 */
public final class CameraFx {
	private static final List<FovModifier> FOV = new ArrayList<>();

	private CameraFx() {
	}

	public static void addFovModifier(FovModifier modifier) {
		FOV.add(modifier);
	}

	/** Product of all active multipliers. Called from the {@code GameRenderer#getFov} hook. */
	public static float fovMultiplier(float partialTick) {
		float m = 1f;
		for (int i = 0, n = FOV.size(); i < n; i++) {
			m *= FOV.get(i).multiplier(partialTick);
		}
		return m;
	}

	@FunctionalInterface
	public interface FovModifier {
		float multiplier(float partialTick);
	}
}
