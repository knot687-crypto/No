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
			server.runCommand("give @a minecraft:stone 640");
			server.runCommand("give @a minecraft:redstone 37");
			server.runCommand("give @a minecraft:iron_pickaxe[damage=100]");
			server.runCommand("effect give @a minecraft:speed 300 1");
			server.runCommand("effect give @a minecraft:strength 90 0");
			server.runCommand("effect give @a minecraft:night_vision infinite 0");
			server.runCommand("effect give @a minecraft:regeneration 8 1");
			context.waitTicks(30);
			shot(context, "hud_default");

			enable(context, "cps", "ping", "speed", "direction", "biome", "light_level", "clock", "memory", "session_time", "server_info",
				"day_counter", "weather");
			context.waitTicks(20);
			shot(context, "hud_info");

			disable(context, "cps", "ping", "speed", "direction", "biome", "light_level", "clock", "memory", "session_time", "server_info",
				"day_counter", "weather");
			enable(context, "compass", "moon_phase", "heading_bar", "keystrokes", "held_item", "inventory_hud", "module_list", "pack_display");
			context.waitTicks(20);
			shot(context, "hud_widgets");

			// Target HUD: a husk (no daylight burning) just in front, looked at squarely, then a hit.
			disable(context, "inventory_hud", "keystrokes", "held_item");
			enable(context, "target_hud");
			server.runCommand("execute as @p at @s run summon minecraft:husk ~ ~ ~2.6 {NoAI:1b,PersistenceRequired:1b,Silent:1b,IsBaby:0b}");
			server.runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 6");
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

		// Style variants of the batch, previewed in the editor.
		context.runOnClient(c -> {
			set("watermark", "style", "BAR");
			set("compass", "style", "NEEDLE");
			set("armor_hud", "layout", "HORIZONTAL");
			set("potion_hud", "layout", "COMPACT");
			set("fps", "graph", true);
			set("fps", "frame_time", true);
			set("coordinates", "layout", "SINGLE_LINE");
			set("target_hud", "portrait", "FACE");
			set("module_list", "bar", "INNER");
			for (String id : new String[] {"keystrokes", "inventory_hud", "held_item", "heading_bar", "clock", "weather"}) {
				ModuleManager.get(id).setEnabled(true);
			}
		});
		context.waitTicks(20);
		context.takeScreenshot("hud_editor_variants");
		context.runOnClient(c -> {
			for (dev.xeys.visuals.core.module.Module m : ModuleManager.all()) {
				for (dev.xeys.visuals.core.setting.Setting<?> st : m.settings()) {
					st.reset();
				}
			}
		});
		context.setScreen(TitleScreen::new);
		context.waitTicks(2);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void set(String module, String key, Object value) {
		for (dev.xeys.visuals.core.setting.Setting<?> st : ModuleManager.get(module).settings()) {
			if (st.key().equals(key)) {
				if (st instanceof dev.xeys.visuals.core.setting.EnumSetting e) {
					for (Object o : e.options()) {
						if (((Enum<?>) o).name().equals(value)) {
							((dev.xeys.visuals.core.setting.Setting) st).set(o);
						}
					}
				} else {
					((dev.xeys.visuals.core.setting.Setting) st).set(value);
				}
				return;
			}
		}
		throw new IllegalArgumentException("No setting " + module + "." + key);
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
