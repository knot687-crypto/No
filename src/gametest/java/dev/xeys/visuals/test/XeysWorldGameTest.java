package dev.xeys.visuals.test;

import com.google.gson.JsonParser;
import dev.xeys.visuals.core.module.ModuleManager;
import dev.xeys.visuals.core.setting.Setting;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * World modules, in sections (select some with -Dxeys.sections=...): blocks, breaking, glint, items,
 * hits, dof, sky, nightsky, celestial, weather, atmosphere, grading, motion, ground, motes.
 */
public final class XeysWorldGameTest implements FabricClientGameTest {
	private ClientGameTestContext context;
	private TestServerContext server;
	private int bx;
	private int by;
	private int bz;

	@Override
	public void runTest(ClientGameTestContext ctx) {
		if (!TestFilter.enabled("world")) {
			return;
		}
		this.context = ctx;
		ctx.getInput().resizeWindow(1280, 720);
		ctx.runOnClient(c -> {
			c.options.guiScale().set(2);
			c.resizeDisplay();
		});
		ctx.waitTicks(2);
		try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
			this.server = world.getServer();
			world.getClientWorld().waitForChunksRender();
			BlockPos base = ctx.computeOnClient(c -> c.player.blockPosition());
			this.bx = base.getX();
			this.by = base.getY();
			this.bz = base.getZ();
			this.command("gamemode creative @a");
			this.command("difficulty easy");
			this.command("time set 5000");
			this.command("weather clear");
			this.command("execute as @p at @s run place structure minecraft:village_plains ~ ~ ~28");
			this.command("execute as @p at @s run place feature minecraft:oak ~-6 ~ ~10");
			this.command("execute as @p at @s run place feature minecraft:birch ~7 ~ ~13");
			this.command("setblock %d %d %d minecraft:polished_andesite", this.bx + 1, this.by, this.bz + 2);
			this.command("setblock %d %d %d minecraft:oak_stairs", this.bx - 1, this.by, this.bz + 2);
			ctx.waitTicks(40);
			world.getClientWorld().waitForChunksRender();

			if (TestFilter.section("blocks")) {
				this.blocks();
			}
			if (TestFilter.section("breaking")) {
				this.breaking();
			}
			if (TestFilter.section("glint")) {
				this.glint();
			}
			if (TestFilter.section("items")) {
				this.items();
			}
			if (TestFilter.section("hits")) {
				this.hits();
			}
			if (TestFilter.section("dof")) {
				this.depthOfField();
			}
			if (TestFilter.section("sky")) {
				this.sky();
			}
			if (TestFilter.section("nightsky")) {
				this.nightSky();
			}
			if (TestFilter.section("celestial")) {
				this.celestial();
			}
			if (TestFilter.section("weather")) {
				this.weather();
			}
			if (TestFilter.section("atmosphere")) {
				this.atmosphere();
			}
			if (TestFilter.section("grading")) {
				this.grading();
			}
			if (TestFilter.section("motion")) {
				this.motion();
			}
			if (TestFilter.section("ground")) {
				this.groundNight();
			}
			if (TestFilter.section("motes")) {
				this.motes();
			}
		}
		ctx.setScreen(TitleScreen::new);
		ctx.waitTicks(2);
	}

	/** Block outline and overlay on a full block, then on stairs. */
	private void blocks() {
		this.onGround(0f, 0f);
		this.look(1, 0, 2);
		this.context.waitTicks(4);
		this.shot("wd_outline_none");
		this.enable("block_outline", true);
		this.context.waitTicks(4);
		this.shot("wd_outline");
		this.set("block_outline", "style", "\"corners\"");
		this.context.waitTicks(2);
		this.shot("wd_outline_corners");
		this.set("block_outline", "style", "\"full\"");
		this.enable("block_overlay", true);
		this.context.waitTicks(4);
		this.shot("wd_overlay");
		this.look(-1, 0, 2);
		this.context.waitTicks(1);
		this.shot("wd_outline_glide");
		this.context.waitTicks(10);
		this.shot("wd_outline_stairs");
		this.set("block_overlay", "face_only", "true");
		this.context.waitTicks(2);
		this.shot("wd_overlay_face");
		this.enable("block_overlay", false);
		this.enable("block_outline", false);
	}

	/** Break progress while mining the andesite with a wooden pickaxe, growing then filling. */
	private void breaking() {
		this.onGround(0f, 0f);
		this.enable("break_progress", true);
		this.command("gamemode survival @a");
		this.command("item replace entity @p weapon.mainhand with minecraft:wooden_pickaxe");
		this.look(1, 0, 2);
		this.context.waitTicks(4);
		this.context.getInput().holdMouse(0);
		this.context.waitTicks(9);
		this.shot("wd_break_progress");
		this.context.getInput().releaseMouse(0);
		this.set("break_progress", "style", "\"fill\"");
		this.command("setblock %d %d %d minecraft:polished_andesite", this.bx + 1, this.by, this.bz + 2);
		this.context.waitTicks(10);
		this.context.getInput().holdMouse(0);
		this.context.waitTicks(12);
		this.shot("wd_break_fill");
		this.context.getInput().releaseMouse(0);
		this.enable("break_progress", false);
		this.command("setblock %d %d %d minecraft:polished_andesite", this.bx + 1, this.by, this.bz + 2);
		this.command("item replace entity @p weapon.mainhand with minecraft:air");
		this.command("gamemode creative @a");
	}

	/** Enchantment glint on the held sword: vanilla, recolored, red and fast, then restored. */
	private void glint() {
		this.onGround(0f, 20f);
		this.command("item replace entity @p weapon.mainhand with minecraft:diamond_sword[enchantments={sharpness:5}]");
		this.context.waitTicks(10);
		this.shot("wd_glint_vanilla");
		this.enable("enchant_glint", true);
		this.context.waitTicks(4);
		this.shot("wd_glint");
		this.set("enchant_glint", "color", "{\"color\":\"#FF5A5A\",\"mode\":\"static\"}");
		this.set("enchant_glint", "speed", "300");
		this.context.waitTicks(4);
		this.shot("wd_glint_red");
		this.enable("enchant_glint", false);
		this.context.waitTicks(2);
		this.shot("wd_glint_restored");
		this.command("item replace entity @p weapon.mainhand with minecraft:air");
	}

	/** Dropped items behind the player: vanilla, flat and bigger, then with loot beams. */
	private void items() {
		this.onGround(180f, 40f);
		String item = "summon minecraft:item %.1f %d %.1f {Item:{id:\"minecraft:%s\",count:1},PickupDelay:32767s,Age:-32768s}";
		this.command(item, this.bx - 0.4, this.by, this.bz - 1.7, "diamond_sword");
		this.command(item, this.bx + 0.8, this.by, this.bz - 2.2, "golden_apple");
		this.command(item, this.bx + 1.7, this.by, this.bz - 1.5, "grass_block");
		this.command(item, this.bx + 0.5, this.by, this.bz - 1.2, "ender_pearl");
		this.command(item, this.bx - 1.2, this.by, this.bz - 2.6, "enchanted_golden_apple");
		this.command(item, this.bx + 2.4, this.by, this.bz - 3.0, "totem_of_undying");
		this.context.waitTicks(30);
		this.shot("wd_items");
		this.enable("item_physics", true);
		this.context.waitTicks(3);
		this.shot("wd_items_physics");
		this.enable("dropped_item_style", true);
		this.set("dropped_item_style", "scale", "160");
		this.context.waitTicks(3);
		this.shot("wd_items_styled");
		this.enable("item_physics", false);
		this.enable("dropped_item_style", false);
		this.enable("loot_beams", true);
		this.set("loot_beams", "min_rarity", "\"all\"");
		this.face(180f, 12f);
		this.context.waitTicks(4);
		this.shot("wd_loot_beams");
		this.enable("loot_beams", false);
		this.command("kill @e[type=minecraft:item]");
	}

	/** Hits on a husk: hurt tint and particles. */
	private void hits() {
		this.onGround(0f, 10f);
		this.enable("hit_color", true);
		this.enable("hit_particles", true);
		this.command("gamemode survival @a");
		this.command("summon minecraft:husk %.1f %d %.1f {NoAI:1b,Silent:1b,PersistenceRequired:1b,IsBaby:0b}", this.bx + 0.5, this.by,
			this.bz + 2.9);
		this.face(0f, 10f);
		this.context.waitTicks(10);
		this.context.getInput().pressMouse(0);
		this.context.waitTicks(2);
		this.shot("wd_hit");
		this.set("hit_particles", "style", "\"magic\"");
		this.context.waitTicks(14);
		this.context.getInput().pressMouse(0);
		this.context.waitTicks(2);
		this.shot("wd_hit_magic");
		this.enable("hit_color", false);
		this.enable("hit_particles", false);
		this.command("tp @e[type=minecraft:husk] ~ -200 ~");
		this.command("gamemode creative @a");
	}

	/** Depth of field: focus on the near block, then on the distance. */
	private void depthOfField() {
		this.onGround(0f, 0f);
		this.look(1, 0, 2);
		this.context.waitTicks(6);
		this.shot("wd_dof_off");
		this.enable("depth_of_field", true);
		this.context.waitTicks(30);
		this.shot("wd_dof_near");
		this.face(0f, -2f);
		this.context.waitTicks(30);
		this.shot("wd_dof_far");
		this.enable("depth_of_field", false);
	}

	/** Over the village: day, bloom, sunset with and without sky tint, night. */
	private void sky() {
		this.aloft(0f, 14f);
		this.shot("wd_day");
		this.enable("bloom", true);
		this.context.waitTicks(3);
		this.shot("wd_bloom_day");
		this.enable("bloom", false);
		this.enable("time_changer", true);
		this.set("time_changer", "hour", "18.6");
		this.context.waitTicks(4);
		this.shot("wd_sunset");
		this.enable("sky_tint", true);
		this.context.waitTicks(4);
		this.shot("wd_sunset_tint");
		this.set("time_changer", "hour", "23");
		this.context.waitTicks(4);
		this.shot("wd_night_tint");
		this.enable("sky_tint", false);
		this.context.waitTicks(2);
		this.shot("wd_night");
		this.enable("time_changer", false);
	}

	/** Night sky layers, looking north and up: starfield, aurora, meteors. */
	private void nightSky() {
		this.aloft(180f, -24f);
		this.enable("time_changer", true);
		this.set("time_changer", "hour", "23");
		this.context.waitTicks(4);
		this.shot("wd_sky_vanilla");
		this.enable("starfield", true);
		this.context.waitTicks(4);
		this.shot("wd_starfield");
		this.enable("aurora", true);
		this.context.waitTicks(10);
		this.shot("wd_aurora");
		this.enable("aurora", false);
		this.enable("shooting_stars", true);
		this.set("shooting_stars", "rate", "120");
		for (int i = 0; i < 6; i++) {
			this.context.waitTicks(10);
			this.shot("wd_meteors_" + i);
		}
		this.enable("shooting_stars", false);
		this.enable("starfield", false);
		this.enable("time_changer", false);
	}

	/** The moon bigger and full, rising in the east; the sun bigger in the morning. */
	private void celestial() {
		this.aloft(-90f, -12f);
		this.enable("time_changer", true);
		this.set("time_changer", "hour", "19.6");
		this.context.waitTicks(4);
		this.shot("wd_moon_vanilla");
		this.enable("celestial", true);
		this.set("celestial", "moon_size", "260");
		this.set("celestial", "phase", "\"full\"");
		this.context.waitTicks(4);
		this.shot("wd_moon_big");
		this.set("time_changer", "hour", "6.9");
		this.set("celestial", "sun_size", "220");
		this.context.waitTicks(4);
		this.shot("wd_sun_big");
		this.enable("celestial", false);
		this.enable("time_changer", false);
	}

	/** Weather: rain, a thunderstorm and snow, each easing in. */
	private void weather() {
		this.aloft(0f, 14f);
		this.enable("weather_changer", true);
		this.set("weather_changer", "weather", "\"rain\"");
		this.context.waitTicks(80);
		this.shot("wd_rain");
		this.set("weather_changer", "weather", "\"thunderstorm\"");
		this.context.waitTicks(80);
		this.shot("wd_thunder");
		this.set("weather_changer", "weather", "\"snow\"");
		this.context.waitTicks(60);
		this.shot("wd_snow");
		this.set("weather_changer", "weather", "\"clear\"");
		this.context.waitTicks(100);
		this.enable("weather_changer", false);
	}

	/** Atmospheric fog, then clouds from below (the harness turns clouds off for stable screenshots). */
	private void atmosphere() {
		this.aloft(0f, 14f);
		this.enable("atmospheric_fog", true);
		this.context.waitTicks(4);
		this.shot("wd_fog");
		this.enable("atmospheric_fog", false);
		this.context.runOnClient(c -> c.options.cloudStatus().set(CloudStatus.FANCY));
		this.face(0f, -32f);
		this.context.waitTicks(4);
		this.shot("wd_clouds_none");
		this.enable("cloud_style", true);
		this.set("cloud_style", "color", "{\"color\":\"#FF9AA8\",\"mode\":\"static\"}");
		this.set("cloud_style", "opacity", "70");
		this.set("cloud_style", "height", "-48");
		this.context.waitTicks(4);
		this.shot("wd_clouds");
		this.enable("cloud_style", false);
		this.context.runOnClient(c -> c.options.cloudStatus().set(CloudStatus.OFF));
	}

	private void grading() {
		this.aloft(0f, 14f);
		for (String look : new String[] {"cinematic", "vibrant", "moody", "warm", "cool", "pastel"}) {
			this.enable("color_grading", true);
			this.set("color_grading", "look", "\"" + look + "\"");
			this.context.waitTicks(3);
			this.shot("wd_grade_" + look);
		}
		this.enable("color_grading", false);
	}

	/** Motion blur while turning, then holding still. */
	private void motion() {
		this.aloft(0f, 14f);
		this.enable("motion_blur", true);
		this.set("motion_blur", "strength", "100");
		for (int i = 0; i < 8; i++) {
			this.context.runOnClient(c -> c.player.setYRot(c.player.getYRot() + 7f));
			this.context.waitTicks(1);
		}
		this.context.runOnClient(c -> c.player.setYRot(c.player.getYRot() + 7f));
		this.context.takeScreenshot("wd_motion_blur");
		this.context.waitTicks(4);
		this.shot("wd_motion_still");
		this.enable("motion_blur", false);
	}

	/** On the ground at night: fireflies, then bloom on a sea lantern and glowstone right in front. */
	private void groundNight() {
		this.onGround(0f, 6f);
		this.command("setblock %d %d %d minecraft:sea_lantern", this.bx + 1, this.by, this.bz + 2);
		this.command("setblock %d %d %d minecraft:glowstone", this.bx - 1, this.by, this.bz + 3);
		this.command("setblock %d %d %d minecraft:lantern", this.bx, this.by, this.bz + 4);
		this.enable("time_changer", true);
		this.set("time_changer", "hour", "22");
		this.context.waitTicks(20);
		this.shot("wd_ground_night");
		this.enable("fireflies", true);
		this.set("fireflies", "density", "120");
		this.context.waitTicks(80);
		this.shot("wd_fireflies");
		this.enable("fireflies", false);
		this.face(0f, 22f);
		this.context.waitTicks(4);
		this.shot("wd_bloom_night_off");
		this.enable("bloom", true);
		this.context.waitTicks(3);
		this.shot("wd_bloom_night");
		this.enable("bloom", false);
		this.enable("time_changer", false);
		this.command("setblock %d %d %d minecraft:polished_andesite", this.bx + 1, this.by, this.bz + 2);
		this.command("setblock %d %d %d minecraft:air", this.bx - 1, this.by, this.bz + 3);
		this.command("setblock %d %d %d minecraft:air", this.bx, this.by, this.bz + 4);
	}

	/** Daytime dust motes and falling leaves under the trees. */
	private void motes() {
		this.onGround(30f, 4f);
		this.enable("dust_motes", true);
		this.set("dust_motes", "size", "200");
		this.set("dust_motes", "density", "300");
		this.enable("falling_leaves", true);
		this.set("falling_leaves", "amount", "2000");
		this.context.waitTicks(60);
		this.shot("wd_motes_leaves");
		this.enable("dust_motes", false);
		this.enable("falling_leaves", false);
	}

	/** Stands on the ground at the base position, looking as given. */
	private void onGround(float yaw, float pitch) {
		this.context.runOnClient(c -> {
			c.player.getAbilities().flying = false;
			c.player.onUpdateAbilities();
		});
		this.command("tp @p %.1f %d %.1f %.1f %.1f", this.bx + 0.5, this.by, this.bz + 0.5, yaw, pitch);
		this.context.waitTicks(3);
		this.face(yaw, pitch);
		this.context.waitTicks(2);
	}

	/** Hovers over the village, looking as given. */
	private void aloft(float yaw, float pitch) {
		this.command("tp @p %.1f %d %.1f %.1f %.1f", this.bx + 0.5, this.by + 36, this.bz - 13.5, yaw, pitch);
		this.context.waitTicks(3);
		this.context.runOnClient(c -> {
			c.player.getAbilities().flying = true;
			c.player.onUpdateAbilities();
		});
		this.face(yaw, pitch);
		this.context.waitTicks(20);
	}

	private void command(String format, Object... args) {
		this.server.runCommand(args.length == 0 ? format : String.format(Locale.ROOT, format, args));
	}

	private void face(float yaw, float pitch) {
		this.context.runOnClient(c -> {
			c.player.setYRot(yaw);
			c.player.setXRot(pitch);
			c.player.yRotO = yaw;
			c.player.xRotO = pitch;
		});
	}

	/** Points the camera at the center of the block at an offset from the player's feet. */
	private void look(int dx, int dy, int dz) {
		this.context.runOnClient(c -> {
			BlockPos feet = c.player.blockPosition();
			c.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(feet.offset(dx, dy, dz)));
		});
	}

	private void set(String id, String key, String json) {
		this.context.runOnClient(c -> {
			for (Setting<?> s : ModuleManager.get(id).settings()) {
				if (s.key().equals(key)) {
					s.fromJson(JsonParser.parseString(json));
					return;
				}
			}
			throw new IllegalArgumentException("No setting " + id + "." + key);
		});
	}

	private void enable(String id, boolean on) {
		this.context.runOnClient(c -> ModuleManager.get(id).setEnabled(on));
	}

	private void shot(String name) {
		this.context.runOnClient(c -> {
			c.getToastManager().clear();
			c.gui.getChat().clearMessages(false);
		});
		this.context.waitTicks(1);
		this.context.takeScreenshot(name);
	}
}
