package dev.xeys.visuals.modules.hud;

import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.InfoHudModule;
import dev.xeys.visuals.ui.Icons;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.biome.Biome;

/** The biome you are standing in, using its translated name. */
public final class BiomeModule extends InfoHudModule {
	private Holder<Biome> lastBiome;
	private String lastName = "Plains";

	public BiomeModule() {
		super("biome", "Biome", Icons.TREES, "The biome you are standing in", "Biome", 4f, HudPosition.Anchor.TOP_LEFT, 6f, 214f);
	}

	@Override
	protected void update(StringBuilder out, boolean preview) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.level != null) {
			Holder<Biome> biome = mc.level.getBiome(mc.player.blockPosition());
			if (biome != this.lastBiome) {
				this.lastBiome = biome;
				this.lastName = biome.unwrapKey()
					.map(k -> Component.translatable(k.identifier().toLanguageKey("biome")).getString())
					.orElse("Unknown");
			}
		}
		out.append(this.lastName);
	}
}
