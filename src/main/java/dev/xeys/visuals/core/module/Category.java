package dev.xeys.visuals.core.module;

import dev.xeys.visuals.ui.Icons;

public enum Category {
	HUD("HUD", Icons.LAYOUT_DASHBOARD, "Glass overlays and live information"),
	SCREEN("Screen", Icons.APERTURE, "2D screen effects, menus and transitions"),
	WORLD("World", Icons.MOUNTAIN, "Sky, lighting, particles and world effects"),
	COSMETICS("Cosmetics", Icons.SPARKLES, "Capes, wings, auras and indicators"),
	ANIMATIONS("Animations", Icons.ORBIT, "Item swing, view model and camera motion"),
	CLIENT("Client", Icons.SETTINGS, "Theme, interface and client preferences");

	private final String title;
	private final char icon;
	private final String subtitle;

	Category(String title, char icon, String subtitle) {
		this.title = title;
		this.icon = icon;
		this.subtitle = subtitle;
	}

	public String title() {
		return this.title;
	}

	public char icon() {
		return this.icon;
	}

	public String subtitle() {
		return this.subtitle;
	}
}
