package dev.xeys.visuals.cosmetic;

/** Moves a cosmetic's pose onto a body part of the posed player model. */
public final class Anchors {
	private Anchors() {
	}

	/**
	 * Into head space (pivot at the neck, the face toward -z). Returns the y of the top of the head,
	 * raised above the skin's hat layer or a helmet so hats sit on top rather than inside.
	 */
	public static float head(CosmeticContext c) {
		c.model.head.translateAndRotate(c.pose);
		if (!c.state.headEquipment.isEmpty()) {
			return -9.4f / 16f;
		}
		return c.state.showHat ? -8.5f / 16f : -8f / 16f;
	}

	/** Into body space (pivot at the neck, the back toward +z, the waist at y = 12/16). */
	public static void body(CosmeticContext c) {
		c.model.body.translateAndRotate(c.pose);
	}

	/** How far the back sticks out behind the body, with a chestplate or the jacket layer. */
	public static float back(CosmeticContext c) {
		if (!c.state.chestEquipment.isEmpty()) {
			return 3.2f / 16f;
		}
		return c.state.showJacket ? 2.4f / 16f : 2f / 16f;
	}
}
