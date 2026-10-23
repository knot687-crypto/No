package dev.xeys.visuals.ui.clickgui;

import dev.xeys.visuals.ui.clickgui.control.Popover;
import org.jspecify.annotations.Nullable;

/** What controls need from the ClickGUI host. */
public interface GuiContext {
	void openPopover(Popover popover);

	void closePopover();

	@Nullable Popover popover();

	/** Routes the next key / mouse press to a keybind control. */
	void captureInput(@Nullable InputCapture capture);

	interface InputCapture {
		/** Return true when the capture is complete. */
		boolean onKey(int key);

		boolean onMouse(int button);
	}
}
