package dev.xeys.visuals;

import dev.xeys.visuals.core.Keybinds;
import dev.xeys.visuals.core.config.ConfigManager;
import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.core.module.ModuleManager;
import dev.xeys.visuals.hud.HudRenderer;
import dev.xeys.visuals.modules.Modules;
import dev.xeys.visuals.render.XeysPipelines;
import dev.xeys.visuals.render.XeysVertexFormats;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.ui.menu.MoonSky;
import dev.xeys.visuals.ui.menu.WorldEntryTransition;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
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
		loader.registerReloader(Xeys.id("glint"), (ResourceManagerReloadListener) manager -> dev.xeys.visuals.world.GlintTextures.onReload());
		loader.addReloaderOrdering(ResourceReloaderKeys.Client.TEXTURES, Xeys.id("glint"));

		Modules.init();
		ConfigManager.init();
		dev.xeys.visuals.core.Session.init();

		Keybinds.register();
		WorldEntryTransition.register();
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Xeys.id("hud"), HudRenderer::render);

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			Events.clientTick();
			ConfigManager.tick();
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> ConfigManager.save());

		net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
			if (renderer instanceof net.minecraft.client.renderer.entity.player.AvatarRenderer<?> avatar) {
				helper.register(new dev.xeys.visuals.cosmetic.CosmeticLayer(avatar));
			}
		});

		WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
			for (Object l : WorldEvents.AFTER_ENTITIES.snapshot()) {
				((WorldEvents.AfterEntities) l).onAfterEntities(ctx);
			}
		});
		WorldRenderEvents.BEFORE_TRANSLUCENT.register(ctx -> {
			for (Object l : WorldEvents.BEFORE_TRANSLUCENT.snapshot()) {
				((WorldEvents.BeforeTranslucent) l).onBeforeTranslucent(ctx);
			}
		});
		WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((ctx, outline) -> {
			boolean vanilla = true;
			for (Object l : WorldEvents.BLOCK_OUTLINE.snapshot()) {
				vanilla &= ((WorldEvents.BlockOutline) l).onBlockOutline(ctx, outline);
			}
			return vanilla;
		});
		WorldRenderEvents.END_MAIN.register(ctx -> {
			for (Object l : WorldEvents.END_MAIN.snapshot()) {
				((WorldEvents.EndMain) l).onEndMain(ctx);
			}
		});

		Xeys.LOG.info("{} initialised with {} modules", Xeys.NAME, ModuleManager.count());
	}
}
