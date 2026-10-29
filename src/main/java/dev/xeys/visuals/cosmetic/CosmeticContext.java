package dev.xeys.visuals.cosmetic;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Everything a cosmetic needs to draw on one player this frame. The pose starts at the model's root:
 * one unit is one block, y points down, the face looks toward -z. Body parts are already posed, so
 * {@code model.head.translateAndRotate(pose)} moves into head space. Reused; never kept.
 */
public final class CosmeticContext {
	public PoseStack pose;
	public SubmitNodeCollector collector;
	/** Packed light at the player. */
	public int light;
	public AvatarRenderState state;
	public PlayerModel model;
	/** Smooth time in seconds, for animation. */
	public float time;
	/** Whether this is the local player. */
	public boolean self;
}
