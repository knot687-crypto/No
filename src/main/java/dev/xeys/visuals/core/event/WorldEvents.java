package dev.xeys.visuals.core.event;

import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.render.post.WorldFx;
import dev.xeys.visuals.render.sky.SkyContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;

/** World-render hooks (Fabric WorldRenderEvents), dispatched to enabled modules only. */
public final class WorldEvents {
	public interface AfterEntities {
		void onAfterEntities(WorldRenderContext context);
	}

	public interface BeforeTranslucent {
		void onBeforeTranslucent(WorldRenderContext context);
	}

	public interface EndMain {
		void onEndMain(WorldRenderContext context);
	}

	/** Draws over or instead of the block outline; return false to suppress vanilla's outline. */
	public interface BlockOutline {
		boolean onBlockOutline(WorldRenderContext context, BlockOutlineRenderState outline);
	}

	/** Adds to the world post-processing pass (motion blur, depth of field, bloom), before the hand is drawn. */
	public interface WorldPostEffect {
		void contributeWorld(WorldFx fx, float partialTick);
	}

	/** Draws into the overworld sky, after the sun, moon and stars and before any terrain. */
	public interface Sky {
		void onSky(SkyContext sky);
	}

	public static final Listeners<AfterEntities> AFTER_ENTITIES = new Listeners<>();
	public static final Listeners<WorldPostEffect> WORLD_POST = new Listeners<>();
	public static final Listeners<Sky> SKY = new Listeners<>();
	public static final Listeners<BeforeTranslucent> BEFORE_TRANSLUCENT = new Listeners<>();
	public static final Listeners<EndMain> END_MAIN = new Listeners<>();
	public static final Listeners<BlockOutline> BLOCK_OUTLINE = new Listeners<>();

	private WorldEvents() {
	}

	static void subscribe(Module module) {
		if (module instanceof AfterEntities l) {
			AFTER_ENTITIES.add(l);
		}
		if (module instanceof BeforeTranslucent l) {
			BEFORE_TRANSLUCENT.add(l);
		}
		if (module instanceof EndMain l) {
			END_MAIN.add(l);
		}
		if (module instanceof BlockOutline l) {
			BLOCK_OUTLINE.add(l);
		}
		if (module instanceof WorldPostEffect l) {
			WORLD_POST.add(l);
		}
		if (module instanceof Sky l) {
			SKY.add(l);
		}
	}

	static void unsubscribe(Module module) {
		if (module instanceof AfterEntities l) {
			AFTER_ENTITIES.remove(l);
		}
		if (module instanceof BeforeTranslucent l) {
			BEFORE_TRANSLUCENT.remove(l);
		}
		if (module instanceof EndMain l) {
			END_MAIN.remove(l);
		}
		if (module instanceof BlockOutline l) {
			BLOCK_OUTLINE.remove(l);
		}
		if (module instanceof WorldPostEffect l) {
			WORLD_POST.remove(l);
		}
		if (module instanceof Sky l) {
			SKY.remove(l);
		}
	}
}
