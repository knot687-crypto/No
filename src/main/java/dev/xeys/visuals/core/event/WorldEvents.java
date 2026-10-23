package dev.xeys.visuals.core.event;

import dev.xeys.visuals.core.module.Module;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;

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

	public static final Listeners<AfterEntities> AFTER_ENTITIES = new Listeners<>();
	public static final Listeners<BeforeTranslucent> BEFORE_TRANSLUCENT = new Listeners<>();
	public static final Listeners<EndMain> END_MAIN = new Listeners<>();

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
	}
}
