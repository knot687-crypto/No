package dev.xeys.visuals.modules.hud;

import dev.xeys.visuals.core.Session;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.InfoHudModule;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Fmt;

/** Time since joining the current world or server. */
public final class SessionTimeModule extends InfoHudModule {
	public SessionTimeModule() {
		super("session_time", "Session Time", Icons.TIMER, "How long you have been playing this session", "Session", 1f,
			HudPosition.Anchor.TOP_RIGHT, -6f, 50f);
	}

	@Override
	protected void update(StringBuilder out, boolean preview) {
		Fmt.duration(out, Session.seconds());
	}
}
