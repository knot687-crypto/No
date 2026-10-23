package dev.xeys.visuals.modules.hud;

import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.InfoHudModule;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Fmt;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/** Time since joining the current world or server. */
public final class SessionTimeModule extends InfoHudModule {
	private static long joinedAt = System.currentTimeMillis();

	static {
		ClientPlayConnectionEvents.JOIN.register((h, s, c) -> joinedAt = System.currentTimeMillis());
	}

	public SessionTimeModule() {
		super("session_time", "Session Time", Icons.TIMER, "How long you have been playing this session", "Session", 1f,
			HudPosition.Anchor.TOP_RIGHT, -6f, 50f);
	}

	@Override
	protected void update(StringBuilder out, boolean preview) {
		Fmt.duration(out, Math.max(0L, (System.currentTimeMillis() - joinedAt) / 1000L));
	}
}
