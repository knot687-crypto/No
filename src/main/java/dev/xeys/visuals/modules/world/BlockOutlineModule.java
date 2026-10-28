package dev.xeys.visuals.modules.world;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.world.WorldDraw;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import dev.xeys.visuals.world.BlockTarget;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** A custom block selection outline: any color, thickness and glow, full edges or corners, gliding between blocks. */
public final class BlockOutlineModule extends Module implements WorldEvents.BlockOutline {
	public enum Style {
		FULL,
		CORNERS
	}

	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFFFFFFF, ColorSetting.Mode.ACCENT).group("Outline"));
	private final NumberSetting thickness = this.add(new NumberSetting("thickness", "Thickness", 2.5f, 1f, 6f, 0.25f).unit("px").decimals(2)
		.describe("At 1080p; scales with resolution").group("Outline"));
	private final EnumSetting<Style> style = this.add(new EnumSetting<>("style", "Style", Style.FULL).group("Outline"));
	private final BoolSetting glow = this.add(new BoolSetting("glow", "Glow", true).group("Outline"));
	private final NumberSetting glowStrength = this.add(new NumberSetting("glow_strength", "Glow Strength", 35f, 5f, 100f, 1f).unit("%")
		.visibleWhen(this.glow::on).group("Outline"));
	private final BoolSetting glide = this.add(new BoolSetting("glide", "Glide", true).describe("The outline slides smoothly between blocks")
		.group("Motion"));

	public BlockOutlineModule() {
		super("block_outline", "Block Outline", Category.WORLD, Icons.BOX_SELECT, "A custom, gliding block selection outline");
	}

	@Override
	public boolean onBlockOutline(WorldRenderContext context, BlockOutlineRenderState outline) {
		BlockPos pos = outline.pos();
		BlockTarget.update(outline.shape().bounds().move(pos), this.glide.on(), 900f);
		Vec3 cam = context.worldState().cameraRenderState.pos;
		PoseStack.Pose pose = context.matrices().last();
		MultiBufferSource consumers = context.consumers();
		VertexConsumer vc = consumers.getBuffer(RenderTypes.lines());
		float scale = Minecraft.getInstance().getWindow().getHeight() / 1080f;
		float width = Math.max(1f, this.thickness.value() * scale);
		int c = this.color.color();
		float corners = this.style.is(Style.CORNERS) ? 0.24f : 0f;
		boolean box = corners > 0f || BlockTarget.moving();
		if (this.glow.on()) {
			int g = Colors.mulAlpha(c, this.glowStrength.value() / 100f * 0.45f);
			this.draw(vc, pose, outline, cam, box, corners, g, width * 3.2f);
		}
		this.draw(vc, pose, outline, cam, box, corners, c, width);
		if (consumers instanceof MultiBufferSource.BufferSource source) {
			source.endLastBatch();
		}
		return false;
	}

	private void draw(VertexConsumer vc, PoseStack.Pose pose, BlockOutlineRenderState outline, Vec3 cam, boolean box, float corners, int color,
					  float width) {
		if (box) {
			WorldDraw.box(vc, pose, (float) (BlockTarget.minX() - cam.x), (float) (BlockTarget.minY() - cam.y), (float) (BlockTarget.minZ() - cam.z),
				(float) (BlockTarget.maxX() - cam.x), (float) (BlockTarget.maxY() - cam.y), (float) (BlockTarget.maxZ() - cam.z), color, width, corners);
		} else {
			BlockPos pos = outline.pos();
			WorldDraw.shape(vc, pose, outline.shape(), pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z, color, width);
		}
	}
}
