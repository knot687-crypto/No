package dev.xeys.visuals.modules.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.xeys.visuals.anim.Anim;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.anim.Easing;
import dev.xeys.visuals.anim.Spring;
import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.hud.HudItems;
import dev.xeys.visuals.hud.HudModule;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.HudStyle;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import dev.xeys.visuals.util.Fmt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix3x2f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** The entity you are looking at: portrait, name, animated health bar with a damage trail, distance and armor. */
public final class TargetHudModule extends HudModule implements Events.ClientTick {
	public enum Portrait {
		FACE,
		MODEL,
		NONE
	}

	public enum HealthFormat {
		VALUE,
		PERCENT,
		HEARTS
	}

	private final EnumSetting<Portrait> portrait = this.add(new EnumSetting<>("portrait", "Portrait", Portrait.FACE)
		.describe("Player faces from their skin, or the full 3D model").group("Display"));
	private final EnumSetting<HealthFormat> format = this.add(new EnumSetting<>("health_format", "Health Format", HealthFormat.VALUE)
		.group("Display"));
	private final NumberSetting panelWidth = this.add(new NumberSetting("width", "Width", 156f, 130f, 240f, 2f).unit("px").group("Display"));
	private final BoolSetting distance = this.add(new BoolSetting("distance", "Distance", true).group("Display"));
	private final BoolSetting armor = this.add(new BoolSetting("armor", "Armor Points", true).group("Display"));
	private final BoolSetting healthColors = this.add(new BoolSetting("health_colors", "Health Colors", true)
		.describe("Green to red as health drops; accent when off").group("Colors"));
	private final BoolSetting trail = this.add(new BoolSetting("trail", "Damage Trail", true)
		.describe("A lighter bar lingers where health was, then catches up").group("Animation"));
	private final BoolSetting shake = this.add(new BoolSetting("shake", "Hit Shake", true).describe("The portrait jolts when the target is hurt")
		.group("Animation"));
	private final BoolSetting playersOnly = this.add(new BoolSetting("players_only", "Players Only", false).group("Targets"));
	private final NumberSetting keep = this.add(new NumberSetting("keep", "Linger", 2.5f, 0.5f, 8f, 0.5f).unit("s").decimals(1)
		.describe("How long the panel stays after you look away").group("Targets"));

	private LivingEntity target;
	private double lastSeen = -100.0;
	private String name = "";
	private double nameAt;
	private final Anim appear = new Anim(0f);
	private final Spring shown = new Spring(1f, 520f, 1f);
	private float trailValue = 1f;
	private double dropAt;
	private float lastHealth = -1f;
	private final Spring jolt = new Spring(0f, 900f, 0.32f);
	private final StringBuilder healthText = new StringBuilder(12);
	private final StringBuilder info = new StringBuilder(16);
	private final StringBuilder armorText = new StringBuilder(4);
	private final Quaternionf modelRotation = new Quaternionf();
	private final Quaternionf cameraRotation = new Quaternionf();
	private final Vector3f modelOffset = new Vector3f();

	// Values for this frame, from the live target or the preview stand-in.
	private LivingEntity frameEntity;
	private float health;
	private float maxHealth;
	private float absorption;
	private int armorValue;
	private int hurtTime;
	private float dist;

	public TargetHudModule() {
		super("target_hud", "Target HUD", Icons.TARGET, "Health and details of the entity you are looking at", HudPosition.Anchor.CENTER, 0f, 70f);
	}

	@Override
	public void onClientTick() {
		Minecraft mc = Minecraft.getInstance();
		if (this.target != null && (mc.level == null || this.target.level() != mc.level)) {
			// Never hold on to an entity from a world we have left.
			this.target = null;
			this.lastSeen = -100.0;
		}
		Entity e = mc.crosshairPickEntity;
		if (e instanceof LivingEntity le && le.isAlive() && mc.player != null && !le.isInvisibleTo(mc.player)
			&& (!this.playersOnly.on() || le instanceof Player)) {
			if (le != this.target) {
				this.target = le;
				this.nameAt = -10.0;
				this.lastHealth = -1f;
			}
			this.lastSeen = Clock.time();
		}
		if (this.target != null && this.target.isRemoved()) {
			this.lastSeen = Math.min(this.lastSeen, Clock.time() - this.keep.value() + 0.4);
		}
	}

	@Override
	protected void onDisable() {
		this.target = null;
		this.appear.snap(0f);
	}

	private boolean live() {
		return this.target != null && Clock.time() - this.lastSeen < this.keep.value();
	}

	@Override
	public boolean hasContent(boolean preview) {
		if (!preview && !this.live() && this.appear.get() <= 0f) {
			this.target = null;
			return false;
		}
		return true;
	}

	@Override
	protected void layout(boolean preview) {
		boolean visible = preview || this.live();
		if (visible && this.appear.target() < 1f) {
			this.appear.to(1f, 0.32f, Easing.OUT_CUBIC);
		} else if (!visible && this.appear.target() > 0f) {
			this.appear.to(0f, 0.3f, Easing.IN_OUT_CUBIC);
		}
		Minecraft mc = Minecraft.getInstance();
		// Live target, else the last one while fading out; previews without a target use you as a stand-in.
		LivingEntity e = this.live() || !preview ? this.target : null;
		if (e == null && preview) {
			e = mc.player;
		}
		this.frameEntity = e;
		if (e != null && e != this.target) {
			// Preview stand-in: you, with health gently cycling so the bar animations are visible.
			this.maxHealth = 20f;
			this.health = 8f + 12f * (float) (0.5 + 0.5 * Math.cos(Clock.time() * 0.9));
			this.health = Math.round(this.health * 2f) / 2f;
			this.absorption = 0f;
			this.armorValue = e.getArmorValue();
			this.hurtTime = 0;
			this.dist = 3.4f;
		} else if (e != null) {
			this.health = e.getHealth();
			this.maxHealth = Math.max(e.getMaxHealth(), 1f);
			this.absorption = e.getAbsorptionAmount();
			this.armorValue = e.getArmorValue();
			this.hurtTime = e.hurtTime;
			this.dist = mc.player != null ? mc.player.distanceTo(e) : 0f;
		} else {
			this.health = 14f;
			this.maxHealth = 20f;
			this.absorption = 0f;
			this.armorValue = 12;
			this.hurtTime = 0;
			this.dist = 3.4f;
		}
		if (Clock.time() - this.nameAt > 2.0 || this.name.isEmpty()) {
			this.nameAt = Clock.time();
			this.name = e != null ? e.getDisplayName().getString() : "Steve";
		}

		float frac = Math.max(0f, Math.min(this.health / this.maxHealth, 1f));
		if (this.lastHealth >= 0f && this.health < this.lastHealth - 0.01f) {
			this.dropAt = Clock.time();
			if (this.shake.on()) {
				this.jolt.impulse(60f);
			}
		}
		if (this.lastHealth < 0f) {
			this.shown.snap(frac);
			this.trailValue = frac;
		}
		this.lastHealth = this.health;
		this.shown.target(frac);
		if (frac >= this.trailValue) {
			this.trailValue = frac;
		} else if (Clock.time() - this.dropAt > 0.45) {
			this.trailValue += (frac - this.trailValue) * Clock.smoothing(5f);
		}

		this.healthText.setLength(0);
		switch (this.format.get()) {
			case VALUE -> Fmt.fixed(this.healthText, this.health + this.absorption, this.health % 1f == 0f && this.absorption % 1f == 0f ? 0 : 1);
			case PERCENT -> this.healthText.append(Math.round(frac * 100f)).append('%');
			case HEARTS -> Fmt.fixed(this.healthText, (this.health + this.absorption) / 2f, 1);
		}
		this.info.setLength(0);
		if (this.distance.on()) {
			Fmt.fixed(this.info, this.dist, 1).append(" m");
		}
		this.armorText.setLength(0);
		this.armorText.append(this.armorValue);

		this.width = this.panelWidth.value();
		this.height = this.portrait.is(Portrait.NONE) ? HudStyle.textSize() * 3.2f + 16f : 46f;
	}

	private int healthColor(float frac) {
		return this.healthColors.on() ? HudItems.durabilityColor(frac) : HudStyle.accent();
	}

	@Override
	protected void draw(GuiGraphics g, float partialTick, boolean preview) {
		float a = Easing.OUT_CUBIC.apply(this.appear.get());
		if (a <= 0.001f) {
			return;
		}
		float w = this.width;
		float h = this.height;
		float k = 0.94f + 0.06f * a;
		g.pose().pushMatrix();
		g.pose().translate(w * 0.5f, h * 0.5f);
		g.pose().scale(k, k);
		g.pose().translate(-w * 0.5f, -h * 0.5f);

		HudStyle.panel(g, 0f, 0f, w, h);
		float pad = 6f;
		float size = HudStyle.textSize();
		float x = pad;
		if (!this.portrait.is(Portrait.NONE)) {
			float ps = h - pad * 2f;
			float jx = this.jolt.get();
			this.drawPortrait(g, x + jx, pad, ps, a, partialTick);
			x += ps + 8f;
		}
		float right = w - pad - 2f;
		int text = Colors.mulAlpha(HudStyle.text(), a);
		int muted = Colors.mulAlpha(HudStyle.textSecondary(), a);
		float frac = Math.max(0f, Math.min(this.health / this.maxHealth, 1f));
		int hc = this.healthColor(frac);

		// Name and health readout.
		float nameY = pad + 1f + size * 0.5f + (this.portrait.is(Portrait.NONE) ? 1f : 1.5f);
		float hw = HudStyle.fontStrong().width(this.healthText, size);
		float iconW = this.format.is(HealthFormat.PERCENT) ? 0f : size + 2f;
		Text.drawTruncated(g, HudStyle.fontStrong(), this.name, x, nameY, size, text, right - x - hw - iconW - 8f);
		HudStyle.label(g, HudStyle.fontStrong(), this.healthText, right - iconW, nameY, size, Colors.mulAlpha(hc, a), Text.Align.RIGHT);
		if (iconW > 0f) {
			Text.icon(g, Icons.HEART, right - size * 0.5f, nameY, size * 0.95f, Colors.mulAlpha(hc, a));
		}

		// Health bar: track, lingering trail, fill with a soft top highlight, absorption on top.
		float by = nameY + size * 0.5f + 5f;
		float bh = 5f;
		float bw = right - x;
		Draw.rect(g, x, by, bw, bh, bh * 0.5f, Colors.withAlpha(0xFFFFFF, 0.08f * a));
		float shown = Math.max(0f, Math.min(this.shown.get(), 1f));
		if (this.trail.on() && this.trailValue > shown + 0.002f) {
			Draw.rect(g, x, by, bw * this.trailValue, bh, bh * 0.5f, Colors.withAlpha(0xFFFFFF, 0.45f * a));
		}
		if (shown > 0.002f) {
			float fw = Math.max(bh, bw * shown);
			Draw.rect(g, x, by, fw, bh).radius(bh * 0.5f).gradientH(Colors.mulAlpha(Colors.shade(hc, -0.18f), a), Colors.mulAlpha(hc, a)).submit();
			Draw.rect(g, x + 1f, by + 0.5f, fw - 2f, 1f, 0.5f, Colors.withAlpha(0xFFFFFF, 0.22f * a));
		}
		if (this.absorption > 0f) {
			float ab = Math.min(this.absorption / this.maxHealth, 1f);
			Draw.rect(g, x, by + bh + 1.5f, bw * ab, 1.5f, 0.75f, Colors.mulAlpha(0xFFFBBF24, a));
		}

		// Distance and armor.
		float iy = by + bh + 5f + size * 0.42f;
		float ix = x;
		if (!this.info.isEmpty()) {
			ix += HudStyle.label(g, HudStyle.font(), this.info, ix, iy, size * 0.86f, muted, Text.Align.LEFT) + 10f;
		}
		if (this.armor.on() && this.armorValue > 0) {
			Text.icon(g, Icons.SHIELD, ix + size * 0.42f, iy, size * 0.84f, muted);
			HudStyle.label(g, HudStyle.font(), this.armorText, ix + size + 1f, iy, size * 0.86f, muted, Text.Align.LEFT);
		}
		g.pose().popMatrix();
	}

	private void drawPortrait(GuiGraphics g, float x, float y, float s, float a, float partialTick) {
		float r = Math.min(HudStyle.radius(), s * 0.3f);
		Draw.rect(g, x, y, s, s, r, Colors.withAlpha(0xFFFFFF, 0.05f * a));
		LivingEntity e = this.frameEntity;
		boolean face = this.portrait.is(Portrait.FACE) && (e == null || e instanceof AbstractClientPlayer);
		if (face) {
			Identifier skin = e instanceof AbstractClientPlayer p ? p.getSkin().body().texturePath() : DefaultPlayerSkin.getDefaultTexture();
			GpuTextureView view = Minecraft.getInstance().getTextureManager().getTexture(skin).getTextureView();
			var sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
			int tint = Colors.withAlpha(0xFFFFFF, a);
			Draw.image(g, view, sampler, x, y, s, s, 8f / 64f, 8f / 64f, 16f / 64f, 16f / 64f).radius(r).color(tint).submit();
			// Hat layer, a touch larger like the in-game model.
			float o = s * 0.04f;
			Draw.image(g, view, sampler, x - o, y - o, s + o * 2f, s + o * 2f, 40f / 64f, 8f / 64f, 48f / 64f, 16f / 64f).radius(r + o)
				.color(tint).submit();
		} else if (e != null) {
			this.drawModel(g, e, x, y, s, partialTick);
		}
		if (this.hurtTime > 0) {
			float t = (this.hurtTime - partialTick) / 10f;
			Draw.rect(g, x, y, s, s, r, Colors.withAlpha(Theme.DANGER, 0.45f * Math.max(t, 0f) * a));
		}
		Draw.outline(g, x, y, s, s, r, 0.75f, Colors.withAlpha(0xFFFFFF, 0.12f * a));
	}

	private void drawModel(GuiGraphics g, LivingEntity e, float x, float y, float s, float partialTick) {
		EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
		EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(e);
		EntityRenderState state = renderer.createRenderState(e, partialTick);
		state.lightCoords = 0xF000F0;
		state.shadowPieces.clear();
		state.outlineColor = 0;
		if (state instanceof LivingEntityRenderState living) {
			// A three-quarter view, turned toward the panel's text.
			living.bodyRot = 180f - 24f;
			living.yRot = -14f;
			living.xRot = living.pose == Pose.FALL_FLYING ? 0f : 4f;
			living.boundingBoxWidth /= living.scale;
			living.boundingBoxHeight /= living.scale;
			living.scale = 1f;
		}
		Matrix3x2f m = g.pose();
		float ax = m.m00 * x + m.m10 * y + m.m20;
		float ay = m.m01 * x + m.m11 * y + m.m21;
		float as = s * m.m00;
		int x0 = Math.round(ax);
		int y0 = Math.round(ay);
		int x1 = Math.round(ax + as);
		int y1 = Math.round(ay + as);
		float extent = Math.max(state.boundingBoxHeight, state.boundingBoxWidth * 1.3f);
		float scale = (y1 - y0) * 0.86f / Math.max(extent, 0.3f);
		this.modelOffset.set(0f, state.boundingBoxHeight * 0.5f, 0f);
		this.modelRotation.identity().rotateZ((float) Math.PI);
		this.cameraRotation.identity();
		g.submitEntityRenderState(state, scale, this.modelOffset, this.modelRotation, this.cameraRotation, x0, y0, x1, y1);
	}
}
