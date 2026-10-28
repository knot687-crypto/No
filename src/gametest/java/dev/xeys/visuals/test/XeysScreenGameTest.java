package dev.xeys.visuals.test;

import dev.xeys.visuals.core.module.ModuleManager;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.TitleScreen;

/** Screen effects, one at a time, over a village scene. */
public final class XeysScreenGameTest implements FabricClientGameTest {
	private static final String[] POST = {"vignette", "film_grain", "chromatic_aberration", "sharpen", "color_filter", "monochrome",
		"ambient_glow", "scanlines", "pixelate"};

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!TestFilter.enabled("screen")) {
			return;
		}
		context.getInput().resizeWindow(1280, 720);
		context.runOnClient(c -> {
			c.options.guiScale().set(2);
			c.resizeDisplay();
		});
		context.waitTicks(2);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			world.getClientWorld().waitForChunksRender();
			server.runCommand("gamemode survival @a");
			server.runCommand("difficulty easy");
			server.runCommand("time set 5000");
			server.runCommand("weather clear");
			server.runCommand("execute as @p at @s run place structure minecraft:village_plains ~ ~ ~28");
			server.runCommand("execute as @p at @s run place feature minecraft:oak ~-6 ~ ~10");
			server.runCommand("execute as @p at @s run place feature minecraft:birch ~7 ~ ~13");
			server.runCommand("execute as @p at @s run tp @s ~ ~2 ~ 0 8");
			context.waitTicks(40);
			world.getClientWorld().waitForChunksRender();
			shot(context, "fx_none");
			for (String id : POST) {
				enable(context, id, true);
				context.waitTicks(4);
				shot(context, "fx_" + id);
				enable(context, id, false);
			}

			// Damage flash and chromatic hit pulse together, then low health.
			enable(context, "damage_flash", true);
			enable(context, "chromatic_aberration", true);
			server.runCommand("damage @p 3");
			context.waitTicks(2);
			shot(context, "fx_damage_flash");
			enable(context, "damage_flash", false);
			enable(context, "chromatic_aberration", false);
			enable(context, "low_health", true);
			server.runCommand("effect give @p minecraft:instant_health 1 5");
			context.waitTicks(4);
			server.runCommand("damage @p 15");
			context.waitTicks(40);
			shot(context, "fx_low_health");
			enable(context, "low_health", false);
			server.runCommand("effect give @p minecraft:instant_health 1 5");

			// Letterbox with the HUD hidden.
			enable(context, "letterbox", true);
			context.runOnClient(c -> c.options.hideGui = true);
			context.waitTicks(30);
			shot(context, "fx_letterbox");
			context.runOnClient(c -> c.options.hideGui = false);
			enable(context, "letterbox", false);

			// Under water.
			enable(context, "underwater_ripple", true);
			server.runCommand("execute as @p at @s run fill ~-3 ~-1 ~-3 ~3 ~4 ~3 minecraft:water");
			server.runCommand("effect give @p minecraft:night_vision 60 0 true");
			context.waitTicks(40);
			shot(context, "fx_underwater");
			enable(context, "underwater_ripple", false);
		}
		context.setScreen(TitleScreen::new);
		context.waitTicks(2);
	}

	private static void enable(ClientGameTestContext context, String id, boolean on) {
		context.runOnClient(c -> ModuleManager.get(id).setEnabled(on));
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(c -> c.getToastManager().clear());
		context.waitTicks(1);
		context.takeScreenshot(name);
	}
}
