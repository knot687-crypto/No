package dev.xeys.visuals.ui.clickgui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.xeys.visuals.core.config.ConfigManager;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.ui.clickgui.control.Popover;
import dev.xeys.visuals.ui.core.TextField;
import dev.xeys.visuals.ui.core.UiSounds;
import dev.xeys.visuals.ui.notify.Notifications;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.jspecify.annotations.Nullable;

/** Profile switcher: select, create, rename, delete, export and import. */
final class ProfilesPopover extends Popover {
	private static final float ROW = 36f;
	private static final float FOOTER = 52f;
	private List<String> profiles;
	private @Nullable String renaming;
	private final TextField renameField = new TextField().textSize(12.5f).maxLength(32);
	private int hoveredRow = -1;
	private int hoveredAction = -1;
	private int hoveredFooter = -1;

	ProfilesPopover() {
		this.w = 300f;
		this.refresh();
		this.renameField.filter(c -> Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_').onSubmit(this::commitRename);
	}

	private void refresh() {
		this.profiles = ConfigManager.listProfiles();
		this.h = 46f + this.profiles.size() * ROW + FOOTER + 8f;
	}

	private void commitRename() {
		if (this.renaming != null) {
			String target = ConfigManager.sanitizeName(this.renameField.value());
			if (!target.equals(this.renaming) && ConfigManager.rename(this.renaming, target)) {
				Notifications.push("Profile renamed", target, Icons.PEN_LINE, Theme.accent());
			}
			this.renaming = null;
			this.refresh();
		}
	}

	@Override
	protected void drawContent(GuiGraphics g, float mx, float my, float a) {
		Text.drawMiddle(g, Fonts.SEMIBOLD, "Profiles", this.x + 16f, this.y + 24f, 13f, dim(Theme.TEXT, a), Text.Align.LEFT);
		Text.drawMiddle(g, Fonts.REGULAR, this.profiles.size() + (this.profiles.size() == 1 ? " profile" : " profiles"), this.x + this.w - 16f,
			this.y + 24f, 11f, dim(Theme.TEXT_MUTED, a), Text.Align.RIGHT);
		String active = ConfigManager.activeProfile();
		this.hoveredRow = -1;
		this.hoveredAction = -1;
		for (int i = 0; i < this.profiles.size(); i++) {
			String name = this.profiles.get(i);
			float ry = this.y + 42f + i * ROW;
			boolean hov = mx >= this.x + 6f && mx < this.x + this.w - 6f && my >= ry && my < ry + ROW;
			boolean act = name.equals(active);
			if (hov) {
				this.hoveredRow = i;
			}
			if (act || hov) {
				Draw.rect(g, this.x + 6f, ry, this.w - 12f, ROW, 9f, dim(act ? Theme.accentSoft() : 0x0DFFFFFF, a));
			}
			float cx = this.x + 22f;
			Draw.circleShape(g, cx, ry + ROW * 0.5f, 6f).color(0).border(1.5f, dim(act ? Theme.accent() : 0x40FFFFFF, a)).submit();
			if (act) {
				Draw.circle(g, cx, ry + ROW * 0.5f, 3f, dim(Theme.accent(), a));
			}
			if (name.equals(this.renaming)) {
				this.renameField.bounds(this.x + 36f, ry + 3f, this.w - 52f, ROW - 6f);
				this.renameField.setFocused(true);
				this.renameField.render(g, mx, my);
				continue;
			}
			Text.drawTruncated(g, Fonts.MEDIUM, name, this.x + 38f, ry + ROW * 0.5f, 12.5f, dim(act ? Theme.TEXT : Theme.TEXT_SECONDARY, a), this.w - 120f);
			if (hov) {
				for (int k = 0; k < 2; k++) {
					float ix = this.x + this.w - 26f - k * 26f;
					boolean ih = Math.abs(mx - ix) < 11f;
					if (ih) {
						this.hoveredAction = k;
					}
					int c = k == 0 ? (ih ? Theme.DANGER : Theme.TEXT_MUTED) : (ih ? Theme.TEXT : Theme.TEXT_MUTED);
					Text.icon(g, k == 0 ? Icons.TRASH_2 : Icons.PEN_LINE, ix, ry + ROW * 0.5f, 13f, dim(c, a));
				}
			}
		}
		float fy = this.y + this.h - FOOTER;
		Draw.rect(g, this.x + 12f, fy, this.w - 24f, 1f).color(dim(Theme.HAIRLINE, a)).submit();
		String[] labels = {"New", "Export", "Import"};
		char[] icons = {Icons.PLUS, Icons.UPLOAD, Icons.DOWNLOAD};
		float bw = (this.w - 24f - 16f) / 3f;
		this.hoveredFooter = -1;
		for (int i = 0; i < 3; i++) {
			float bx = this.x + 12f + i * (bw + 8f);
			float by = fy + 12f;
			boolean hov = mx >= bx && mx < bx + bw && my >= by && my < by + 30f;
			if (hov) {
				this.hoveredFooter = i;
			}
			Draw.rect(g, bx, by, bw, 30f).radius(9f).color(dim(hov ? 0xFF24242B : 0xFF18181D, a)).border(1f, dim(hov ? Theme.accent(0.45f) : 0x14FFFFFF, a)).submit();
			float tw = Fonts.SEMIBOLD.width(labels[i], 11.5f) + 18f;
			float tx = bx + (bw - tw) * 0.5f;
			Text.icon(g, icons[i], tx + 6f, by + 15f, 12f, dim(hov ? Theme.accent() : Theme.TEXT_SECONDARY, a));
			Text.drawMiddle(g, Fonts.SEMIBOLD, labels[i], tx + 18f, by + 15f, 11.5f, dim(Theme.TEXT, a), Text.Align.LEFT);
		}
	}

	@Override
	public boolean mouseClicked(float mx, float my, int button) {
		if (!this.contains(mx, my)) {
			return false;
		}
		if (this.renaming != null) {
			if (this.renameField.contains(mx, my)) {
				return this.renameField.mouseClicked(mx, my, button);
			}
			this.commitRename();
		}
		if (this.hoveredRow >= 0) {
			String name = this.profiles.get(this.hoveredRow);
			if (this.hoveredAction == 0) {
				if (ConfigManager.delete(name)) {
					Notifications.push("Profile deleted", name, Icons.TRASH_2, Theme.TEXT_MUTED);
				} else {
					Notifications.push("Can't delete", "Keep at least one profile", Icons.CIRCLE_ALERT, Theme.WARNING);
				}
				this.refresh();
			} else if (this.hoveredAction == 1) {
				this.renaming = name;
				this.renameField.setValue(name);
			} else if (!name.equals(ConfigManager.activeProfile())) {
				ConfigManager.switchTo(name);
				Notifications.push("Profile loaded", name, Icons.FOLDER_OPEN, Theme.accent());
			}
			UiSounds.soft();
			return true;
		}
		switch (this.hoveredFooter) {
			case 0 -> {
				String created = ConfigManager.create("Profile");
				this.refresh();
				this.renaming = created;
				this.renameField.setValue(created);
				UiSounds.click();
			}
			case 1 -> {
				Minecraft.getInstance().keyboardHandler.setClipboard(ConfigManager.exportString());
				try {
					Path file = ConfigManager.exportFile();
					Notifications.push("Profile exported", "Copied and saved as " + file.getFileName(), Icons.UPLOAD, Theme.accent());
				} catch (IOException e) {
					Notifications.push("Profile copied", "Could not write the export file", Icons.CLIPBOARD, Theme.WARNING);
				}
				UiSounds.click();
			}
			case 2 -> {
				String name = ConfigManager.importString(Minecraft.getInstance().keyboardHandler.getClipboard(), "Imported");
				if (name != null) {
					Notifications.push("Profile imported", name, Icons.DOWNLOAD, Theme.accent());
					this.refresh();
				} else {
					Notifications.push("Nothing to import", "Copy an exported profile first", Icons.CIRCLE_ALERT, Theme.WARNING);
				}
				UiSounds.click();
			}
			default -> {
			}
		}
		return true;
	}

	@Override
	public boolean mouseReleased(float mx, float my, int button) {
		return this.renameField.mouseReleased(mx, my, button);
	}

	@Override
	public boolean keyPressed(int key, int scancode, int modifiers) {
		if (this.renaming != null) {
			if (key == InputConstants.KEY_ESCAPE) {
				this.renaming = null;
				return true;
			}
			return this.renameField.keyPressed(key, scancode, modifiers);
		}
		return false;
	}

	@Override
	public boolean charTyped(int codepoint, int modifiers) {
		return this.renaming != null && this.renameField.charTyped(codepoint, modifiers);
	}
}
