package dev.xeys.visuals.render.post;

/**
 * Parameters of the world post-processing pass for one frame. Enabled modules fill in their effect,
 * starting from "off"; if nothing turns an effect on, the pass is skipped.
 */
public final class WorldFx {
	/** Fraction of each frame's camera motion smeared into the image (0 = off). */
	public float motion;
	/** Samples along the motion vector. */
	public int motionSamples;
	/** Longest blur, as a fraction of the screen width. */
	public float motionMax;

	/** Depth of field strength (0 = off). */
	public float dof;
	/** Distance in focus, in blocks. */
	public float focus;
	/** How quickly blur grows away from the focus distance. */
	public float aperture;
	/** Blur levels of the wide defocus blur. */
	public int dofLevels;

	/** Bloom strength (0 = off). */
	public float bloom;
	/** Luminance above which pixels glow. */
	public float bloomThreshold;
	/** Bloom blur levels (radius roughly doubles per level). */
	public int bloomLevels;
	public int bloomTint;

	public WorldFx() {
		this.reset();
	}

	public void reset() {
		this.motion = 0f;
		this.motionSamples = 8;
		this.motionMax = 0.06f;
		this.dof = 0f;
		this.focus = 8f;
		this.aperture = 6f;
		this.dofLevels = 3;
		this.bloom = 0f;
		this.bloomThreshold = 0.72f;
		this.bloomLevels = 5;
		this.bloomTint = 0xFFFFFFFF;
	}

	public boolean active() {
		return this.motion > 0f || this.dof > 0f || this.bloom > 0f;
	}
}
