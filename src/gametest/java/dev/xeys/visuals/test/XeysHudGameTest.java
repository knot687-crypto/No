package dev.xeys.visuals.test;

import dev.xeys.visuals.core.module.ModuleManager;
import dev.xeys.visuals.hud.HudEditorScreen;
import dev.xeys.visuals.ui.clickgui.ClickGuiScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.TitleScreen;

/** In-world HUD: default elements, every batch-1 element, the target HUD and the HUD editor. */
public final class XeysHudGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!TestFilter.enabled("hud")) {
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
			server.runCommand("time set 1000");
			server.runCommand("weather clear");
			server.runCommand("item replace entity @a armor.head with minecraft:diamond_helmet");
			server.runCommand("item replace entity @a armor.chest with minecraft:diamond_chestplate[damage=300]");
			server.runCommand("item replace entity @a armor.legs with minecraft:iron_leggings");
			server.runCommand("item replace entity @a armor.feet with minecraft:netherite_boots[damage=460]");
			server.runCommand("item replace entity @a weapon.mainhand with minecraft:diamond_sword[damage=200]");
			server.runCommand("item replace entity @a weapon.offhand with minecraft:torch 32");
			server.runCommand("give @a minecraft:oak_log 48");
			server.runCommand("give @a minecraft:cobblestone 64");
			server.runCommand("give @a minecraft:golden_apple 3");
			server.runCommand("give @a minecraft:ender_pearl 6");
			server.runCommand("give @a minecraft:bow[damage=120]");
			server.runCommand("give @a minecraft:arrow 64");
			server.runCommand("effect give @a minecraft:speed 300 1");
			server.runCommand("effect give @a minecraft:strength 90 0");
			server.runCommand("effect give @a minecraft:night_vision infinite 0");
			server.runCommand("effect give @a minecraft:regeneration 8 1");
			context.waitTicks(30);
			shot(context, "hud_default");

			enable(context, "cps", "ping", "speed", "direction", "biome", "light_level", "clock", "memory", "session_time", "day_counter",
				"server_info", "weather", "compass", "moon_phase", "heading_bar", "keystrokes", "held_item", "inventory_hud", "pack_display",
				"module_list");
			context.waitTicks(20);
			shot(context, "hud_all");

			// Target HUD: a husk (no daylight burning) just in front, then a hit.
			disable(context, "inventory_hud", "keystrokes");
			enable(context, "target_hud");
			server.runCommand("execute as @p at @s run summon minecraft:husk ~ ~ ~2.4 {NoAI:1b,PersistenceRequired:1b,Silent:1b,IsBaby:0b}");
			server.runCommand("execute as @p at @s run tp @s ~ ~ ~ facing entity @e[type=minecraft:husk,limit=1,sort=nearest] eyes");
			context.waitTicks(15);
			shot(context, "hud_target");
			server.runCommand("damage @e[type=minecraft:husk,limit=1] 6");
			context.waitTicks(3);
			shot(context, "hud_target_hit");
			context.waitTicks(30);
			shot(context, "hud_target_settled");

			context.setScreen(() -> new HudEditorScreen(null));
			context.waitTicks(20);
			shot(context, "hud_editor");
			context.setScreen(() -> new ClickGuiScreen(null));
			context.waitTicks(20);
			shot(context, "gui_hud_category");
			context.setScreen(() -> null);
			context.waitTicks(2);
		}
		// Editor outside a world: every element with sample data over the menu backdrop.
		context.setScreen(() -> new HudEditorScreen(new TitleScreen()));
		context.waitTicks(20);
		context.takeScreenshot("hud_editor_menu");
		context.setScreen(TitleScreen::new);
		context.waitTicks(2);
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(c -> c.getToastManager().clear());
		context.waitTicks(1);
		context.takeScreenshot(name);
	}

	private static void enable(ClientGameTestContext context, String... ids) {
		context.runOnClient(c -> {
			for (String id : ids) {
				ModuleManager.get(id).setEnabled(true);
			}
		});
	}

	private static void disable(ClientGameTestContext context, String... ids) {
		context.runOnClient(c -> {
			for (String id : ids) {
				ModuleManager.get(id).setEnabled(false);
			}
		});
	}
}
