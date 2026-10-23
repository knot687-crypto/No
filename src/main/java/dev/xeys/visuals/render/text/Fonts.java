package dev.xeys.visuals.render.text;

import dev.xeys.visuals.Xeys;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * The XEYS typefaces: Inter in four weights for UI text, Inter Display Black for the wordmark and the
 * Lucide icon font. Atlases are baked offline and decoded during resource reload, uploaded on the render
 * thread.
 */
public final class Fonts {
	public static final XeysFont REGULAR = new XeysFont("inter_regular");
	public static final XeysFont MEDIUM = new XeysFont("inter_medium");
	public static final XeysFont SEMIBOLD = new XeysFont("inter_semibold");
	public static final XeysFont BOLD = new XeysFont("inter_bold");
	public static final XeysFont DISPLAY = new XeysFont("inter_display_black");
	public static final XeysFont ICONS = new XeysFont("icons");

	private static final List<XeysFont> ALL = List.of(REGULAR, MEDIUM, SEMIBOLD, BOLD, DISPLAY, ICONS);

	private Fonts() {
	}

	/** Resource reloader: decodes the shipped atlases on worker threads, then uploads them. */
	public static PreparableReloadListener reloader() {
		return (state, prepareExecutor, barrier, applyExecutor) -> {
			ResourceManager manager = state.resourceManager();
			List<CompletableFuture<FontAtlas>> futures = new ArrayList<>();
			for (XeysFont font : ALL) {
				futures.add(CompletableFuture.supplyAsync(() -> load(manager, font), prepareExecutor));
			}
			return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
				.thenApply(v -> {
					Map<XeysFont, FontAtlas> baked = new LinkedHashMap<>();
					for (int i = 0; i < ALL.size(); i++) {
						FontAtlas atlas = futures.get(i).join();
						if (atlas != null) {
							baked.put(ALL.get(i), atlas);
						}
					}
					return baked;
				})
				.thenCompose(barrier::wait)
				.thenAcceptAsync(baked -> baked.forEach(XeysFont::upload), applyExecutor);
		};
	}

	private static FontAtlas load(ResourceManager manager, XeysFont font) {
		Identifier id = Xeys.id("fonts/" + font.name() + ".sdf");
		try {
			Resource resource = manager.getResourceOrThrow(id);
			try (InputStream in = resource.open()) {
				return FontAtlas.load(font.name(), in.readAllBytes());
			}
		} catch (IOException | RuntimeException e) {
			Xeys.LOG.error("Failed to load font {}", id, e);
			return null;
		}
	}

	public static boolean ready() {
		for (XeysFont font : ALL) {
			if (!font.ready()) {
				return false;
			}
		}
		return true;
	}
}
