package dev.xeys.visuals.cosmetic;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.event.WorldEvents;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** The render layer every player model carries; it hands the posed model to each enabled cosmetic. */
public final class CosmeticLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private final CosmeticContext context = new CosmeticContext();

	public CosmeticLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
		super(parent);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
		if (state.isInvisible || WorldEvents.PLAYER_LAYER.isEmpty() || !Cosmetics.shownOn(state.id)) {
			return;
		}
		CosmeticContext c = this.context;
		c.pose = poseStack;
		c.collector = collector;
		c.light = light;
		c.state = state;
		c.model = this.getParentModel();
		c.time = (float) (Clock.time() % 3600.0);
		c.self = Cosmetics.isSelf(state.id);
		for (Object l : WorldEvents.PLAYER_LAYER.snapshot()) {
			poseStack.pushPose();
			((WorldEvents.PlayerLayer) l).submitOnPlayer(c);
			poseStack.popPose();
		}
		c.state = null;
		c.model = null;
	}
}
