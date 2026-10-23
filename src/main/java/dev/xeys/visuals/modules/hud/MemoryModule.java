package dev.xeys.visuals.modules.hud;

import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.InfoHudModule;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Fmt;

/** Java heap usage. */
public final class MemoryModule extends InfoHudModule {
	private final BoolSetting percent = this.add(new BoolSetting("percent", "Percentage", false).group("Display"));

	public MemoryModule() {
		super("memory", "Memory", Icons.CPU, "Game memory usage", "Memory", 1f, HudPosition.Anchor.TOP_RIGHT, -6f, 28f);
	}

	@Override
	protected void update(StringBuilder out, boolean preview) {
		Runtime rt = Runtime.getRuntime();
		long used = rt.totalMemory() - rt.freeMemory();
		long max = rt.maxMemory();
		if (this.percent.on()) {
			out.append(Math.round(used * 100.0 / max)).append('%');
		} else {
			Fmt.fixed(out, used / 1073741824.0, 1).append(" / ");
			Fmt.fixed(out, max / 1073741824.0, 1).append(" GB");
		}
	}
}
