package dev.xeys.visuals.test;

import dev.xeys.visuals.ui.dev.RenderTestScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.TitleScreen;

public final class XeysClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(1600, 900);
		context.waitTicks(5);
		// Main menu: cinematic intro, settled state, hover.
		context.setScreen(TitleScreen::new);
		context.waitTicks(8);
		context.takeScreenshot("menu_intro");
		context.waitTicks(50);
		context.takeScreenshot("menu_settled");
		context.getInput().setCursorPos(800, 806);
		context.waitTicks(12);
		context.takeScreenshot("menu_hover");

		// Click Singleplayer: exit transition, then the world list fading in.
		context.getInput().setCursorPos(570, 806);
		context.waitTicks(2);
		context.getInput().pressMouse(0);
		context.waitTicks(6);
		context.takeScreenshot("menu_exit_mid");
		context.waitTicks(14);
		context.takeScreenshot("menu_after_exit");
		context.setScreen(TitleScreen::new);
		context.waitTicks(20);
		context.takeScreenshot("menu_return");

		// Resolution independence.
		context.getInput().resizeWindow(854, 480);
		context.waitTicks(10);
		context.takeScreenshot("menu_854x480");
		context.getInput().resizeWindow(2560, 1440);
		context.waitTicks(10);
		context.takeScreenshot("menu_2560x1440");
		context.getInput().resizeWindow(1600, 900);
		context.waitTicks(5);

		context.setScreen(RenderTestScreen::new);
		context.waitTicks(10);
		context.takeScreenshot("render_test");

		// World entry: fade from black with an easing field of view. (Software GL in CI competes with
		// world generation for CPU, so worlds are created at a moderate window size.)
		context.getInput().resizeWindow(1280, 720);
		context.waitTicks(2);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.takeScreenshot("world_entry_early");
			context.waitTicks(3);
			context.takeScreenshot("world_entry_mid");
			world.getClientWorld().waitForChunksRender();
			context.waitTicks(40);
			context.takeScreenshot("world_entry_done");
		}
		context.setScreen(TitleScreen::new);
		context.waitTicks(2);
	}
}
