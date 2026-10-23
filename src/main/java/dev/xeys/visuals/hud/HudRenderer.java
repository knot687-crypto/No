package dev.xeys.visuals.hud;

import dev.xeys.visuals.core.module.ModuleManager;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Renders every enabled HUD module at its anchored position. Registered as a Fabric HUD layer. */
public final class HudRenderer {
	private HudRenderer() {
	}

	public static void render(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen instanceof HudEditorScreen) {
			return;
		}
		if (HudStyle.config().hideInDebug.on() && mc.debugEntries.isOverlayVisible()) {
			return;
		}
		float partial = delta.getGameTimeDeltaPartialTick(false);
		float sw = g.guiWidth();
		float sh = g.guiHeight();
		List<HudModule> modules = ModuleManager.hudModules();
		for (int i = 0, n = modules.size(); i < n; i++) {
			HudModule m = modules.get(i);
			if (m.isEnabled() && m.hasContent(false)) {
				renderModule(g, m, sw, sh, partial, false);
			}
		}
	}

	/** Lays out and draws one module at its configured position. */
	public static void renderModule(GuiGraphics g, HudModule m, float screenW, float screenH, float partial, boolean preview) {
		m.layoutFrame(preview);
		float s = m.position.scale();
		float x = m.position.left(screenW, m.width() * s);
		float y = m.position.top(screenH, m.height() * s);
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(s, s);
		m.drawFrame(g, partial, preview);
		g.pose().popMatrix();
	}
}
