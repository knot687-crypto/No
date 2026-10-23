package dev.xeys.visuals.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

/**
 * Vertex formats used by the XEYS 2D renderer.
 *
 * <p>Shapes are drawn as single quads whose fragment shader evaluates a signed distance field, so every
 * parameter the shader needs travels as a per-vertex attribute. That keeps any number of shapes in one
 * batch (the GUI renderer batches by pipeline) and keeps edges analytically anti-aliased at every scale.
 *
 * <p>Custom elements use the UV usage (the only usage allowed at a non-zero index) with float storage,
 * which the GL backend binds through {@code glVertexAttribPointer}. IDs are picked from the free slots at
 * class-load time so they never collide with elements registered by other mods.
 */
public final class XeysVertexFormats {
	/** Shape params A: half width, half height, border width, softness (shadow blur). */
	public static final VertexFormatElement PARAMS_A = registerFree(3);
	/** Shape params B: corner radii (top-left, top-right, bottom-right, bottom-left). */
	public static final VertexFormatElement PARAMS_B = registerFree(4);
	/** Shape params C: secondary color (border / outline / glow) as straight RGBA floats. */
	public static final VertexFormatElement PARAMS_C = registerFree(5);
	/** Shape params D: border bottom fade, inner glow size, inner glow strength, additive amount. */
	public static final VertexFormatElement PARAMS_D = registerFree(6);
	/** Clip rectangle in framebuffer pixels (x0, y0, x1, y1), origin top-left. */
	public static final VertexFormatElement CLIP = registerFree(7);

	/**
	 * Position.z carries the clip feather in pixels; the vertex shader never uses z for placement.
	 */
	public static final VertexFormat SHAPE = VertexFormat.builder()
		.add("Position", VertexFormatElement.POSITION)
		.add("Color", VertexFormatElement.COLOR)
		.add("UV0", VertexFormatElement.UV0)
		.add("ParamsA", PARAMS_A)
		.add("ParamsB", PARAMS_B)
		.add("ParamsC", PARAMS_C)
		.add("ParamsD", PARAMS_D)
		.add("Clip", CLIP)
		.build();

	/**
	 * Text: UV0 is the atlas coordinate, ParamsA = (weight offset px, outline width px, softness px, unused),
	 * ParamsC = outline / glow color.
	 */
	public static final VertexFormat TEXT = VertexFormat.builder()
		.add("Position", VertexFormatElement.POSITION)
		.add("Color", VertexFormatElement.COLOR)
		.add("UV0", VertexFormatElement.UV0)
		.add("ParamsA", PARAMS_A)
		.add("ParamsC", PARAMS_C)
		.add("Clip", CLIP)
		.build();

	private XeysVertexFormats() {
	}

	private static VertexFormatElement registerFree(int uvIndex) {
		for (int id = VertexFormatElement.MAX_COUNT - 1; id > 6; id--) {
			if (VertexFormatElement.byId(id) == null) {
				return VertexFormatElement.register(id, uvIndex, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.UV, 4);
			}
		}
		throw new IllegalStateException("No free vertex format element slots left");
	}

	public static void bootstrap() {
		// Forces class initialisation (element registration) on the render thread during client init.
	}
}
