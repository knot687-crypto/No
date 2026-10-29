package dev.xeys.visuals.modules.world;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.anim.Easing;
import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.mixin.MultiPlayerGameModeAccessor;
import dev.xeys.visuals.render.world.WorldDraw;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Shows how far a block is from breaking: a box growing inside it, or filling it from the bottom. */
public final class BreakProgressModule extends Module implements Events.FrameStart, WorldEvents.BlockOutline {
	public enum Style {
		GROW,
		FILL
	}

	private final EnumSetting<Style> style = this.add(new EnumSetting<>("style", "Style", Style.GROW).group("Progress"));
	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0x66FFFFFF, ColorSetting.Mode.ACCENT).group("Progress"));
	private final BoolSetting edges = this.add(new BoolSetting("edges", "Edges", true).group("Progress"));
	private final BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
	private float shown;

	public BreakProgressModule() {
		super("break_progress", "Break Progress", Category.WORLD, Icons.PICKAXE, "See how close a block is to breaking");
	}

	@Override
	public void onFrameStart() {
		MultiPlayerGameMode gm = Minecraft.getInstance().gameMode;
		if (gm == null) {
			this.shown = 0f;
			return;
		}
		MultiPlayerGameModeAccessor acc = (MultiPlayerGameModeAccessor) gm;
		float progress = acc.xeys$isDestroying() ? acc.xeys$destroyProgress() : 0f;
		if (acc.xeys$isDestroying() && !acc.xeys$destroyBlockPos().equals(this.target)) {
			this.target.set(acc.xeys$destroyBlockPos());
			this.shown = 0f;
		}
		// Progress arrives in 20 Hz steps; ease toward it, but drop at once when breaking stops.
		this.shown = progress < this.shown ? progress : this.shown + (progress - this.shown) * Clock.smoothing(20f);
	}

	@Override
	public boolean onBlockOutline(WorldRenderContext context, BlockOutlineRenderState outline) {
		if (this.shown < 0.004f || !outline.pos().equals(this.target)) {
			return true;
		}
		BlockPos pos = outline.pos();
		AABB b = outline.shape().isEmpty() ? new AABB(0, 0, 0, 1, 1, 1) : outline.shape().bounds();
		Vec3 cam = context.worldState().cameraRenderState.pos;
		// Just outside the block's faces, so the fill is visible and never fights them.
		double out = 0.004;
		float x0 = (float) (pos.getX() - cam.x + b.minX - out);
		float y0 = (float) (pos.getY() - cam.y + b.minY - out);
		float z0 = (float) (pos.getZ() - cam.z + b.minZ - out);
		float x1 = (float) (pos.getX() - cam.x + b.maxX + out);
		float y1 = (float) (pos.getY() - cam.y + b.maxY + out);
		float z1 = (float) (pos.getZ() - cam.z + b.maxZ + out);
		float p = Easing.OUT_CUBIC.apply(Math.min(1f, this.shown));
		int base = this.color.color();
		// Brightens toward white as the block is about to give.
		int c = Colors.withAlpha(Colors.lerp(base, 0xFFFFFFFF, p * p * 0.6f), Colors.alpha(base));
		PoseStack.Pose pose = context.matrices().last();
		MultiBufferSource consumers = context.consumers();
		VertexConsumer fill = consumers.getBuffer(RenderTypes.debugFilledBox());
		if (this.style.is(Style.GROW)) {
			// A square on every face, growing from the face's center.
			float hx = (x1 - x0) * 0.5f * p;
			float hy = (y1 - y0) * 0.5f * p;
			float hz = (z1 - z0) * 0.5f * p;
			float cx = (x0 + x1) * 0.5f;
			float cy = (y0 + y1) * 0.5f;
			float cz = (z0 + z1) * 0.5f;
			WorldDraw.quad(fill, pose, cx - hx, y0, cz - hz, cx + hx, y0, cz - hz, cx + hx, y0, cz + hz, cx - hx, y0, cz + hz, c);
			WorldDraw.quad(fill, pose, cx - hx, y1, cz - hz, cx - hx, y1, cz + hz, cx + hx, y1, cz + hz, cx + hx, y1, cz - hz, c);
			WorldDraw.quad(fill, pose, cx - hx, cy - hy, z0, cx - hx, cy + hy, z0, cx + hx, cy + hy, z0, cx + hx, cy - hy, z0, c);
			WorldDraw.quad(fill, pose, cx - hx, cy - hy, z1, cx + hx, cy - hy, z1, cx + hx, cy + hy, z1, cx - hx, cy + hy, z1, c);
			WorldDraw.quad(fill, pose, x0, cy - hy, cz - hz, x0, cy - hy, cz + hz, x0, cy + hy, cz + hz, x0, cy + hy, cz - hz, c);
			WorldDraw.quad(fill, pose, x1, cy - hy, cz - hz, x1, cy + hy, cz - hz, x1, cy + hy, cz + hz, x1, cy - hy, cz + hz, c);
		} else {
			// The block filling up from the bottom.
			WorldDraw.filledBox(fill, pose, x0, y0, z0, x1, y0 + (y1 - y0) * p, z1, c);
		}
		if (this.edges.on()) {
			VertexConsumer lines = consumers.getBuffer(RenderTypes.lines());
			float width = Math.max(1f, 2f * Minecraft.getInstance().getWindow().getHeight() / 1080f);
			int edge = Colors.withAlpha(c, 0.9f);
			if (this.style.is(Style.GROW)) {
				WorldDraw.box(lines, pose, x0, y0, z0, x1, y1, z1, edge, width, 0.5f * p);
			} else {
				WorldDraw.box(lines, pose, x0, y0, z0, x1, y0 + (y1 - y0) * p, z1, edge, width, 0f);
			}
		}
		if (consumers instanceof MultiBufferSource.BufferSource source) {
			source.endLastBatch();
		}
		return true;
	}
}
