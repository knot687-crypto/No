package dev.xeys.visuals.test;

import java.util.Arrays;

/**
 * Selects test groups with -Dxeys.tests=hud,gui (default: all), and sections inside a group with
 * -Dxeys.sections=bloom,sky (default: all).
 */
final class TestFilter {
	private TestFilter() {
	}

	static boolean enabled(String group) {
		String spec = System.getProperty("xeys.tests", "all");
		return spec.equals("all") || Arrays.asList(spec.split(",")).contains(group);
	}

	static boolean section(String name) {
		String spec = System.getProperty("xeys.sections", "all");
		return spec.equals("all") || Arrays.asList(spec.split(",")).contains(name);
	}
}
