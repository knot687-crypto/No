package dev.xeys.visuals.core.combat;

import dev.xeys.visuals.anim.Clock;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * Turns "you swung at something" into "your hit landed": after an attack, it waits for the game to
 * confirm the target was hurt (or killed) and then tells listeners. Shared by hit markers and particles.
 */
public final class HitTracker {
	public enum Kind {
		HIT,
		CRITICAL,
		KILL
	}

	public interface Listener {
		void onHit(LivingEntity target, Kind kind);
	}

	private static final CopyOnWriteArrayList<Listener> LISTENERS = new CopyOnWriteArrayList<>();
	private static @Nullable LivingEntity pending;
	private static double pendingAt;
	private static int pendingHurt;
	private static boolean pendingCrit;
	private static @Nullable LivingEntity confirmed;
	private static double confirmedAt;
	private static boolean upgraded;

	private HitTracker() {
	}

	public static void listen(Listener listener) {
		LISTENERS.addIfAbsent(listener);
	}

	public static void onAttack(Player player, Entity target) {
		if (!(target instanceof LivingEntity living) || LISTENERS.isEmpty()) {
			return;
		}
		pending = living;
		pendingAt = Clock.time();
		pendingHurt = living.hurtTime;
		pendingCrit = player.fallDistance > 0.0 && !player.onGround() && !player.onClimbable() && !player.isInWater()
			&& !player.hasEffect(MobEffects.BLINDNESS) && !player.isPassenger() && !player.isSprinting();
	}

	/** Called once per frame. */
	public static void frame() {
		LivingEntity p = pending;
		if (p != null) {
			if (Clock.time() - pendingAt > 0.6) {
				pending = null;
			} else if (p.hurtTime > pendingHurt || p.isDeadOrDying()) {
				Kind kind = p.isDeadOrDying() ? Kind.KILL : (pendingCrit ? Kind.CRITICAL : Kind.HIT);
				pending = null;
				confirmed = p;
				confirmedAt = Clock.time();
				upgraded = kind == Kind.KILL;
				fire(p, kind);
			}
		}
		// A hit that turns out to be the kill a moment later is reported again as a kill.
		LivingEntity c = confirmed;
		if (c != null && !upgraded && c.isDeadOrDying() && Clock.time() - confirmedAt < 0.3) {
			upgraded = true;
			fire(c, Kind.KILL);
		}
	}

	private static void fire(LivingEntity target, Kind kind) {
		for (Listener l : LISTENERS) {
			l.onHit(target, kind);
		}
	}
}
