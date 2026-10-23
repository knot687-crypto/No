package dev.xeys.visuals.modules.hud;

import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.hud.HudMath;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.InfoHudModule;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Fmt;

/** Which way you are facing, as a compass point and bearing (0 = north, 90 = east). */
public final class DirectionModule extends InfoHudModule {
	private final BoolSetting full = this.add(new BoolSetting("full_names", "Full Names", false).group("Display"));
	private final BoolSetting degrees = this.add(new BoolSetting("degrees", "Bearing", true).describe("Degrees clockwise from north").group("Display"));

	public DirectionModule() {
		super("direction", "Direction", Icons.NAVIGATION, "Which way you are facing", "Facing", 30f, HudPosition.Anchor.TOP_LEFT, 6f, 192f);
	}

	@Override
	protected void update(StringBuilder out, boolean preview) {
		float heading = HudMath.heading(1f);
		int idx = HudMath.point(heading);
		out.append(this.full.on() ? HudMath.pointName(idx) : HudMath.pointLabel(idx));
		if (this.degrees.on()) {
			out.append("  ");
			Fmt.fixed(out, Math.floorMod(Math.round(heading), 360), 0).append('°');
		}
	}
}
