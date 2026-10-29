package dev.xeys.visuals.modules.cosmetics;

import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.cosmetic.NameTags;
import dev.xeys.visuals.ui.Icons;

/** Cleaner name tags: a darker glass backing in your color, and the size you like. */
public final class NametagsModule extends Module {
	private final ColorSetting backing = this.add(new ColorSetting("backing", "Backing", 0xB0060608).onChange(v -> this.apply()).group("Name Tags"));
	private final NumberSetting scale = this.add(new NumberSetting("scale", "Size", 100f, 50f, 200f, 5f).unit("%").onChange(v -> this.apply())
		.group("Name Tags"));

	public NametagsModule() {
		super("nametags", "Name Tags", Category.COSMETICS, Icons.TEXT, "Restyled name tags with a glass backing");
	}

	private void apply() {
		if (this.isEnabled()) {
			NameTags.backing = this.backing.color();
			NameTags.scale = this.scale.value() / 100f;
			NameTags.active = true;
		}
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		NameTags.active = false;
		NameTags.scale = 1f;
	}
}
