package dev.xeys.visuals.hud;

import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Renders every enabled HUD module where {@link HudLayout} placed it. Registered as a Fabric HUD layer. */
public final class HudRenderer {
	private HudRenderer() {
	}

	public static void render(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen instanceof HudEditorScreen) {
			return;
		}
		float partial = delta.getGameTimeDeltaPartialTick(false);
		if (mc.level != null) {
			dev.xeys.visuals.core.event.Events.renderOverlays(g, partial);
		}
		if (HudStyle.config().hideInDebug.on() && mc.debugEntries.isOverlayVisible()) {
			return;
		}
		List<HudModule> visible = HudLayout.compute(g.guiWidth(), g.guiHeight(), false);
		for (int i = 0, n = visible.size(); i < n; i++) {
			drawModule(g, visible.get(i), partial, false);
		}
	}

	/** Draws one already laid-out module at its frame position. */
	public static void drawModule(GuiGraphics g, HudModule m, float partial, boolean preview) {
		float s = m.position.scale();
		g.pose().pushMatrix();
		g.pose().translate(m.frameLeft(), m.frameTop());
		g.pose().scale(s, s);
		m.drawFrame(g, partial, preview);
		g.pose().popMatrix();
	}
}
