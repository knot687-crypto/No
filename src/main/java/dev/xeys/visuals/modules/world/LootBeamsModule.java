package dev.xeys.visuals.modules.world;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.world.WorldGlow;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.Vec3;

/** Columns of light rise from dropped items, colored by rarity, so loot is easy to spot. */
public final class LootBeamsModule extends Module implements WorldEvents.EndMain {
	public enum MinRarity {
		ALL,
		UNCOMMON,
		RARE,
		EPIC
	}

	public enum ColorMode {
		RARITY,
		CUSTOM
	}

	private static final int MAX = 160;

	private final EnumSetting<MinRarity> minRarity = this.add(new EnumSetting<>("min_rarity", "Show For", MinRarity.UNCOMMON)
		.describe("Lowest rarity that gets a beam; enchanted items count as rare").group("Beams"));
	private final EnumSetting<ColorMode> colorMode = this.add(new EnumSetting<>("color_mode", "Color", ColorMode.RARITY).group("Beams"));
	private final ColorSetting color = this.add(new ColorSetting("color", "Custom Color", 0xFFFFFFFF, ColorSetting.Mode.ACCENT).noAlpha()
		.visibleWhen(() -> this.colorMode.is(ColorMode.CUSTOM)).group("Beams"));
	private final NumberSetting height = this.add(new NumberSetting("height", "Height", 2.5f, 1f, 8f, 0.25f).unit(" blocks").decimals(2).group("Beams"));
	private final NumberSetting width = this.add(new NumberSetting("width", "Width", 100f, 40f, 250f, 5f).unit("%").group("Beams"));
	private final NumberSetting opacity = this.add(new NumberSetting("opacity", "Opacity", 70f, 10f, 100f, 5f).unit("%").group("Beams"));
	private final NumberSetting range = this.add(new NumberSetting("range", "Range", 48f, 8f, 128f, 4f).unit(" blocks").group("Beams"));
	private final BoolSetting baseGlow = this.add(new BoolSetting("base_glow", "Base Glow", true).group("Beams"));
	private final float[] bx = new float[MAX];
	private final float[] by = new float[MAX];
	private final float[] bz = new float[MAX];
	private final int[] bc = new int[MAX];

	public LootBeamsModule() {
		super("loot_beams", "Loot Beams", Category.WORLD, Icons.GEM, "Light beams over dropped items, colored by rarity");
	}

	private static int rarityColor(Rarity rarity) {
		return switch (rarity) {
			case COMMON -> 0xFFFFFFFF;
			case UNCOMMON -> 0xFFFFE066;
			case RARE -> 0xFF5CE6FF;
			case EPIC -> 0xFFD58CFF;
		};
	}

	private boolean shown(Rarity rarity) {
		return switch (this.minRarity.get()) {
			case ALL -> true;
			case UNCOMMON -> rarity != Rarity.COMMON;
			case RARE -> rarity == Rarity.RARE || rarity == Rarity.EPIC;
			case EPIC -> rarity == Rarity.EPIC;
		};
	}

	@Override
	public void onEndMain(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null || !WorldGlow.beamsUsable()) {
			return;
		}
		Vec3 cam = context.worldState().cameraRenderState.pos;
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		double r = this.range.value();
		float alphaBase = this.opacity.value() / 100f;
		int n = 0;
		for (Entity e : level.entitiesForRendering()) {
			if (!(e instanceof ItemEntity item) || n >= MAX) {
				continue;
			}
			ItemStack stack = item.getItem();
			Rarity rarity = stack.getRarity();
			if (stack.isEmpty() || !this.shown(rarity)) {
				continue;
			}
			double x = Mth.lerp(partial, item.xOld, item.getX()) - cam.x;
			double y = Mth.lerp(partial, item.yOld, item.getY()) - cam.y;
			double z = Mth.lerp(partial, item.zOld, item.getZ()) - cam.z;
			double dist = Math.sqrt(x * x + y * y + z * z);
			if (dist > r) {
				continue;
			}
			float fade = (1f - smooth((float) ((dist - r * 0.7) / (r * 0.3)))) * Math.min(1f, (item.getAge() + partial) / 10f);
			int rgb = this.colorMode.is(ColorMode.CUSTOM) ? this.color.color() : rarityColor(rarity);
			this.bx[n] = (float) x;
			this.by[n] = (float) y;
			this.bz[n] = (float) z;
			this.bc[n] = Colors.withAlpha(rgb, Math.round(255f * fade * alphaBase));
			n++;
		}
		if (n == 0) {
			return;
		}
		PoseStack.Pose pose = context.matrices().last();
		float h = this.height.value();
		float w = 0.16f * this.width.value() / 100f;
		VertexConsumer beams = context.consumers().getBuffer(WorldGlow.BEAMS);
		for (int i = 0; i < n; i++) {
			WorldGlow.beam(beams, pose, this.bx[i], this.by[i] + 0.05f, this.bz[i], h, w, this.bc[i]);
		}
		if (this.baseGlow.on() && WorldGlow.dotsUsable()) {
			WorldGlow.face(mc.gameRenderer.getMainCamera());
			VertexConsumer dots = context.consumers().getBuffer(WorldGlow.DOTS);
			for (int i = 0; i < n; i++) {
				WorldGlow.dot(dots, pose, this.bx[i], this.by[i] + 0.2f, this.bz[i], 0.55f * this.width.value() / 100f,
					Colors.mulAlpha(this.bc[i], 0.45f));
			}
		}
	}

	private static float smooth(float x) {
		float c = Math.max(0f, Math.min(1f, x));
		return c * c * (3f - 2f * c);
	}
}
