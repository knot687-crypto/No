package dev.xeys.visuals.core;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/** When the current world or server session started. */
public final class Session {
	private static long joinedAt = System.currentTimeMillis();

	private Session() {
	}

	public static void init() {
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> joinedAt = System.currentTimeMillis());
	}

	public static long seconds() {
		return Math.max(0L, (System.currentTimeMillis() - joinedAt) / 1000L);
	}
}
