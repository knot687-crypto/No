package dev.xeys.visuals.core;

import com.mojang.blaze3d.platform.InputConstants;
import dev.xeys.visuals.Xeys;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.module.ModuleManager;
import dev.xeys.visuals.core.setting.KeybindSetting;
import dev.xeys.visuals.ui.clickgui.ClickGuiScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** The ClickGUI key (a vanilla key mapping, rebindable in Controls) and per-module keybinds. */
public final class Keybinds {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Xeys.id("xeys"));
	public static KeyMapping openGui;

	private Keybinds() {
	}

	public static void register() {
		openGui = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.xeys.open_gui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			while (openGui.consumeClick()) {
				if (mc.screen == null) {
					mc.setScreen(new ClickGuiScreen(null));
				}
			}
		});
	}

	/** Raw key events (from the keyboard mixin) while no screen is open. */
	public static void onKey(int key, int action) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen != null || key == KeybindSetting.NONE) {
			return;
		}
		for (Module m : ModuleManager.all()) {
			if (m.keybind.matchesKey(key)) {
				handle(m, action);
			}
		}
	}

	public static void onMouse(int button, int action) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen != null) {
			return;
		}
		for (Module m : ModuleManager.all()) {
			if (m.keybind.matchesMouse(button)) {
				handle(m, action);
			}
		}
	}

	private static void handle(Module m, int action) {
		if (m.keybind.behavior() == KeybindSetting.Behavior.HOLD) {
			if (action == GLFW.GLFW_PRESS) {
				m.setEnabled(true);
			} else if (action == GLFW.GLFW_RELEASE) {
				m.setEnabled(false);
			}
		} else if (action == GLFW.GLFW_PRESS) {
			m.toggle();
			dev.xeys.visuals.ui.notify.Notifications.moduleToggled(m);
		}
	}
}
