package dev.xeys.visuals.hud;

import dev.xeys.visuals.anim.Clock;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** Small shared calculations for HUD elements. */
public final class HudMath {
	private static final String[] POINTS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
	private static final String[] POINT_NAMES = {"North", "Northeast", "East", "Southeast", "South", "Southwest", "West", "Northwest"};

	private HudMath() {
	}

	/**
	 * Compass bearing of the camera in degrees, 0 = north, 90 = east, interpolated for this frame. Without
	 * a player (menu previews) it turns slowly so previews show the element moving.
	 */
	public static float heading(float partialTick) {
		Player p = Minecraft.getInstance().player;
		if (p == null) {
			return (float) ((Clock.time() * 14.0) % 360.0);
		}
		float h = (p.getViewYRot(partialTick) + 180f) % 360f;
		return h < 0f ? h + 360f : h;
	}

	/** Index 0..7 of the nearest compass point (N, NE, E, ...). */
	public static int point(float heading) {
		return Math.floorMod(Math.round(heading / 45f), 8);
	}

	public static String pointLabel(int index) {
		return POINTS[index];
	}

	public static String pointName(int index) {
		return POINT_NAMES[index];
	}

	public static float smoothstep(float edge0, float edge1, float x) {
		float t = Math.max(0f, Math.min((x - edge0) / (edge1 - edge0), 1f));
		return t * t * (3f - 2f * t);
	}
}
