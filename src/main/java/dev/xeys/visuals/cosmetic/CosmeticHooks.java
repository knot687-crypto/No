package dev.xeys.visuals.cosmetic;

import dev.xeys.visuals.anim.Emotes;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Render-state changes for cosmetics, applied as each player's render state is extracted: a custom cape,
 * hidden armor, your own name tag and your model's scale. Only the render state changes, never the
 * player.
 */
public final class CosmeticHooks {
	/** The cape every cosmetic-wearing player shows, or null for their own. */
	public static ClientAsset.@Nullable Texture cape;
	/** Armor slots to hide: head, chest, legs, feet. */
	public static final boolean[] HIDE_ARMOR = new boolean[4];
	public static boolean ownNameTag;
	/** Scale of your own model. */
	public static float selfScale = 1f;

	private static @Nullable PlayerSkin skinIn;
	private static ClientAsset.@Nullable Texture skinCape;
	private static @Nullable PlayerSkin skinOut;

	private CosmeticHooks() {
	}

	public static void extract(Avatar avatar, AvatarRenderState state) {
		if (Cosmetics.isSelf(state.id)) {
			state.bodyRot += Emotes.spin();
		}
		if (!Cosmetics.shownOn(state.id)) {
			return;
		}
		ClientAsset.Texture c = cape;
		if (c != null) {
			state.skin = withCape(state.skin, c);
			state.showCape = true;
		}
		if (HIDE_ARMOR[0]) {
			state.headEquipment = ItemStack.EMPTY;
		}
		if (HIDE_ARMOR[1]) {
			state.chestEquipment = ItemStack.EMPTY;
		}
		if (HIDE_ARMOR[2]) {
			state.legsEquipment = ItemStack.EMPTY;
		}
		if (HIDE_ARMOR[3]) {
			state.feetEquipment = ItemStack.EMPTY;
		}
		if (ownNameTag && Cosmetics.isSelf(state.id) && state.nameTag == null && !avatar.isInvisible()) {
			Component name = avatar.getDisplayName();
			state.nameTag = name;
			state.nameTagAttachment = new Vec3(0.0, avatar.getBbHeight() + 0.5, 0.0);
		}
	}

	/** The skin with our cape; cached, so the same skin and cape give the same object every frame. */
	private static PlayerSkin withCape(PlayerSkin skin, ClientAsset.Texture capeTexture) {
		if (skin != skinIn || capeTexture != skinCape || skinOut == null) {
			skinIn = skin;
			skinCape = capeTexture;
			skinOut = new PlayerSkin(skin.body(), capeTexture, skin.elytra(), skin.model(), skin.secure());
		}
		return skinOut;
	}

	public static float scale(AvatarRenderState state) {
		return selfScale != 1f && Cosmetics.isSelf(state.id) ? selfScale : 1f;
	}
}
