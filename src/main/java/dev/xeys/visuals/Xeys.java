package dev.xeys.visuals;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Xeys {
	public static final String MOD_ID = "xeys";
	public static final String NAME = "XEYS VISUALS";
	public static final Logger LOG = LoggerFactory.getLogger(NAME);

	private Xeys() {
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
