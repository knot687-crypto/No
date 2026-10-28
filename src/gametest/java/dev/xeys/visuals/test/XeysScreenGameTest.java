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
		if (TestFilter.enabled("overlay")) {
			this.overlays(context);
		}
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

	/** Overlay effects (damage direction, hit markers, speed lines, alerts) and screen restyles. */
	private void overlays(ClientGameTestContext context) {
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
			server.runCommand("execute as @p at @s run place structure minecraft:village_plains ~ ~ ~28");
			server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 4");
			context.waitTicks(30);
			world.getClientWorld().waitForChunksRender();

			// Damage from the right and from behind.
			enable(context, "damage_direction", true);
			server.runCommand("execute as @p at @s run summon minecraft:husk ~-4 ~ ~0.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,IsBaby:0b,Tags:[\"right\"]}");
			server.runCommand("execute as @p at @s run summon minecraft:husk ~0.5 ~ ~-4 {NoAI:1b,Silent:1b,PersistenceRequired:1b,IsBaby:0b,Tags:[\"behind\"]}");
			context.waitTicks(5);
			server.runCommand("damage @p 1 minecraft:mob_attack by @e[type=minecraft:husk,tag=right,limit=1]");
			context.waitTicks(12);
			server.runCommand("damage @p 1 minecraft:mob_attack by @e[type=minecraft:husk,tag=behind,limit=1]");
			context.waitTicks(4);
			shot(context, "ov_damage_direction");
			enable(context, "damage_direction", false);
			server.runCommand("kill @e[type=minecraft:husk]");

			// A hit that lands, on a husk in front.
			enable(context, "hit_marker", true);
			server.runCommand("execute as @p at @s run summon minecraft:husk ~ ~ ~2.2 {NoAI:1b,Silent:1b,PersistenceRequired:1b,IsBaby:0b}");
			server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 8");
			context.waitTicks(10);
			context.getInput().pressMouse(0);
			context.waitTicks(3);
			shot(context, "ov_hit_marker");
			enable(context, "hit_marker", false);
			server.runCommand("kill @e[type=minecraft:husk]");

			// Durability alert as boots wear below 10%.
			enable(context, "durability_alert", true);
			server.runCommand("item replace entity @p armor.feet with minecraft:netherite_boots[damage=400]");
			context.waitTicks(5);
			server.runCommand("item replace entity @p armor.feet with minecraft:netherite_boots[damage=440]");
			context.waitTicks(10);
			shot(context, "ov_durability_alert");
			enable(context, "durability_alert", false);

			// Inventory with a frosted background, then a tooltip.
			enable(context, "inventory_background", true);
			enable(context, "tooltip_style", true);
			server.runCommand("item replace entity @p hotbar.0 with minecraft:diamond_sword[enchantments={sharpness:5,unbreaking:3}]");
			server.runCommand("give @p minecraft:golden_apple 7");
			context.waitTicks(5);
			context.getInput().pressKey(org.lwjgl.glfw.GLFW.GLFW_KEY_E);
			context.waitTicks(12);
			shot(context, "ov_inventory");
			context.getInput().setCursorPos(496, 494);
			context.waitTicks(6);
			shot(context, "ov_tooltip");
			context.getInput().pressKey(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE);
			context.waitTicks(4);
			enable(context, "inventory_background", false);
			enable(context, "tooltip_style", false);

			// Screenshot flash and preview card.
			enable(context, "screenshot_flash", true);
			context.getInput().pressKey(org.lwjgl.glfw.GLFW.GLFW_KEY_F2);
			context.takeScreenshot("ov_screenshot_flash");
			context.waitTicks(14);
			shot(context, "ov_screenshot_preview");
			enable(context, "screenshot_flash", false);

			// Speed lines: fly fast in spectator mode.
			enable(context, "speed_lines", true);
			server.runCommand("gamemode spectator @a");
			context.waitTicks(3);
			for (int i = 0; i < 12; i++) {
				context.runOnClient(c -> c.player.setDeltaMovement(0.0, 0.0, 1.6));
				context.waitTicks(1);
			}
			shot(context, "ov_speed_lines");
			enable(context, "speed_lines", false);
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
