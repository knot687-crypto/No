package dev.xeys.visuals.modules.hud;

import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.HudStyle;
import dev.xeys.visuals.hud.InfoHudModule;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;

/** Connection latency with signal bars. */
public final class PingModule extends InfoHudModule {
	private final BoolSetting colorCoded = this.add(new BoolSetting("color_coded", "Latency Colors", true).group("Display"));
	private int ping = -1;

	public PingModule() {
		super("ping", "Ping", Icons.WIFI, "Latency to the server with signal bars", "Ping", 2f, HudPosition.Anchor.TOP_LEFT, 6f, 148f);
	}

	@Override
	protected void update(StringBuilder out, boolean preview) {
		Minecraft mc = Minecraft.getInstance();
		this.ping = -1;
		if (mc.player != null && mc.getConnection() != null) {
			PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
			if (info != null) {
				this.ping = info.getLatency();
			}
		} else if (preview) {
			this.ping = 32;
		}
		if (this.ping < 0) {
			out.append("--");
		} else {
			out.append(this.ping).append(" ms");
		}
	}

	private int quality() {
		if (this.ping < 0) {
			return 0;
		}
		return this.ping < 60 ? 4 : this.ping < 120 ? 3 : this.ping < 220 ? 2 : 1;
	}

	@Override
	protected int valueColor() {
		if (!this.colorCoded.on() || this.ping < 0) {
			return HudStyle.text();
		}
		return switch (this.quality()) {
			case 4 -> Theme.SUCCESS;
			case 3 -> HudStyle.text();
			case 2 -> Theme.WARNING;
			default -> Theme.DANGER;
		};
	}

	/** Four signal bars instead of a glyph. */
	@Override
	protected void drawIcon(GuiGraphics g, float x, float centerY, float size) {
		float base = centerY + size * 0.42f;
		int q = this.quality();
		int on = this.colorCoded.on() ? this.valueColor() : HudStyle.accent();
		float bw = size * 0.17f;
		float gap = size * 0.09f;
		for (int i = 0; i < 4; i++) {
			float bh = size * (0.3f + 0.19f * i);
			int c = i < q ? on : Colors.withAlpha(0xFFFFFF, 0.18f);
			Draw.rect(g, x + i * (bw + gap), base - bh, bw, bh, bw * 0.5f, c);
		}
	}
}
