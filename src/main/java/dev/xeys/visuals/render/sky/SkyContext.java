package dev.xeys.visuals.render.sky;

/** What the sky pass knows this frame, as vanilla's sky renderer received it. */
public final class SkyContext {
	/** Celestial rotations in radians (vanilla's sky angles). */
	public float sunAngle;
	public float moonAngle;
	public float starAngle;
	/** 1 in clear weather, falling toward 0 in rain. */
	public float rainBrightness;
	/** Vanilla star brightness: 0 by day, about 0.5 on a clear night. */
	public float starBrightness;
	public float partialTick;

	/** 0 by day, 1 on a clear night: how visible night-sky effects should be. */
	public float night() {
		return Math.min(1f, this.starBrightness * 2f);
	}
}
