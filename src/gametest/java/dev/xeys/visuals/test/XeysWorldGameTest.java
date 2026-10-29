package dev.xeys.visuals.test;

import com.google.gson.JsonParser;
import dev.xeys.visuals.core.module.ModuleManager;
import dev.xeys.visuals.core.setting.Setting;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * World modules: block outline and overlay, break progress, glint, dropped items and loot beams, hits,
 * depth of field, time, weather, sky and night-sky layers, fog, clouds, grading, bloom, motion blur and
 * ambient particles.
 */
public final class XeysWorldGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (!TestFilter.enabled("world")) {
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
			BlockPos base = context.computeOnClient(c -> c.player.blockPosition());
			int bx = base.getX();
			int by = base.getY();
			int bz = base.getZ();
			server.runCommand("gamemode creative @a");
			server.runCommand("difficulty easy");
			server.runCommand("time set 5000");
			server.runCommand("weather clear");
			server.runCommand("execute as @p at @s run place structure minecraft:village_plains ~ ~ ~28");
			server.runCommand("execute as @p at @s run place feature minecraft:oak ~-6 ~ ~10");
			server.runCommand("execute as @p at @s run place feature minecraft:birch ~7 ~ ~13");
			server.runCommand(String.format(Locale.ROOT, "setblock %d %d %d minecraft:polished_andesite", bx + 1, by, bz + 2));
			server.runCommand(String.format(Locale.ROOT, "setblock %d %d %d minecraft:oak_stairs", bx - 1, by, bz + 2));
			context.waitTicks(40);
			world.getClientWorld().waitForChunksRender();

			// Block outline and overlay on a full block, then on stairs.
			look(context, 1, 0, 2);
			context.waitTicks(4);
			shot(context, "wd_outline_none");
			enable(context, "block_outline", true);
			context.waitTicks(4);
			shot(context, "wd_outline");
			set(context, "block_outline", "style", "\"corners\"");
			context.waitTicks(2);
			shot(context, "wd_outline_corners");
			set(context, "block_outline", "style", "\"full\"");
			enable(context, "block_overlay", true);
			context.waitTicks(4);
			shot(context, "wd_overlay");
			look(context, -1, 0, 2);
			context.waitTicks(1);
			shot(context, "wd_outline_glide");
			context.waitTicks(10);
			shot(context, "wd_outline_stairs");
			set(context, "block_overlay", "face_only", "true");
			context.waitTicks(2);
			shot(context, "wd_overlay_face");
			enable(context, "block_overlay", false);
			enable(context, "block_outline", false);

			// Break progress while mining the andesite with a wooden pickaxe.
			enable(context, "break_progress", true);
			server.runCommand("gamemode survival @a");
			server.runCommand("item replace entity @p weapon.mainhand with minecraft:wooden_pickaxe");
			look(context, 1, 0, 2);
			context.waitTicks(4);
			context.getInput().holdMouse(0);
			context.waitTicks(9);
			shot(context, "wd_break_progress");
			context.getInput().releaseMouse(0);
			set(context, "break_progress", "style", "\"fill\"");
			server.runCommand(String.format(Locale.ROOT, "setblock %d %d %d minecraft:polished_andesite", bx + 1, by, bz + 2));
			context.waitTicks(10);
			context.getInput().holdMouse(0);
			context.waitTicks(12);
			shot(context, "wd_break_fill");
			context.getInput().releaseMouse(0);
			enable(context, "break_progress", false);
			server.runCommand(String.format(Locale.ROOT, "setblock %d %d %d minecraft:polished_andesite", bx + 1, by, bz + 2));
			server.runCommand("gamemode creative @a");

			// Enchantment glint, vanilla then recolored, on the held sword.
			server.runCommand("item replace entity @p weapon.mainhand with minecraft:diamond_sword[enchantments={sharpness:5}]");
			context.runOnClient(c -> {
				c.player.setXRot(20f);
				c.player.xRotO = 20f;
			});
			context.waitTicks(6);
			shot(context, "wd_glint_vanilla");
			enable(context, "enchant_glint", true);
			context.waitTicks(4);
			shot(context, "wd_glint");
			set(context, "enchant_glint", "color", "{\"color\":\"#FF5A5A\",\"mode\":\"static\"}");
			set(context, "enchant_glint", "speed", "300");
			context.waitTicks(4);
			shot(context, "wd_glint_red");
			enable(context, "enchant_glint", false);
			context.waitTicks(2);
			shot(context, "wd_glint_restored");
			server.runCommand("item replace entity @p weapon.mainhand with minecraft:air");

			// Dropped items behind the player: vanilla, flat and bigger, then with loot beams.
			String item = "summon minecraft:item %.1f %d %.1f {Item:{id:\"minecraft:%s\",count:1},PickupDelay:32767s,Age:-32768s}";
			server.runCommand(String.format(Locale.ROOT, item, bx - 0.4, by, bz - 1.7, "diamond_sword"));
			server.runCommand(String.format(Locale.ROOT, item, bx + 0.8, by, bz - 2.2, "golden_apple"));
			server.runCommand(String.format(Locale.ROOT, item, bx + 1.7, by, bz - 1.5, "grass_block"));
			server.runCommand(String.format(Locale.ROOT, item, bx + 0.5, by, bz - 1.2, "ender_pearl"));
			server.runCommand(String.format(Locale.ROOT, item, bx - 1.2, by, bz - 2.6, "enchanted_golden_apple"));
			server.runCommand(String.format(Locale.ROOT, item, bx + 2.4, by, bz - 3.0, "totem_of_undying"));
			context.runOnClient(c -> {
				c.player.setYRot(180f);
				c.player.setXRot(40f);
				c.player.yRotO = 180f;
				c.player.xRotO = 40f;
			});
			context.waitTicks(30);
			shot(context, "wd_items");
			enable(context, "item_physics", true);
			context.waitTicks(3);
			shot(context, "wd_items_physics");
			enable(context, "dropped_item_style", true);
			set(context, "dropped_item_style", "scale", "160");
			context.waitTicks(3);
			shot(context, "wd_items_styled");
			enable(context, "item_physics", false);
			enable(context, "dropped_item_style", false);
			enable(context, "loot_beams", true);
			set(context, "loot_beams", "min_rarity", "\"all\"");
			context.runOnClient(c -> {
				c.player.setXRot(12f);
				c.player.xRotO = 12f;
			});
			context.waitTicks(4);
			shot(context, "wd_loot_beams");
			enable(context, "loot_beams", false);
			server.runCommand("kill @e[type=minecraft:item]");

			// Hits on a husk: hurt tint and particles.
			enable(context, "hit_color", true);
			enable(context, "hit_particles", true);
			server.runCommand("gamemode survival @a");
			server.runCommand(String.format(Locale.ROOT,
				"summon minecraft:husk %.1f %d %.1f {NoAI:1b,Silent:1b,PersistenceRequired:1b,IsBaby:0b}", bx + 0.5, by, bz + 2.9));
			context.runOnClient(c -> {
				c.player.setYRot(0f);
				c.player.setXRot(10f);
				c.player.yRotO = 0f;
				c.player.xRotO = 10f;
			});
			context.waitTicks(10);
			context.getInput().pressMouse(0);
			context.waitTicks(2);
			shot(context, "wd_hit");
			set(context, "hit_particles", "style", "\"magic\"");
			context.waitTicks(14);
			context.getInput().pressMouse(0);
			context.waitTicks(2);
			shot(context, "wd_hit_magic");
			enable(context, "hit_color", false);
			enable(context, "hit_particles", false);
			server.runCommand("tp @e[type=minecraft:husk] ~ -200 ~");
			server.runCommand("gamemode creative @a");

			// Depth of field: focus on the near block, the village behind softens.
			look(context, 1, 0, 2);
			context.waitTicks(6);
			shot(context, "wd_dof_off");
			enable(context, "depth_of_field", true);
			context.waitTicks(30);
			shot(context, "wd_dof_near");
			context.runOnClient(c -> {
				c.player.setYRot(0f);
				c.player.setXRot(-2f);
				c.player.yRotO = 0f;
				c.player.xRotO = -2f;
			});
			context.waitTicks(30);
			shot(context, "wd_dof_far");
			enable(context, "depth_of_field", false);

			// Fly up and look out over the village for the sky and atmosphere.
			tp(server, bx, by + 36, bz - 14, 0f, 14f);
			context.waitTicks(3);
			context.runOnClient(c -> {
				c.player.getAbilities().flying = true;
				c.player.onUpdateAbilities();
			});
			face(context, 0f, 14f);
			context.waitTicks(30);
			world.getClientWorld().waitForChunksRender();
			shot(context, "wd_day");
			enable(context, "bloom", true);
			context.waitTicks(3);
			shot(context, "wd_bloom_day");
			enable(context, "bloom", false);
			enable(context, "time_changer", true);
			set(context, "time_changer", "hour", "18.6");
			context.waitTicks(4);
			shot(context, "wd_sunset");
			enable(context, "sky_tint", true);
			context.waitTicks(4);
			shot(context, "wd_sunset_tint");
			set(context, "time_changer", "hour", "23");
			context.waitTicks(4);
			shot(context, "wd_night_tint");
			enable(context, "sky_tint", false);
			context.waitTicks(2);
			shot(context, "wd_night");

			// Night sky layers, looking north and up.
			face(context, 180f, -24f);
			context.waitTicks(4);
			shot(context, "wd_sky_vanilla");
			enable(context, "starfield", true);
			context.waitTicks(4);
			shot(context, "wd_starfield");
			enable(context, "aurora", true);
			context.waitTicks(10);
			shot(context, "wd_aurora");
			enable(context, "shooting_stars", true);
			set(context, "shooting_stars", "rate", "60");
			for (int i = 0; i < 6; i++) {
				context.waitTicks(12);
				shot(context, "wd_meteors_" + i);
			}
			enable(context, "shooting_stars", false);
			enable(context, "aurora", false);
			enable(context, "starfield", false);

			// The moon, bigger and full, rising in the east.
			set(context, "time_changer", "hour", "19.6");
			face(context, -90f, -12f);
			context.waitTicks(4);
			shot(context, "wd_moon_vanilla");
			enable(context, "celestial", true);
			set(context, "celestial", "moon_size", "260");
			set(context, "celestial", "phase", "\"full\"");
			context.waitTicks(4);
			shot(context, "wd_moon_big");
			// And the sun, bigger, low in the east in the morning.
			set(context, "time_changer", "hour", "6.9");
			set(context, "celestial", "sun_size", "220");
			context.waitTicks(4);
			shot(context, "wd_sun_big");
			enable(context, "celestial", false);
			enable(context, "time_changer", false);
			face(context, 0f, 14f);
			context.waitTicks(2);

			// Weather: eases in over a couple of seconds.
			enable(context, "weather_changer", true);
			set(context, "weather_changer", "weather", "\"rain\"");
			context.waitTicks(80);
			shot(context, "wd_rain");
			set(context, "weather_changer", "weather", "\"thunderstorm\"");
			context.waitTicks(80);
			shot(context, "wd_thunder");
			set(context, "weather_changer", "weather", "\"snow\"");
			context.waitTicks(60);
			shot(context, "wd_snow");
			set(context, "weather_changer", "weather", "\"clear\"");
			context.waitTicks(100);
			enable(context, "weather_changer", false);

			enable(context, "atmospheric_fog", true);
			context.waitTicks(4);
			shot(context, "wd_fog");
			enable(context, "atmospheric_fog", false);

			// Clouds from below (the test harness turns them off for stable screenshots).
			context.runOnClient(c -> c.options.cloudStatus().set(net.minecraft.client.CloudStatus.FANCY));
			face(context, 0f, -32f);
			context.waitTicks(4);
			shot(context, "wd_clouds_none");
			enable(context, "cloud_style", true);
			set(context, "cloud_style", "color", "{\"color\":\"#FF9AA8\",\"mode\":\"static\"}");
			set(context, "cloud_style", "opacity", "70");
			set(context, "cloud_style", "height", "-48");
			context.waitTicks(4);
			shot(context, "wd_clouds");
			enable(context, "cloud_style", false);
			face(context, 0f, 14f);

			for (String look : new String[] {"cinematic", "vibrant", "moody", "warm", "cool", "pastel"}) {
				enable(context, "color_grading", true);
				set(context, "color_grading", "look", "\"" + look + "\"");
				context.waitTicks(3);
				shot(context, "wd_grade_" + look);
			}
			enable(context, "color_grading", false);

			// Motion blur while turning.
			enable(context, "motion_blur", true);
			set(context, "motion_blur", "strength", "100");
			for (int i = 0; i < 8; i++) {
				context.runOnClient(c -> c.player.setYRot(c.player.getYRot() + 7f));
				context.waitTicks(1);
			}
			context.runOnClient(c -> c.player.setYRot(c.player.getYRot() + 7f));
			context.takeScreenshot("wd_motion_blur");
			context.waitTicks(4);
			shot(context, "wd_motion_still");
			enable(context, "motion_blur", false);

			// Back on the ground at night: fireflies, then bloom on lanterns.
			tp(server, bx, by, bz, 0f, 6f);
			context.runOnClient(c -> c.player.getAbilities().flying = false);
			server.runCommand(String.format(Locale.ROOT, "setblock %d %d %d minecraft:lantern", bx - 2, by, bz + 4));
			server.runCommand(String.format(Locale.ROOT, "setblock %d %d %d minecraft:sea_lantern", bx + 2, by, bz + 5));
			enable(context, "time_changer", true);
			set(context, "time_changer", "hour", "22");
			face(context, 0f, 6f);
			context.waitTicks(20);
			shot(context, "wd_ground_night");
			enable(context, "fireflies", true);
			set(context, "fireflies", "density", "120");
			context.waitTicks(80);
			shot(context, "wd_fireflies");
			enable(context, "fireflies", false);
			enable(context, "bloom", true);
			context.waitTicks(3);
			shot(context, "wd_bloom_night");
			enable(context, "bloom", false);
			enable(context, "time_changer", false);

			// Daytime dust motes and falling leaves under the trees.
			enable(context, "dust_motes", true);
			set(context, "dust_motes", "size", "300");
			set(context, "dust_motes", "density", "300");
			enable(context, "falling_leaves", true);
			set(context, "falling_leaves", "amount", "2000");
			face(context, 30f, 4f);
			context.waitTicks(60);
			shot(context, "wd_motes_leaves");
			enable(context, "dust_motes", false);
			enable(context, "falling_leaves", false);
		}
		context.setScreen(TitleScreen::new);
		context.waitTicks(2);
	}

	private static void tp(TestServerContext server, int x, int y, int z, float yaw, float pitch) {
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %.1f %.1f", x + 0.5, y, z + 0.5, yaw, pitch));
	}

	private static void face(ClientGameTestContext context, float yaw, float pitch) {
		context.runOnClient(c -> {
			c.player.setYRot(yaw);
			c.player.setXRot(pitch);
			c.player.yRotO = yaw;
			c.player.xRotO = pitch;
		});
	}

	/** Points the camera at the center of the block at an offset from the player's feet. */
	private static void look(ClientGameTestContext context, int dx, int dy, int dz) {
		context.runOnClient(c -> {
			BlockPos feet = c.player.blockPosition();
			c.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(feet.offset(dx, dy, dz)));
		});
	}

	private static void set(ClientGameTestContext context, String id, String key, String json) {
		context.runOnClient(c -> {
			for (Setting<?> s : ModuleManager.get(id).settings()) {
				if (s.key().equals(key)) {
					s.fromJson(JsonParser.parseString(json));
					return;
				}
			}
			throw new IllegalArgumentException("No setting " + id + "." + key);
		});
	}

	private static void enable(ClientGameTestContext context, String id, boolean on) {
		context.runOnClient(c -> ModuleManager.get(id).setEnabled(on));
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(c -> {
			c.getToastManager().clear();
			c.gui.getChat().clearMessages(false);
		});
		context.waitTicks(1);
		context.takeScreenshot(name);
	}
}
