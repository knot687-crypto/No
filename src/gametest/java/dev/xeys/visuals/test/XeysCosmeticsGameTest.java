package dev.xeys.visuals.test;

import com.google.gson.JsonParser;
import dev.xeys.visuals.core.module.ModuleManager;
import dev.xeys.visuals.core.setting.Setting;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;

/**
 * Cosmetics on the local player, seen in third person (sections: head, back, around, motion, combat,
 * player, inventory).
 */
public final class XeysCosmeticsGameTest implements FabricClientGameTest {
	private ClientGameTestContext context;
	private TestServerContext server;
	private int bx;
	private int by;
	private int bz;

	@Override
	public void runTest(ClientGameTestContext ctx) {
		if (!TestFilter.enabled("cosmetics")) {
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
			this.command("time set 6000");
			this.command("weather clear");
			this.command("execute as @p at @s run place feature minecraft:oak ~-6 ~ ~-9");
			this.command("execute as @p at @s run place feature minecraft:birch ~5 ~ ~-11");
			ctx.waitTicks(30);
			world.getClientWorld().waitForChunksRender();

			if (TestFilter.section("head")) {
				this.head();
			}
			if (TestFilter.section("back")) {
				this.back();
			}
			if (TestFilter.section("around")) {
				this.around();
			}
			if (TestFilter.section("motion")) {
				this.motion();
			}
			if (TestFilter.section("player")) {
				this.player();
			}
			if (TestFilter.section("inventory")) {
				this.inventory();
			}
			// Last: fights leave particles and drops around the player.
			if (TestFilter.section("combat")) {
				this.combat();
			}
			this.camera(CameraType.FIRST_PERSON);
		}
		ctx.setScreen(TitleScreen::new);
		ctx.waitTicks(2);
	}

	/** Head pieces, seen from the front. */
	private void head() {
		this.stand(0f, 10f);
		this.camera(CameraType.THIRD_PERSON_FRONT);
		this.context.waitTicks(4);
		this.shot("cs_front_none");
		for (String id : new String[] {"halo", "crown", "top_hat", "china_hat", "ears", "horns", "glowing_eyes"}) {
			this.enable(id, true);
			this.context.waitTicks(6);
			this.shot("cs_" + id);
			this.enable(id, false);
		}
		this.enable("halo", true);
		this.set("halo", "style", "\"golden\"");
		this.context.waitTicks(4);
		this.shot("cs_halo_golden");
		this.enable("halo", false);
		this.enable("ears", true);
		this.set("ears", "kind", "\"bunny\"");
		this.set("ears", "outer", "{\"color\":\"#F2F2F2\",\"mode\":\"static\"}");
		this.context.waitTicks(4);
		this.shot("cs_ears_bunny");
		this.enable("ears", false);
	}

	/** Pieces on the back, seen from behind. */
	private void back() {
		this.stand(180f, 10f);
		this.camera(CameraType.THIRD_PERSON_BACK);
		this.context.waitTicks(4);
		this.shot("cs_back_none");
		this.enable("cape", true);
		for (String design : new String[] {"obsidian", "gradient", "galaxy", "aurora", "carbon", "sunset"}) {
			this.set("cape", "design", "\"" + design + "\"");
			this.context.waitTicks(4);
			this.shot("cs_cape_" + design);
		}
		this.enable("cape", false);
		for (String style : new String[] {"angel", "dragon", "energy"}) {
			this.enable("wings", true);
			this.set("wings", "style", "\"" + style + "\"");
			this.context.waitTicks(4);
			this.shot("cs_wings_" + style);
		}
		this.enable("wings", false);
		for (String kind : new String[] {"fox", "cat", "wolf"}) {
			this.enable("tail", true);
			this.set("tail", "kind", "\"" + kind + "\"");
			if (!kind.equals("fox")) {
				this.set("tail", "base", kind.equals("cat") ? "{\"color\":\"#1E1E22\",\"mode\":\"static\"}" : "{\"color\":\"#8C8F96\",\"mode\":\"static\"}");
			}
			this.context.waitTicks(4);
			this.shot("cs_tail_" + kind);
		}
		this.enable("tail", false);
		this.enable("scarf", true);
		this.context.waitTicks(4);
		this.shot("cs_scarf");
		this.enable("scarf", false);
	}

	/** Effects around the player, seen from the front. */
	private void around() {
		this.stand(0f, 10f);
		this.camera(CameraType.THIRD_PERSON_FRONT);
		for (String id : new String[] {"aura", "orbit_rings", "companion", "rain_cloud"}) {
			this.enable(id, true);
			this.context.waitTicks(20);
			this.shot("cs_" + id);
			this.enable(id, false);
		}
	}

	/** Trails, footsteps and jump circles while moving, seen from behind. */
	private void motion() {
		this.stand(180f, 20f);
		this.camera(CameraType.THIRD_PERSON_BACK);
		this.enable("trails", true);
		this.enable("footsteps", true);
		this.context.getInput().holdKeyFor(o -> o.keyUp, 20);
		this.context.getInput().holdKeyFor(o -> o.keyLeft, 10);
		this.shot("cs_trails_footsteps");
		this.enable("trails", false);
		this.context.waitTicks(4);
		this.enable("jump_circles", true);
		this.context.getInput().holdKeyFor(o -> o.keyJump, 2);
		this.context.waitTicks(4);
		this.shot("cs_jump");
		this.context.waitTicks(8);
		this.shot("cs_land");
		this.enable("jump_circles", false);
		this.enable("footsteps", false);
	}

	/** Target indicator styles and a kill effect on a husk. */
	private void combat() {
		this.stand(0f, 12f);
		this.camera(CameraType.FIRST_PERSON);
		this.command("gamemode survival @a");
		this.command("summon minecraft:husk %.1f %d %.1f {NoAI:1b,Silent:1b,PersistenceRequired:1b,IsBaby:0b,Health:100f,attributes:[{id:\"max_health\",base:100}]}",
			this.bx + 0.5, this.by, this.bz + 2.6);
		this.context.waitTicks(10);
		for (String style : new String[] {"ring", "orbit", "marker"}) {
			this.enable("target_indicator", true);
			this.set("target_indicator", "style", "\"" + style + "\"");
			this.context.waitTicks(14);
			this.context.getInput().pressMouse(0);
			this.context.waitTicks(3);
			this.shot("cs_target_" + style);
		}
		this.enable("target_indicator", false);
		this.command("kill @e[type=minecraft:husk]");
		this.command("item replace entity @p weapon.mainhand with minecraft:diamond_sword");
		this.context.waitTicks(60);
		for (String effect : new String[] {"lightning", "shockwave", "firework"}) {
			this.command("summon minecraft:husk %.1f %d %.1f {NoAI:1b,Silent:1b,PersistenceRequired:1b,IsBaby:0b,attributes:[{id:\"minecraft:max_health\",base:1.0}]}",
				this.bx + 0.5, this.by, this.bz + 2.6);
			this.enable("kill_effects", true);
			this.set("kill_effects", "effect", "\"" + effect + "\"");
			this.set("kill_effects", "sound", "false");
			this.context.waitTicks(14);
			this.context.getInput().pressMouse(0);
			this.context.waitTicks(switch (effect) {
				case "lightning" -> 3;
				case "shockwave" -> 6;
				default -> 8;
			});
			this.shot("cs_kill_" + effect);
			this.command("kill @e[type=minecraft:husk]");
			this.command("kill @e[type=minecraft:item]");
			this.context.waitTicks(40);
		}
		this.enable("kill_effects", false);
		this.command("item replace entity @p weapon.mainhand with minecraft:air");
		this.command("gamemode creative @a");
	}

	/** Your own name tag restyled, mini me and hidden armor, seen from the front. */
	private void player() {
		this.stand(0f, 10f);
		this.camera(CameraType.THIRD_PERSON_FRONT);
		this.set("cosmetics", "own_name", "true");
		this.context.waitTicks(4);
		this.shot("cs_own_name");
		this.enable("nametags", true);
		this.context.waitTicks(4);
		this.shot("cs_nametags");
		this.enable("nametags", false);
		this.set("cosmetics", "own_name", "false");
		this.command("item replace entity @p armor.head with minecraft:diamond_helmet");
		this.command("item replace entity @p armor.chest with minecraft:diamond_chestplate");
		this.context.waitTicks(4);
		this.shot("cs_armor");
		this.enable("hide_armor", true);
		this.set("hide_armor", "chestplate", "true");
		this.context.waitTicks(4);
		this.shot("cs_armor_hidden");
		this.enable("hide_armor", false);
		this.command("item replace entity @p armor.head with minecraft:air");
		this.command("item replace entity @p armor.chest with minecraft:air");
		this.enable("player_scale", true);
		this.context.waitTicks(4);
		this.shot("cs_mini_me");
		this.enable("player_scale", false);
	}

	/** Several cosmetics at once on the inventory's player preview. */
	private void inventory() {
		this.stand(0f, 0f);
		this.camera(CameraType.FIRST_PERSON);
		this.command("gamemode survival @a");
		this.enable("cape", true);
		this.enable("halo", true);
		this.enable("wings", true);
		this.set("wings", "style", "\"energy\"");
		this.context.setScreen(() -> new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
		this.context.waitTicks(10);
		this.shot("cs_inventory");
		this.context.setScreen(() -> null);
		this.enable("cape", false);
		this.enable("halo", false);
		this.enable("wings", false);
		this.command("gamemode creative @a");
	}

	private void stand(float yaw, float pitch) {
		this.command("tp @p %.1f %d %.1f %.1f %.1f", this.bx + 0.5, this.by, this.bz + 0.5, yaw, pitch);
		this.context.waitTicks(3);
		this.context.runOnClient(c -> {
			c.player.setYRot(yaw);
			c.player.setXRot(pitch);
			c.player.yRotO = yaw;
			c.player.xRotO = pitch;
			c.player.yBodyRot = yaw;
			c.player.yBodyRotO = yaw;
			c.player.yHeadRot = yaw;
			c.player.yHeadRotO = yaw;
		});
		this.context.waitTicks(2);
	}

	private void camera(CameraType type) {
		this.context.runOnClient(c -> c.options.setCameraType(type));
	}

	private void command(String format, Object... args) {
		this.server.runCommand(args.length == 0 ? format : String.format(Locale.ROOT, format, args));
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
