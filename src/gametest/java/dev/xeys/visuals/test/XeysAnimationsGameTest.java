package dev.xeys.visuals.test;

import com.google.gson.JsonParser;
import dev.xeys.visuals.Xeys;
import dev.xeys.visuals.anim.CameraMotion;
import dev.xeys.visuals.core.Keybinds;
import dev.xeys.visuals.core.module.ModuleManager;
import dev.xeys.visuals.core.setting.Setting;
import dev.xeys.visuals.render.world.CameraFx;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;

/**
 * Animations: hands, swings and item switches in first person, zoom, camera motion, creature reactions,
 * emotes and the swing trail (sections: hands, swings, switch, zoom, camera, glide, entities, emotes, trail).
 */
public final class XeysAnimationsGameTest implements FabricClientGameTest {
	private ClientGameTestContext context;
	private TestServerContext server;
	private int bx;
	private int by;
	private int bz;

	@Override
	public void runTest(ClientGameTestContext ctx) {
		if (!TestFilter.enabled("animations")) {
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

			if (TestFilter.section("hands")) {
				this.hands();
			}
			if (TestFilter.section("swings")) {
				this.swings();
			}
			if (TestFilter.section("switch")) {
				this.itemSwitch();
			}
			if (TestFilter.section("zoom")) {
				this.zoom();
			}
			if (TestFilter.section("glide")) {
				this.glide();
			}
			if (TestFilter.section("emotes")) {
				this.emotes();
			}
			if (TestFilter.section("trail")) {
				this.trail();
			}
			if (TestFilter.section("entities")) {
				this.entities();
			}
			// Last: hurts the player and blows a crater behind the stand.
			if (TestFilter.section("camera")) {
				this.camera();
			}
			this.camera(CameraType.FIRST_PERSON);
		}
		ctx.setScreen(TitleScreen::new);
		ctx.waitTicks(2);
	}

	/** View model, block-hit and hand sway with a sword and a torch. */
	private void hands() {
		this.stand(180f, 0f);
		this.camera(CameraType.FIRST_PERSON);
		this.command("item replace entity @p weapon.mainhand with minecraft:diamond_sword");
		this.command("item replace entity @p weapon.offhand with minecraft:torch");
		this.context.waitTicks(12);
		this.shot("an_hand_vanilla");
		this.enable("view_model", true);
		this.set("view_model", "x", "0.12");
		this.set("view_model", "y", "0.06");
		this.set("view_model", "z", "0.2");
		this.set("view_model", "rot_y", "25");
		this.set("view_model", "scale", "70");
		this.context.waitTicks(3);
		this.shot("an_view_model");
		this.enable("view_model", false);

		this.enable("old_animations", true);
		this.context.getInput().holdKey(o -> o.keyUse);
		this.context.waitTicks(4);
		this.shot("an_blockhit");
		this.enable("swing_speed", true);
		this.set("swing_speed", "speed", "25");
		this.context.getInput().pressMouse(0);
		this.context.waitTicks(4);
		this.shot("an_blockhit_swing_a");
		this.context.waitTicks(4);
		this.shot("an_blockhit_swing_b");
		this.context.waitTicks(6);
		this.shot("an_blockhit_swing_c");
		this.context.getInput().releaseKey(o -> o.keyUse);
		this.context.waitTicks(20);
		this.enable("swing_speed", false);
		this.enable("old_animations", false);

		// Sway: the hands lag behind a sudden turn.
		this.enable("hand_sway", true);
		this.set("hand_sway", "sway", "300");
		this.context.waitTicks(4);
		this.turn(150f);
		this.context.waitTicks(1);
		this.shot("an_sway");
		this.enable("hand_sway", false);
		this.command("item replace entity @p weapon.offhand with minecraft:air");
	}

	/** Each swing style, frozen part way through a slowed swing. */
	private void swings() {
		this.stand(180f, 0f);
		this.camera(CameraType.FIRST_PERSON);
		this.command("item replace entity @p weapon.mainhand with minecraft:diamond_sword");
		this.context.waitTicks(10);
		// Without the attack cooldown dip, so the frames show the swing alone.
		this.enable("item_switch", true);
		this.set("item_switch", "style", "\"instant\"");
		this.enable("swing_speed", true);
		this.set("swing_speed", "speed", "25");
		this.swingShots("vanilla");
		this.enable("swing_style", true);
		for (String style : new String[] {"smooth", "punch", "slash", "chop", "spin"}) {
			this.set("swing_style", "style", "\"" + style + "\"");
			this.swingShots(style);
		}
		this.enable("swing_style", false);
		this.enable("swing_speed", false);
		this.enable("item_switch", false);
	}

	/** Frames at about a fifth, two fifths, three fifths and four fifths through a 24-tick swing. */
	private void swingShots(String style) {
		this.context.getInput().pressMouse(0);
		this.context.waitTicks(4);
		this.shot("an_swing_" + style + "_a");
		this.context.waitTicks(4);
		this.shot("an_swing_" + style + "_b");
		this.context.waitTicks(4);
		this.shot("an_swing_" + style + "_c");
		this.context.waitTicks(4);
		this.shot("an_swing_" + style + "_d");
		this.context.waitTicks(12);
	}

	/** The equip styles, caught as the new item comes in. */
	private void itemSwitch() {
		this.stand(180f, 0f);
		this.camera(CameraType.FIRST_PERSON);
		this.command("item replace entity @p hotbar.0 with minecraft:diamond_sword");
		this.command("item replace entity @p hotbar.1 with minecraft:golden_axe");
		this.context.getInput().pressKey(o -> o.keyHotbarSlots[0]);
		this.context.waitTicks(15);
		this.enable("item_switch", true);
		for (String style : new String[] {"slide", "spin", "pop"}) {
			this.set("item_switch", "style", "\"" + style + "\"");
			this.context.getInput().pressKey(o -> o.keyHotbarSlots[1]);
			this.context.waitTicks(1);
			this.shot("an_switch_" + style + "_a");
			this.context.waitTicks(1);
			this.shot("an_switch_" + style + "_b");
			this.context.getInput().pressKey(o -> o.keyHotbarSlots[0]);
			this.context.waitTicks(15);
		}
		this.set("item_switch", "style", "\"instant\"");
		this.context.getInput().pressKey(o -> o.keyHotbarSlots[1]);
		this.context.waitTicks(1);
		this.shot("an_switch_instant");
		this.enable("item_switch", false);
		this.context.getInput().pressKey(o -> o.keyHotbarSlots[0]);
		this.context.waitTicks(10);
	}

	/** Holding the zoom key, scrolling while zoomed and releasing. */
	private void zoom() {
		this.stand(180f, 0f);
		this.camera(CameraType.FIRST_PERSON);
		this.context.waitTicks(4);
		this.shot("an_zoom_off");
		this.enable("zoom", true);
		this.context.getInput().holdKey(Keybinds.zoom);
		this.context.waitTicks(15);
		this.shot("an_zoom_held");
		for (int i = 0; i < 4; i++) {
			this.context.getInput().scroll(1.0);
			this.context.waitTicks(1);
		}
		this.context.waitTicks(12);
		this.shot("an_zoom_scrolled");
		this.set("zoom", "hide_hand", "true");
		this.context.waitTicks(2);
		this.shot("an_zoom_no_hand");
		this.context.getInput().releaseKey(Keybinds.zoom);
		this.context.waitTicks(15);
		this.shot("an_zoom_released");
		this.set("zoom", "hide_hand", "false");
		this.enable("zoom", false);
	}

	/** Switching to third person with the glide on and a longer distance. */
	private void glide() {
		this.stand(0f, 10f);
		this.camera(CameraType.FIRST_PERSON);
		this.enable("perspective_glide", true);
		this.set("perspective_glide", "distance", "160");
		this.context.waitTicks(3);
		this.camera(CameraType.THIRD_PERSON_BACK);
		this.context.waitTicks(1);
		this.shot("an_glide_start");
		this.context.waitTicks(20);
		this.shot("an_glide_end");
		this.enable("perspective_glide", false);
		this.camera(CameraType.FIRST_PERSON);
	}

	/** Each emote, played with the emote key; the camera turns to the front and back again. */
	private void emotes() {
		this.stand(180f, 0f);
		this.camera(CameraType.FIRST_PERSON);
		this.command("item replace entity @p weapon.mainhand with minecraft:air");
		this.enable("emotes", true);
		for (String emote : new String[] {"wave", "cheer", "clap", "dab", "salute", "point", "bow", "spin"}) {
			this.set("emotes", "emote", "\"" + emote + "\"");
			this.context.getInput().holdKey(Keybinds.emote);
			this.context.waitTicks(12);
			this.shot("an_emote_" + emote);
			this.context.getInput().releaseKey(Keybinds.emote);
			this.context.waitTicks(8);
		}
		this.shot("an_emote_released");
		this.enable("emotes", false);
	}

	/** The crescent left by a sword swing. */
	private void trail() {
		this.stand(180f, 0f);
		this.camera(CameraType.FIRST_PERSON);
		this.command("item replace entity @p weapon.mainhand with minecraft:diamond_sword");
		this.context.waitTicks(10);
		this.enable("swing_trail", true);
		// Slowed to a quarter, so frames a tick apart show the arc being drawn.
		this.enable("swing_speed", true);
		this.set("swing_speed", "speed", "25");
		this.context.getInput().pressMouse(0);
		this.context.waitTicks(1);
		this.shot("an_trail_a");
		this.context.waitTicks(2);
		this.shot("an_trail_b");
		this.context.waitTicks(3);
		this.shot("an_trail_c");
		this.context.waitTicks(4);
		this.shot("an_trail_d");
		this.context.waitTicks(30);
		this.enable("swing_speed", false);
		this.enable("swing_trail", false);
	}

	/** Hit squash with red armor on an armored husk, then each death style. */
	private void entities() {
		this.stand(180f, 12f);
		this.camera(CameraType.FIRST_PERSON);
		this.command("item replace entity @p weapon.mainhand with minecraft:air");
		this.command("summon minecraft:husk %.1f %d %.1f {NoAI:1b,Silent:1b,PersistenceRequired:1b,IsBaby:0b,Rotation:[0f,0f],"
			+ "equipment:{head:{id:\"minecraft:iron_helmet\"},chest:{id:\"minecraft:iron_chestplate\"}}}", this.bx + 0.5, this.by, this.bz - 2.1);
		this.context.waitTicks(10);
		this.enable("hit_squash", true);
		this.set("hit_squash", "strength", "250");
		this.enable("old_animations", true);
		this.context.getInput().pressMouse(0);
		this.context.waitTicks(2);
		this.shot("an_squash_red_armor");
		this.context.waitTicks(20);
		this.enable("hit_squash", false);
		this.enable("old_animations", false);
		this.command("kill @e[type=minecraft:husk]");
		this.context.waitTicks(30);
		this.enable("death_animation", true);
		for (String style : new String[] {"sink", "shrink", "ascend", "spin"}) {
			this.set("death_animation", "style", "\"" + style + "\"");
			this.command("summon minecraft:husk %.1f %d %.1f {NoAI:1b,Silent:1b,PersistenceRequired:1b,IsBaby:0b,Rotation:[0f,0f]}",
				this.bx + 0.5, this.by, this.bz - 2.1);
			this.context.waitTicks(8);
			this.command("kill @e[type=minecraft:husk]");
			this.context.waitTicks(9);
			this.shot("an_death_" + style);
			this.context.waitTicks(30);
			this.command("kill @e[type=minecraft:item]");
		}
		this.enable("death_animation", false);
	}

	/** Strafe tilt, landing dip, sneaking, bobbing, hurt reactions, an explosion and water sway. */
	private void camera() {
		this.stand(180f, 0f);
		this.camera(CameraType.FIRST_PERSON);
		this.command("item replace entity @p weapon.mainhand with minecraft:diamond_sword");
		this.context.waitTicks(6);
		this.shot("an_cam_still");

		this.enable("strafe_tilt", true);
		this.set("strafe_tilt", "tilt", "8");
		this.context.getInput().holdKey(o -> o.keyRight);
		this.context.waitTicks(10);
		this.shot("an_cam_strafe_right");
		this.context.getInput().releaseKey(o -> o.keyRight);
		this.context.waitTicks(15);
		this.enable("strafe_tilt", false);

		this.enable("landing_dip", true);
		this.set("landing_dip", "strength", "300");
		this.stand(180f, 0f);
		this.command("tp @p %.1f %d %.1f 180 0", this.bx + 0.5, this.by + 5, this.bz + 0.5);
		for (int i = 0; i < 20 && this.context.computeOnClient(c -> c.player.onGround()); i++) {
			this.context.waitTick();
		}
		for (int i = 0; i < 60 && !this.context.computeOnClient(c -> c.player.onGround()); i++) {
			this.context.waitTick();
		}
		float dip = 0f;
		for (int i = 0; i < 8; i++) {
			this.context.waitTick();
			dip = Math.max(dip, this.context.computeOnClient(c -> CameraMotion.landingDip()));
			if (i == 2) {
				this.context.takeScreenshot("an_cam_landing");
			}
		}
		Xeys.LOG.info("landing dip peak {}", dip);
		if (dip < 0.05f) {
			throw new AssertionError("Landing dip did not move the camera: " + dip);
		}
		this.context.waitTicks(20);
		this.enable("landing_dip", false);

		// Smooth sneak: after two ticks of sneaking the eye has dropped much less than vanilla's.
		this.stand(180f, 0f);
		float vanillaDrop = this.sneakDrop();
		this.enable("smooth_sneak", true);
		this.set("smooth_sneak", "smoothness", "100");
		float smoothDrop = this.sneakDrop();
		this.enable("smooth_sneak", false);
		Xeys.LOG.info("eye drop after 2 sneak ticks: vanilla {} smooth {}", vanillaDrop, smoothDrop);
		if (!(smoothDrop < vanillaDrop * 0.6f)) {
			throw new AssertionError("Smooth sneak did not slow the eye: vanilla " + vanillaDrop + " smooth " + smoothDrop);
		}

		this.stand(180f, 0f);
		this.enable("view_bobbing", true);
		this.set("view_bobbing", "world", "0");
		this.set("view_bobbing", "hand", "200");
		this.context.getInput().holdKey(o -> o.keyUp);
		this.context.waitTicks(7);
		this.shot("an_cam_bob");
		this.context.getInput().releaseKey(o -> o.keyUp);
		this.context.waitTicks(10);
		this.enable("view_bobbing", false);

		this.stand(180f, 0f);
		this.command("gamemode survival @a");
		this.context.waitTicks(4);
		this.command("damage @p 1 minecraft:generic");
		this.context.waitTicks(2);
		this.shot("an_cam_hurt_vanilla");
		this.context.waitTicks(30);
		this.enable("hurt_camera", true);
		this.set("hurt_camera", "tilt", "300");
		this.set("hurt_camera", "kick", "6");
		this.enable("camera_shake", true);
		this.set("camera_shake", "strength", "200");
		this.command("damage @p 1 minecraft:generic");
		this.context.waitTicks(2);
		this.shot("an_cam_hurt_shake");
		this.context.waitTicks(30);
		this.enable("hurt_camera", false);

		// Low health: the heartbeat pulses the field of view.
		this.enable("heartbeat", true);
		this.command("damage @p 15 minecraft:generic");
		this.context.waitTicks(10);
		float low = 1f;
		float high = 0f;
		for (int i = 0; i < 30; i++) {
			this.context.waitTick();
			float m = this.context.computeOnClient(c -> CameraFx.fovMultiplier(0f));
			low = Math.min(low, m);
			high = Math.max(high, m);
		}
		Xeys.LOG.info("heartbeat fov multiplier range {} .. {}", low, high);
		if (high - low < 0.005f) {
			throw new AssertionError("Heartbeat did not pulse the field of view: " + low + " .. " + high);
		}
		this.enable("heartbeat", false);
		this.command("effect give @p minecraft:instant_health 1 5");
		this.command("gamemode creative @a");
		this.context.waitTicks(10);

		// An explosion behind the stand shakes the camera.
		this.command("summon minecraft:tnt %.1f %d %.1f {fuse:0}", this.bx + 0.5, this.by, this.bz + 5.5);
		float shake = 0f;
		for (int i = 0; i < 6; i++) {
			this.context.waitTick();
			shake = Math.max(shake, this.context.computeOnClient(c -> CameraMotion.trauma()));
			if (i == 2) {
				this.context.takeScreenshot("an_cam_explosion");
			}
		}
		Xeys.LOG.info("explosion shake peak {}", shake);
		if (shake < 0.3f) {
			throw new AssertionError("The explosion did not shake the camera: " + shake);
		}
		this.context.waitTicks(30);
		this.enable("camera_shake", false);

		this.enable("water_sway", true);
		this.set("water_sway", "strength", "300");
		this.command("fill %d %d %d %d %d %d minecraft:water", this.bx - 3, this.by, this.bz - 3, this.bx + 3, this.by + 3, this.bz + 3);
		this.stand(180f, 0f);
		this.context.waitTicks(25);
		this.shot("an_cam_water_a");
		this.context.waitTicks(12);
		this.shot("an_cam_water_b");
		this.command("fill %d %d %d %d %d %d minecraft:air", this.bx - 3, this.by, this.bz - 3, this.bx + 3, this.by + 3, this.bz + 3);
		this.enable("water_sway", false);
		this.context.waitTicks(10);
	}

	/** How far the camera's eye has dropped two ticks into sneaking. */
	private float sneakDrop() {
		this.context.waitTicks(10);
		double before = this.context.computeOnClient(c -> c.gameRenderer.getMainCamera().position().y);
		this.context.getInput().holdKey(o -> o.keyShift);
		this.context.waitTicks(2);
		double after = this.context.computeOnClient(c -> c.gameRenderer.getMainCamera().position().y);
		this.context.getInput().releaseKey(o -> o.keyShift);
		this.context.waitTicks(20);
		return (float) (before - after);
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
			c.player.yBob = yaw;
			c.player.yBobO = yaw;
			c.player.xBob = pitch;
			c.player.xBobO = pitch;
		});
		this.context.waitTicks(2);
	}

	/** Turns the view at once, as a fast flick of the mouse would. */
	private void turn(float degrees) {
		this.context.runOnClient(c -> {
			float yaw = c.player.getYRot() + degrees;
			c.player.setYRot(yaw);
			c.player.yRotO = yaw;
		});
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
