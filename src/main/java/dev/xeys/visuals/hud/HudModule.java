package dev.xeys.visuals.hud;

import dev.xeys.visuals.anim.Spring;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A movable, scalable HUD element. Each frame the renderer calls {@link #layout} (compute size, refresh
 * cached text) and then {@link #draw} at the local origin; splitting the two means right- and bottom-
 * anchored elements are positioned with this frame's size and never jitter when their content changes.
 */
public abstract class HudModule extends Module {
	public final HudPosition position;
	protected float width = 40f;
	protected float height = 16f;
	private boolean laidOut;
	private final Spring frameX = new Spring(0f, 300f, 1f);
	private final Spring frameY = new Spring(0f, 300f, 1f);
	private boolean frameValid;

	protected HudModule(String id, String name, char icon, String description, HudPosition.Anchor anchor, float x, float y) {
		super(id, name, Category.HUD, icon, description);
		this.position = new HudPosition(anchor, x, y);
	}

	/** Updates {@link #width}/{@link #height} (unscaled GUI pixels) for this frame. */
	protected abstract void layout(boolean preview);

	/** Draws within (0, 0, width, height). {@code preview} is true in the HUD editor / outside a world. */
	protected abstract void draw(GuiGraphics g, float partialTick, boolean preview);

	/** Whether the element has anything to show right now (e.g. Potion HUD with no effects). */
	public boolean hasContent(boolean preview) {
		return true;
	}

	public final void layoutFrame(boolean preview) {
		this.layout(preview);
		this.laidOut = true;
	}

	/**
	 * Where the element sits this frame (GUI px, top-left), decided by {@link HudLayout}. Elements in the
	 * automatic layout glide to new spots; manually placed ones (and newly shown ones) move instantly.
	 */
	final void placeFrame(float left, float top, boolean immediate) {
		if (immediate || !this.frameValid) {
			this.frameX.snap(left);
			this.frameY.snap(top);
			this.frameValid = true;
		} else {
			this.frameX.target(left);
			this.frameY.target(top);
		}
	}

	/** Forgets the frame position so the element appears in place (no glide) next time it is shown. */
	final void invalidateFrame() {
		this.frameValid = false;
	}

	/** Left edge this frame, on the physical pixel grid so items and hairlines stay crisp. */
	public final float frameLeft() {
		return snapToPixel(this.frameX.get());
	}

	public final float frameTop() {
		return snapToPixel(this.frameY.get());
	}

	private static float snapToPixel(float v) {
		float scale = Minecraft.getInstance().getWindow().getGuiScale();
		return Math.round(v * scale) / scale;
	}

	/** Whether {@link #width()} and {@link #height()} come from a real layout yet. */
	public final boolean laidOut() {
		return this.laidOut;
	}

	public final void drawFrame(GuiGraphics g, float partialTick, boolean preview) {
		this.draw(g, partialTick, preview);
	}

	public float width() {
		return this.width;
	}

	public float height() {
		return this.height;
	}

	public float scaledWidth() {
		return this.width * this.position.scale();
	}

	public float scaledHeight() {
		return this.height * this.position.scale();
	}
}
