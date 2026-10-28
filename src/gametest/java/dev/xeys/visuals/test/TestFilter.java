package dev.xeys.visuals.test;

import java.util.Arrays;

/** Selects test groups with -Dxeys.tests=hud,gui (default: all). */
final class TestFilter {
	private TestFilter() {
	}

	static boolean enabled(String group) {
		String spec = System.getProperty("xeys.tests", "all");
		return spec.equals("all") || Arrays.asList(spec.split(",")).contains(group);
	}
}
