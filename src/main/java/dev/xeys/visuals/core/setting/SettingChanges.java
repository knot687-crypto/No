package dev.xeys.visuals.core.setting;

/** Global dirty flag consumed by the config autosave. */
public final class SettingChanges {
	private static volatile long version;

	private SettingChanges() {
	}

	public static void markDirty() {
		version++;
	}

	public static long version() {
		return version;
	}
}
