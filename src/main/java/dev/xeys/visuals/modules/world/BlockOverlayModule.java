package dev.xeys.visuals.modules.world;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.render.world.WorldDraw;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import dev.xeys.visuals.world.BlockTarget;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A translucent fill over the targeted block (or just the face you are looking at), optionally breathing. */
public final class BlockOverlayModule extends Module implements WorldEvents.BlockOutline {
	private static final float INFLATE = 0.0025f;

	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0x38FFFFFF, ColorSetting.Mode.ACCENT).group("Overlay"));
	private final BoolSetting faceOnly = this.add(new BoolSetting("face_only", "Face Only", false).describe("Fill only the face you look at")
		.group("Overlay"));
	private final BoolSetting pulse = this.add(new BoolSetting("pulse", "Breathing", true).group("Overlay"));

	public BlockOverlayModule() {
		super("block_overlay", "Block Overlay", Category.WORLD, Icons.CUBOID, "A translucent fill over the block you target");
	}

	@Override
	public boolean onBlockOutline(WorldRenderContext context, BlockOutlineRenderState outline) {
		BlockTarget.update(outline.shape().bounds().move(outline.pos()), true, 900f);
		Vec3 cam = context.worldState().cameraRenderState.pos;
		PoseStack.Pose pose = context.matrices().last();
		MultiBufferSource consumers = context.consumers();
		VertexConsumer vc = consumers.getBuffer(RenderTypes.debugFilledBox());
		int c = this.color.color();
		if (this.pulse.on()) {
			c = Colors.mulAlpha(c, 0.7f + 0.3f * (float) Math.sin(Clock.time() * Math.PI * 2.0 / 2.4));
		}
		float x0 = (float) (BlockTarget.minX() - cam.x) - INFLATE;
		float y0 = (float) (BlockTarget.minY() - cam.y) - INFLATE;
		float z0 = (float) (BlockTarget.minZ() - cam.z) - INFLATE;
		float x1 = (float) (BlockTarget.maxX() - cam.x) + INFLATE;
		float y1 = (float) (BlockTarget.maxY() - cam.y) + INFLATE;
		float z1 = (float) (BlockTarget.maxZ() - cam.z) + INFLATE;
		HitResult hit = Minecraft.getInstance().hitResult;
		if (this.faceOnly.on() && hit instanceof BlockHitResult bh) {
			Direction d = bh.getDirection();
			switch (d) {
				case DOWN -> WorldDraw.quad(vc, pose, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, c);
				case UP -> WorldDraw.quad(vc, pose, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, c);
				case NORTH -> WorldDraw.quad(vc, pose, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, c);
				case SOUTH -> WorldDraw.quad(vc, pose, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, c);
				case WEST -> WorldDraw.quad(vc, pose, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, c);
				case EAST -> WorldDraw.quad(vc, pose, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, c);
			}
		} else {
			WorldDraw.filledBox(vc, pose, x0, y0, z0, x1, y1, z1, c);
		}
		if (consumers instanceof MultiBufferSource.BufferSource source) {
			source.endLastBatch();
		}
		return true;
	}
}
