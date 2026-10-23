package dev.xeys.visuals.test;

import dev.xeys.visuals.ui.dev.RenderTestScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class XeysClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(1600, 900);
		context.waitTicks(10);
		context.setScreen(RenderTestScreen::new);
		context.waitTicks(10);
		context.takeScreenshot("render_test");
		context.setScreen(() -> new TitleScreen());
		context.waitTicks(2);
	}
}
