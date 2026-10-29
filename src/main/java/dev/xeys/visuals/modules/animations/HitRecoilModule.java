package dev.xeys.visuals.modules.animations;

import dev.xeys.visuals.anim.CameraMotion;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.combat.HitTracker;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;

/** A small upward kick of the camera each time one of your hits lands, stronger on criticals. */
public final class HitRecoilModule extends Module {
	private final NumberSetting kick = this.add(new NumberSetting("kick", "Kick", 1f, 0.25f, 4f, 0.25f).unit("°").decimals(2).group("Recoil"));
	private double kickedAt = -1.0;

	public HitRecoilModule() {
		super("hit_recoil", "Hit Recoil", Category.ANIMATIONS, Icons.TARGET, "The camera kicks when your hits land");
		HitTracker.listen((target, kind) -> {
			// A kill can be reported just after its hit, which already kicked.
			if (!this.isEnabled() || kind == HitTracker.Kind.KILL && Clock.time() - this.kickedAt < 1.5) {
				return;
			}
			this.kickedAt = Clock.time();
			CameraMotion.recoil(this.kick.value() * (kind == HitTracker.Kind.HIT ? 1f : 1.6f));
		});
	}
}
