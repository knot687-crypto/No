package dev.xeys.visuals.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;

/** Vanilla screens with the restyled widgets. */
public final class XeysWidgetGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!TestFilter.enabled("widgets")) {
			return;
		}
		context.getInput().resizeWindow(1280, 720);
		context.runOnClient(c -> {
			c.options.guiScale().set(2);
			c.resizeDisplay();
		});
		context.waitTicks(2);
		context.setScreen(() -> new OptionsScreen(new TitleScreen(), Minecraft.getInstance().options));
		context.waitTicks(10);
		context.getInput().setCursorPos(500, 300);
		context.waitTicks(6);
		context.takeScreenshot("widgets_options");
		context.setScreen(() -> new VideoSettingsScreen(new TitleScreen(), Minecraft.getInstance(), Minecraft.getInstance().options));
		context.waitTicks(10);
		context.takeScreenshot("widgets_video");
		context.setScreen(TitleScreen::new);
		context.waitTicks(4);
		context.runOnClient(c -> CreateWorldScreen.openFresh(c, () -> c.setScreen(new TitleScreen())));
		context.waitTicks(20);
		context.takeScreenshot("widgets_create_world");
		context.setScreen(TitleScreen::new);
		context.waitTicks(4);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getClientWorld().waitForChunksRender();
			context.setScreen(() -> new PauseScreen(true));
			context.waitTicks(15);
			context.getInput().setCursorPos(640, 250);
			context.waitTicks(6);
			context.takeScreenshot("widgets_pause");
			context.setScreen(() -> null);
			context.waitTicks(2);
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("kill @a");
			context.waitTicks(10);
			context.takeScreenshot("death_early");
			context.waitTicks(30);
			context.takeScreenshot("death_settled");
			context.runOnClient(c -> c.player.respawn());
			context.waitTicks(10);
			context.setScreen(() -> null);
		}
		net.minecraft.client.multiplayer.LevelLoadTracker tracker = new net.minecraft.client.multiplayer.LevelLoadTracker();
		tracker.start(net.minecraft.server.level.progress.LevelLoadListener.Stage.LOAD_INITIAL_CHUNKS, 100);
		tracker.update(net.minecraft.server.level.progress.LevelLoadListener.Stage.LOAD_INITIAL_CHUNKS, 64, 100);
		context.setScreen(() -> new net.minecraft.client.gui.screens.LevelLoadingScreen(tracker,
			net.minecraft.client.gui.screens.LevelLoadingScreen.Reason.OTHER));
		context.waitTicks(30);
		context.takeScreenshot("loading_screen");
		context.runOnClient(c -> dev.xeys.visuals.core.module.ModuleManager.get("screen_transitions").setEnabled(true));
		context.setScreen(TitleScreen::new);
		context.waitTicks(4);
		context.setScreen(() -> new OptionsScreen(new TitleScreen(), Minecraft.getInstance().options));
		context.takeScreenshot("transition_mid");
		context.setScreen(TitleScreen::new);
		context.waitTicks(2);
	}
}
