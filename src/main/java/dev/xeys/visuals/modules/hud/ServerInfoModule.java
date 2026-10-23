package dev.xeys.visuals.modules.hud;

import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.InfoHudModule;
import dev.xeys.visuals.ui.Icons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

/** Server address (or Singleplayer) and online player count. */
public final class ServerInfoModule extends InfoHudModule {
	private final BoolSetting players = this.add(new BoolSetting("players", "Player Count", true).group("Display"));

	public ServerInfoModule() {
		super("server_info", "Server", Icons.SERVER, "The server you are on and how many are online", "", 1f, HudPosition.Anchor.TOP_RIGHT, -6f, 94f);
	}

	@Override
	protected void update(StringBuilder out, boolean preview) {
		Minecraft mc = Minecraft.getInstance();
		ServerData server = mc.getCurrentServer();
		if (mc.hasSingleplayerServer() || (server == null && mc.level != null)) {
			out.append("Singleplayer");
		} else if (server != null) {
			out.append(server.ip);
		} else {
			out.append("play.example.net");
		}
		if (this.players.on() && mc.getConnection() != null) {
			out.append("  ·  ").append(mc.getConnection().getOnlinePlayers().size()).append(" online");
		}
	}
}
