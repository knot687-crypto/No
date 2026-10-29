package dev.xeys.visuals.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.xeys.visuals.Xeys;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * Every pipeline XEYS renders with.
 *
 * <p>These are deliberately <em>not</em> registered as vanilla static pipelines: a static pipeline that
 * fails to compile aborts resource loading. Instead they are precompiled after every shader reload by
 * {@link #precompileAll()} (so first use never hitches) and a pipeline that fails on an exotic driver
 * only disables the feature using it, with the error in the log.
 */
public final class XeysPipelines {
	private static final List<RenderPipeline> ALL = new ArrayList<>();
	private static final Map<RenderPipeline, Boolean> VALID = Collections.synchronizedMap(new IdentityHashMap<>());

	/** GUI shapes: premultiplied alpha output, no depth. */
	private static final RenderPipeline.Snippet GUI_SHAPE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
		.withVertexShader(Xeys.id("core/shape"))
		.withFragmentShader(Xeys.id("core/shape"))
		.withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
		.withVertexFormat(XeysVertexFormats.SHAPE, VertexFormat.Mode.QUADS)
		.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
		.withDepthWrite(false)
		.withCull(false)
		.buildSnippet();

	public static final RenderPipeline RECT = register(RenderPipeline.builder(GUI_SHAPE)
		.withLocation(Xeys.id("pipeline/gui_rect"))
		.withShaderDefine("SHAPE_RECT")
		.build());

	public static final RenderPipeline ARC = register(RenderPipeline.builder(GUI_SHAPE)
		.withLocation(Xeys.id("pipeline/gui_arc"))
		.withShaderDefine("SHAPE_ARC")
		.build());

	public static final RenderPipeline LINE = register(RenderPipeline.builder(GUI_SHAPE)
		.withLocation(Xeys.id("pipeline/gui_line"))
		.withShaderDefine("SHAPE_LINE")
		.build());

	/** Rounded rect filled with the blurred scene (frosted glass). Sampler0 = blurred scene. */
	public static final RenderPipeline GLASS = register(RenderPipeline.builder(GUI_SHAPE)
		.withLocation(Xeys.id("pipeline/gui_glass"))
		.withShaderDefine("SHAPE_RECT")
		.withShaderDefine("SHAPE_GLASS")
		.withSampler("Sampler0")
		.build());

	/** Rounded rect filled with a texture. Sampler0 = image, ParamsC = (u0, v0, u1, v1). */
	public static final RenderPipeline IMAGE = register(RenderPipeline.builder(GUI_SHAPE)
		.withLocation(Xeys.id("pipeline/gui_image"))
		.withShaderDefine("SHAPE_RECT")
		.withShaderDefine("SHAPE_IMAGE")
		.withSampler("Sampler0")
		.build());

	/** Signed-distance-field text. Sampler0 = glyph atlas (single channel). */
	public static final RenderPipeline TEXT = register(RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
		.withLocation(Xeys.id("pipeline/gui_text"))
		.withVertexShader(Xeys.id("core/sdf_text"))
		.withFragmentShader(Xeys.id("core/sdf_text"))
		.withSampler("Sampler0")
		.withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
		.withVertexFormat(XeysVertexFormats.TEXT, VertexFormat.Mode.QUADS)
		.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
		.withDepthWrite(false)
		.withCull(false)
		.build());

	/** Full-screen passes: no vertex data, a single triangle generated from gl_VertexID. */
	private static final RenderPipeline.Snippet FULLSCREEN = RenderPipeline.builder()
		.withVertexShader(Xeys.id("core/fullscreen"))
		.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
		.withDepthWrite(false)
		.withCull(false)
		.withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
		.buildSnippet();

	public static final RenderPipeline BLUR_DOWN = register(RenderPipeline.builder(FULLSCREEN)
		.withLocation(Xeys.id("pipeline/blur_down"))
		.withFragmentShader(Xeys.id("post/kawase_down"))
		.withSampler("InSampler")
		.withUniform("BlurConfig", UniformType.UNIFORM_BUFFER)
		.withoutBlend()
		.build());

	public static final RenderPipeline BLUR_UP = register(RenderPipeline.builder(FULLSCREEN)
		.withLocation(Xeys.id("pipeline/blur_up"))
		.withFragmentShader(Xeys.id("post/kawase_up"))
		.withSampler("InSampler")
		.withUniform("BlurConfig", UniformType.UNIFORM_BUFFER)
		.withoutBlend()
		.build());

	/** The main-menu sky: stars, meteors and the ray-traced moon in one full-screen pass. */
	public static final RenderPipeline MOON_SKY = register(RenderPipeline.builder(FULLSCREEN)
		.withLocation(Xeys.id("pipeline/moon_sky"))
		.withFragmentShader(Xeys.id("core/moon_sky"))
		.withSampler("MoonAlbedo")
		.withSampler("MoonNormal")
		.withSampler("SkyCache")
		.withUniform("MoonConfig", UniformType.UNIFORM_BUFFER)
		.withoutBlend()
		.build());

	/** Opaque full-screen copy of a texture. */
	public static final RenderPipeline BLIT = register(RenderPipeline.builder(FULLSCREEN)
		.withLocation(Xeys.id("pipeline/blit"))
		.withFragmentShader(Xeys.id("post/blit"))
		.withSampler("InSampler")
		.withoutBlend()
		.build());

	/** All 2D screen effects (vignette, grain, chromatic aberration, grading...) in one pass. */
	public static final RenderPipeline SCREEN_FX = register(RenderPipeline.builder(FULLSCREEN)
		.withLocation(Xeys.id("pipeline/screen_fx"))
		.withFragmentShader(Xeys.id("post/screen_fx"))
		.withSampler("InSampler")
		.withUniform("ScreenFx", UniformType.UNIFORM_BUFFER)
		.withoutBlend()
		.build());

	/** First bloom downsample: keeps only what is brighter than a threshold. */
	public static final RenderPipeline BLOOM_PREFILTER = register(RenderPipeline.builder(FULLSCREEN)
		.withLocation(Xeys.id("pipeline/bloom_prefilter"))
		.withFragmentShader(Xeys.id("post/bloom_prefilter"))
		.withSampler("InSampler")
		.withUniform("BlurConfig", UniformType.UNIFORM_BUFFER)
		.withoutBlend()
		.build());

	/** Bloom upsample: adds the blurred deeper level onto this level, accumulating every radius. */
	public static final RenderPipeline BLOOM_UP = register(RenderPipeline.builder(FULLSCREEN)
		.withLocation(Xeys.id("pipeline/bloom_up"))
		.withFragmentShader(Xeys.id("post/kawase_up"))
		.withSampler("InSampler")
		.withUniform("BlurConfig", UniformType.UNIFORM_BUFFER)
		.withBlend(BlendFunction.ADDITIVE)
		.build());

	/** Motion blur, depth of field and bloom over the world image, before the hand is drawn. */
	public static final RenderPipeline WORLD_POST = register(RenderPipeline.builder(FULLSCREEN)
		.withLocation(Xeys.id("pipeline/world_post"))
		.withFragmentShader(Xeys.id("post/world_post"))
		.withSampler("SceneSampler")
		.withSampler("DepthSampler")
		.withSampler("DofSampler")
		.withSampler("BloomSampler")
		.withUniform("WorldPost", UniformType.UNIFORM_BUFFER)
		.withoutBlend()
		.build());

	/** Additive sky layers drawn in vanilla's sky pass, on the celestial sphere. */
	private static final RenderPipeline.Snippet SKY_FX = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
		.withVertexShader(Xeys.id("core/sky_fx"))
		.withFragmentShader(Xeys.id("core/sky_fx"))
		.withUniform("SkyFx", UniformType.UNIFORM_BUFFER)
		.withBlend(BlendFunction.OVERLAY)
		.withDepthWrite(false)
		.withCull(false)
		.withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
		.buildSnippet();

	public static final RenderPipeline SKY_AURORA = register(RenderPipeline.builder(SKY_FX)
		.withLocation(Xeys.id("pipeline/sky_aurora"))
		.withShaderDefine("SKY_AURORA")
		.build());

	public static final RenderPipeline SKY_METEOR = register(RenderPipeline.builder(SKY_FX)
		.withLocation(Xeys.id("pipeline/sky_meteor"))
		.withShaderDefine("SKY_METEOR")
		.build());

	public static final RenderPipeline SKY_STARS = register(RenderPipeline.builder(SKY_FX)
		.withLocation(Xeys.id("pipeline/sky_stars"))
		.withShaderDefine("SKY_STARS")
		.build());

	/** Additive, fogged glow sprites in the world (fireflies, motes, beams). Depth-tested, never written. */
	private static final RenderPipeline.Snippet WORLD_GLOW = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
		.withVertexShader(Xeys.id("core/world_glow"))
		.withFragmentShader(Xeys.id("core/world_glow"))
		.withBlend(BlendFunction.LIGHTNING)
		.withDepthWrite(false)
		.withCull(false)
		.withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
		.buildSnippet();

	public static final RenderPipeline WORLD_GLOW_DOT = register(RenderPipeline.builder(WORLD_GLOW)
		.withLocation(Xeys.id("pipeline/world_glow_dot"))
		.withShaderDefine("GLOW_DOT")
		.build());

	public static final RenderPipeline WORLD_GLOW_BEAM = register(RenderPipeline.builder(WORLD_GLOW)
		.withLocation(Xeys.id("pipeline/world_glow_beam"))
		.withShaderDefine("GLOW_BEAM")
		.build());

	public static final RenderPipeline WORLD_GLOW_RING = register(RenderPipeline.builder(WORLD_GLOW)
		.withLocation(Xeys.id("pipeline/world_glow_ring"))
		.withShaderDefine("GLOW_RING")
		.build());

	/** Static sky gradient and nebula, rendered once per resize into a half-resolution cache. */
	public static final RenderPipeline MOON_NEBULA = register(RenderPipeline.builder(FULLSCREEN)
		.withLocation(Xeys.id("pipeline/moon_nebula"))
		.withFragmentShader(Xeys.id("core/moon_nebula"))
		.withUniform("MoonConfig", UniformType.UNIFORM_BUFFER)
		.withoutBlend()
		.build());

	private XeysPipelines() {
	}

	private static RenderPipeline register(RenderPipeline pipeline) {
		ALL.add(pipeline);
		return pipeline;
	}

	public static void bootstrap() {
		// Class initialisation creates the pipelines.
	}

	/** Compiles every pipeline against the current shader sources. Render thread only. */
	public static void precompileAll() {
		GpuDevice device = RenderSystem.getDevice();
		int failed = 0;
		for (RenderPipeline pipeline : ALL) {
			boolean ok;
			try {
				ok = device.precompilePipeline(pipeline).isValid();
			} catch (RuntimeException e) {
				Xeys.LOG.error("Pipeline {} threw while compiling", pipeline.getLocation(), e);
				ok = false;
			}
			VALID.put(pipeline, ok);
			if (!ok) {
				failed++;
				Xeys.LOG.error("Pipeline {} failed to compile; the features using it are disabled", pipeline.getLocation());
			}
		}
		Xeys.LOG.info("Compiled {} render pipelines ({} failed)", ALL.size(), failed);
	}

	/** False only when the pipeline is known to have failed compilation. */
	public static boolean isUsable(RenderPipeline pipeline) {
		return VALID.getOrDefault(pipeline, Boolean.TRUE);
	}
}
