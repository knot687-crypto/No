package dev.xeys.visuals.cosmetic;

import dev.xeys.visuals.Xeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/** Shared cosmetic state: who wears cosmetics, and the render types solid and emissive pieces use. */
public final class Cosmetics {
	/** A plain white texture; cosmetic geometry is colored per vertex. */
	public static final Identifier WHITE = Xeys.id("textures/cosmetic/white.png");
	/** Lit, double-sided solid geometry. */
	public static final RenderType SOLID = RenderTypes.entityCutoutNoCull(WHITE);
	/** Lit, double-sided translucent geometry. */
	public static final RenderType TRANSLUCENT = RenderTypes.entityTranslucent(WHITE);
	/** Unlit, additive light (glowing eyes, gems, energy). */
	public static final RenderType EMISSIVE = RenderTypes.eyes(WHITE);

	/** Whether other players also show your cosmetics (they only ever exist on your screen). */
	public static boolean onOthers;

	private Cosmetics() {
	}

	public static boolean isSelf(int entityId) {
		Player player = Minecraft.getInstance().player;
		return player != null && player.getId() == entityId;
	}

	public static boolean shownOn(int entityId) {
		return onOthers || isSelf(entityId);
	}

	/** Whether the camera is in first person (effects around your own body would sit in your view). */
	public static boolean firstPerson() {
		return Minecraft.getInstance().options.getCameraType().isFirstPerson();
	}
}
