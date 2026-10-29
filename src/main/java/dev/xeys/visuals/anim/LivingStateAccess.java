package dev.xeys.visuals.anim;

/** Extra per-entity render state, added to vanilla's living render state. */
public interface LivingStateAccess {
	/** 0..1 through the hurt flash (1 just hit), 0 when not hurt. */
	float xeys$hurt();

	void xeys$setHurt(float hurt);
}
