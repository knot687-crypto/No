package dev.xeys.visuals.render.post;

/**
 * Parameters of the combined screen-effects pass for one frame. Every enabled screen-effect module adds
 * its contribution, starting from neutral values; if nothing changes them, the pass is skipped.
 */
public final class ScreenFx {
	public float vignette;
	public float vignetteRadius;
	public float vignetteSoftness;
	public float vignetteRoundness;
	public int vignetteColor;

	public float grain;
	public float grainSize;
	public float grainSpeed;

	public float chroma;
	public float chromaFalloff;

	public float scanlines;
	public float scanSpacing;
	public float scanSpeed;

	public float saturation;
	public float contrast;
	public float brightness;
	public float sharpen;

	public int tint;
	public float tintAmount;

	public float mono;
	public int monoMode;

	public int edgeColor;
	public float edge;

	public int flashColor;
	public float flash;

	public float pixelSize;
	public float letterbox;
	public float wave;
	public float waveSpeed;

	public int glowColor;
	public float glow;

	public ScreenFx() {
		this.reset();
	}

	public void reset() {
		this.vignette = 0f;
		this.vignetteRadius = 0.75f;
		this.vignetteSoftness = 0.45f;
		this.vignetteRoundness = 1f;
		this.vignetteColor = 0xFF000000;
		this.grain = 0f;
		this.grainSize = 1.5f;
		this.grainSpeed = 1f;
		this.chroma = 0f;
		this.chromaFalloff = 1.5f;
		this.scanlines = 0f;
		this.scanSpacing = 3f;
		this.scanSpeed = 0f;
		this.saturation = 1f;
		this.contrast = 1f;
		this.brightness = 0f;
		this.sharpen = 0f;
		this.tint = 0xFFFFFFFF;
		this.tintAmount = 0f;
		this.mono = 0f;
		this.monoMode = 0;
		this.edgeColor = 0xFF000000;
		this.edge = 0f;
		this.flashColor = 0xFFFFFFFF;
		this.flash = 0f;
		this.pixelSize = 0f;
		this.letterbox = 0f;
		this.wave = 0f;
		this.waveSpeed = 1f;
		this.glowColor = 0xFFFFFFFF;
		this.glow = 0f;
	}

	/** Whether anything differs from the untouched image. */
	public boolean active() {
		return this.vignette > 0f || this.grain > 0f || this.chroma > 0f || this.scanlines > 0f || this.saturation != 1f
			|| this.contrast != 1f || this.brightness != 0f || this.sharpen > 0f || this.tintAmount > 0f || this.mono > 0f
			|| this.edge > 0f || this.flash > 0f || this.pixelSize > 1f || this.letterbox > 0f || this.wave > 0f || this.glow > 0f;
	}
}
