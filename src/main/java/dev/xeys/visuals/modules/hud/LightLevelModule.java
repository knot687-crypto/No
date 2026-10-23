package dev.xeys.visuals.modules.hud;

import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.HudStyle;
import dev.xeys.visuals.hud.InfoHudModule;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.Icons;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;

/** Block light at your feet; amber when mobs can spawn. */
public final class LightLevelModule extends InfoHudModule {
	private final BoolSetting split = this.add(new BoolSetting("split", "Sky and Block", false)
		.describe("Show sky and block light separately").group("Display"));
	private int block = 15;

	public LightLevelModule() {
		super("light_level", "Light Level", Icons.LIGHTBULB, "Light level where you stand", "Light", 5f, HudPosition.Anchor.TOP_LEFT, 6f, 236f);
	}

	@Override
	protected void update(StringBuilder out, boolean preview) {
		Minecraft mc = Minecraft.getInstance();
		int sky = 15;
		this.block = 12;
		if (mc.player != null && mc.level != null) {
			BlockPos pos = mc.player.blockPosition();
			sky = mc.level.getBrightness(LightLayer.SKY, pos);
			this.block = mc.level.getBrightness(LightLayer.BLOCK, pos);
		}
		if (this.split.on()) {
			out.append("Sky ").append(sky).append("  Block ").append(this.block);
		} else {
			out.append(this.block);
		}
	}

	@Override
	protected int valueColor() {
		return this.block == 0 ? Theme.WARNING : HudStyle.text();
	}
}
