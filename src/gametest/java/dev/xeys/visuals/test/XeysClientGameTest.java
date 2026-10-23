package dev.xeys.visuals.test;

import dev.xeys.visuals.ui.clickgui.ClickGuiScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;

public final class XeysClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(1600, 900);
		context.waitTicks(5);
		context.setScreen(() -> new ClickGuiScreen(null));
		context.waitTicks(4);
		context.takeScreenshot("gui_open_anim");
		context.waitTicks(20);
		// Window: 1040x680 design units centered in 1920x1080 -> origin (440, 200).
		clickDesign(context, 550, 200 + 92 + 5 * 40 + 18);
		context.waitTicks(15);
		context.takeScreenshot("gui_client");
		// Open the Theme card (first card, left column).
		clickDesign(context, 800, 200 + 88 + 37);
		context.waitTicks(15);
		context.takeScreenshot("gui_theme_settings");
		// Accent color swatch (first row, right side) opens the picker.
		clickDesign(context, 440 + 1040 - 26 - 30, 200 + 8 + 92 + 34 + 23);
		context.waitTicks(10);
		context.takeScreenshot("gui_color_picker");
		context.getInput().pressKey(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE);
		context.waitTicks(6);
		// Profiles popover.
		clickDesign(context, 520, 200 + 680 - 108 + 46 + 20);
		context.waitTicks(10);
		context.takeScreenshot("gui_profiles");
		context.setScreen(TitleScreen::new);
		context.waitTicks(2);
	}

	/** Clicks at design coordinates (1080 units tall) by converting to window pixels. */
	private static void clickDesign(ClientGameTestContext context, double dx, double dy) {
		double f = 900.0 / 1080.0;
		context.getInput().setCursorPos(dx * f, dy * f);
		context.waitTicks(2);
		context.getInput().pressMouse(0);
		context.waitTicks(2);
	}
}
