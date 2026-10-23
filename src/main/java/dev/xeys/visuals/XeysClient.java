package dev.xeys.visuals;

import dev.xeys.visuals.render.XeysPipelines;
import dev.xeys.visuals.render.XeysVertexFormats;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.ui.menu.MoonSky;
import dev.xeys.visuals.ui.menu.WorldEntryTransition;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public final class XeysClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		XeysVertexFormats.bootstrap();
		XeysPipelines.bootstrap();

		ResourceLoader loader = ResourceLoader.get(PackType.CLIENT_RESOURCES);
		loader.registerReloader(Xeys.id("fonts"), Fonts.reloader());
		loader.registerReloader(Xeys.id("moon"), MoonSky.reloader());
		loader.registerReloader(Xeys.id("pipelines"), (ResourceManagerReloadListener) manager -> XeysPipelines.precompileAll());
		loader.addReloaderOrdering(ResourceReloaderKeys.Client.SHADERS, Xeys.id("pipelines"));

		WorldEntryTransition.register();

		Xeys.LOG.info("{} initialised", Xeys.NAME);
	}
}
