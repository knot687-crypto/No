package dev.xeys.visuals.modules.hud;

import dev.xeys.visuals.core.input.InputStats;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.InfoHudModule;
import dev.xeys.visuals.ui.Icons;

/** Clicks per second for the left (and optionally right) mouse button. */
public final class CpsModule extends InfoHudModule {
	private final BoolSetting showRight = this.add(new BoolSetting("show_right", "Right Button", true).group("Display"));

	public CpsModule() {
		super("cps", "CPS", Icons.MOUSE_POINTER, "Clicks per second for each mouse button", "CPS", 20f, HudPosition.Anchor.TOP_LEFT, 6f, 126f);
	}

	@Override
	protected void update(StringBuilder out, boolean preview) {
		out.append(InputStats.leftCps());
		if (this.showRight.on()) {
			out.append(" · ").append(InputStats.rightCps());
		}
	}
}
